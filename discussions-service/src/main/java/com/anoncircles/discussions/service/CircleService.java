package com.anoncircles.discussions.service;

import com.anoncircles.discussions.domain.Circle;
import com.anoncircles.discussions.domain.Membership;
import com.anoncircles.discussions.dto.CircleResponse;
import com.anoncircles.discussions.dto.CircleScope;
import com.anoncircles.discussions.dto.CircleSort;
import com.anoncircles.discussions.dto.JoinResponse;
import com.anoncircles.discussions.dto.PageResponse;
import com.anoncircles.discussions.lib.HandleGenerator;
import com.anoncircles.discussions.repository.CircleRepository;
import com.anoncircles.discussions.repository.MembershipRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Business logic for circles: list (scoped + searched + paginated), fetch,
 * create, update description, delete, join, leave.
 *
 * <p>All writes that touch more than one row are {@link Transactional} so the
 * {@code member_count} trigger stays consistent.
 */
@Service
@RequiredArgsConstructor
public class CircleService {

    private final CircleRepository circleRepository;
    private final MembershipRepository membershipRepository;
    private final CircleScopeClauseBuilder scopeBuilder;
    private final HandleGenerator handleGenerator;

    public PageResponse<CircleResponse> list(
            CircleScope scope, CircleSort sort, String search,
            UUID viewerId, int page, int limit) {

        CircleScopeClauseBuilder.Result clause = scopeBuilder.build(scope, search, viewerId);
        List<CircleResponse> rows = circleRepository.findPage(
                clause.whereClause(),
                sort.orderByClause(),
                clause.params(),
                viewerId,
                page,
                limit);
        long total = circleRepository.countWhere(clause.whereClause(), clause.params());
        return new PageResponse<>(rows, total, page, limit);
    }

    public CircleResponse get(UUID id, UUID viewerId) {
        return circleRepository.findByIdForViewer(id, viewerId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Circle not found"));
    }

    @Transactional
    public CircleResponse create(String topic, String description, UUID adminUserId) {
        Circle circle = circleRepository.insert(topic, description, adminUserId);
        String handle = handleGenerator.generate(adminUserId, circle.id());
        // INSERT into memberships fires the trigger → member_count goes 0→1.
        membershipRepository.insert(adminUserId, circle.id(), handle);
        // Re-read to pick up the trigger-updated counter and viewer flags.
        return circleRepository.findByIdForViewer(circle.id(), adminUserId)
                .orElseThrow(() -> new IllegalStateException(
                        "Circle vanished immediately after create: " + circle.id()));
    }

    @Transactional
    public CircleResponse updateDescription(UUID id, String description, UUID viewerId) {
        Circle circle = circleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Circle not found"));
        requireAdmin(circle.adminUserId(), viewerId);
        circleRepository.updateDescription(id, description);
        return circleRepository.findByIdForViewer(id, viewerId)
                .orElseThrow(() -> new IllegalStateException("Circle vanished after update: " + id));
    }

    @Transactional
    public void delete(UUID id, UUID viewerId) {
        Circle circle = circleRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Circle not found"));
        requireAdmin(circle.adminUserId(), viewerId);
        circleRepository.deleteById(id);
    }

    @Transactional
    public JoinResponse join(UUID circleId, UUID userId) {
        Circle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Circle not found"));
        if (circle.adminUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Admins are implicitly members and cannot join again");
        }
        // Idempotent: return the existing handle without re-inserting.
        return membershipRepository.findHandle(userId, circleId)
                .map(JoinResponse::new)
                .orElseGet(() -> {
                    String handle = handleGenerator.generate(userId, circleId);
                    Membership inserted = membershipRepository.insert(userId, circleId, handle);
                    return new JoinResponse(inserted.handle());
                });
    }

    @Transactional
    public void leave(UUID circleId, UUID userId) {
        Circle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Circle not found"));
        if (circle.adminUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Admins cannot leave; delete the circle instead");
        }
        int deleted = membershipRepository.delete(userId, circleId);
        if (deleted == 0) {
            // Not a member — no-op is the friendlier choice. Use 404 only if we
            // want callers to react. Return silently keeps the UI optimistic-revert
            // path simple.
        }
    }

    // ---- helpers ----

    private static void requireAdmin(UUID adminUserId, UUID viewerId) {
        if (!adminUserId.equals(viewerId)) {
            throw new AccessDeniedException("Admin-only operation");
        }
    }

    /** Visible for tests — exposes the param-map structure used by the listing query. */
    static Map<String, Object> mergeParams(Map<String, Object> a, Map<String, Object> b) {
        Map<String, Object> merged = new java.util.HashMap<>(a);
        merged.putAll(b);
        return merged;
    }
}
