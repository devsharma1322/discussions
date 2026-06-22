package com.anoncircles.discussions.service;

import com.anoncircles.discussions.domain.Circle;
import com.anoncircles.discussions.domain.Thread;
import com.anoncircles.discussions.dto.PageResponse;
import com.anoncircles.discussions.dto.ThreadResponse;
import com.anoncircles.discussions.repository.CircleRepository;
import com.anoncircles.discussions.repository.MembershipRepository;
import com.anoncircles.discussions.repository.ThreadRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.UUID;

/**
 * Threads sit inside a {@link Circle}. Reads are open to any authenticated
 * user; writes (creating a thread) require membership — admin counts as a
 * member.
 *
 * <p>{@code created_by} is snapshotted from the author's per-circle handle so
 * thread history stays stable even if the handle scheme ever changes.
 */
@Service
@RequiredArgsConstructor
public class ThreadService {

    private final ThreadRepository threadRepository;
    private final CircleRepository circleRepository;
    private final MembershipRepository membershipRepository;

    public PageResponse<ThreadResponse> list(UUID circleId, int page, int limit) {
        requireCircleExists(circleId);
        List<Thread> rows = threadRepository.listByCircle(circleId, page, limit);
        long total = threadRepository.countByCircle(circleId);
        return new PageResponse<>(rows.stream().map(ThreadResponse::from).toList(),
                total, page, limit);
    }

    public ThreadResponse get(UUID threadId) {
        return threadRepository.findById(threadId)
                .map(ThreadResponse::from)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
    }

    @Transactional
    public ThreadResponse create(UUID circleId, String title, UUID userId) {
        Circle circle = circleRepository.findById(circleId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Circle not found"));
        String handle = resolveHandleOrForbid(circle, userId, circleId);
        Thread thread = threadRepository.insert(circleId, title, handle);
        return ThreadResponse.from(thread);
    }

    // ----- helpers -----

    private void requireCircleExists(UUID circleId) {
        circleRepository.findById(circleId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Circle not found"));
    }

    /**
     * Admin can post under their own snapshot handle (looked up via their
     * membership row, which {@link CircleService#create} guarantees exists).
     * Non-members 403.
     */
    private String resolveHandleOrForbid(Circle circle, UUID userId, UUID circleId) {
        return membershipRepository.findHandle(userId, circleId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Must be a member of the circle to post"));
    }
}
