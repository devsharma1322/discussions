package com.anoncircles.discussions.unit;

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
import com.anoncircles.discussions.service.CircleScopeClauseBuilder;
import com.anoncircles.discussions.service.CircleScopeClauseBuilder.Result;
import com.anoncircles.discussions.service.CircleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link CircleService} — every code path with the repos and
 * helpers mocked, no Spring context.
 */
class CircleServiceTest {

    private CircleRepository circles;
    private MembershipRepository memberships;
    private CircleScopeClauseBuilder scope;
    private HandleGenerator handles;
    private CircleService service;

    private UUID admin;
    private UUID other;
    private UUID circleId;

    @BeforeEach
    void setUp() {
        circles = mock(CircleRepository.class);
        memberships = mock(MembershipRepository.class);
        scope = mock(CircleScopeClauseBuilder.class);
        handles = mock(HandleGenerator.class);
        service = new CircleService(circles, memberships, scope, handles);

        admin = UUID.randomUUID();
        other = UUID.randomUUID();
        circleId = UUID.randomUUID();
    }

    // ===== list =====

    @Test
    void list_buildsScopeClause_passesToRepoAndCounts() {
        Result clause = new Result("c.foo = :bar", Map.of("bar", "baz"));
        when(scope.build(CircleScope.MINE, "spring", admin)).thenReturn(clause);
        when(circles.findPage(eq("c.foo = :bar"), eq(CircleSort.POPULAR.orderByClause()),
                eq(Map.of("bar", "baz")), eq(admin), eq(1), eq(10)))
                .thenReturn(List.of(stubResponse(circleId, "x")));
        when(circles.countWhere(eq("c.foo = :bar"), eq(Map.of("bar", "baz")))).thenReturn(42L);

        PageResponse<CircleResponse> page = service.list(
                CircleScope.MINE, CircleSort.POPULAR, "spring", admin, 1, 10);

        assertThat(page.total()).isEqualTo(42);
        assertThat(page.data()).hasSize(1);
    }

    // ===== get =====

    @Test
    void get_returnsCircle() {
        when(circles.findByIdForViewer(circleId, admin))
                .thenReturn(Optional.of(stubResponse(circleId, "x")));
        CircleResponse r = service.get(circleId, admin);
        assertThat(r.id()).isEqualTo(circleId);
    }

    @Test
    void get_throws404IfMissing() {
        when(circles.findByIdForViewer(any(), any())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.get(circleId, admin))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    // ===== create =====

    @Test
    void create_insertsCircleAndAdminMembership() {
        Circle inserted = new Circle(circleId, "k8s", "kubernetes chat", admin, 0, Instant.now());
        when(circles.insert("k8s", "kubernetes chat", admin)).thenReturn(inserted);
        when(handles.generate(admin, circleId)).thenReturn("BoldHawk-07");
        when(memberships.insert(admin, circleId, "BoldHawk-07"))
                .thenReturn(new Membership(admin, circleId, "BoldHawk-07", Instant.now()));
        when(circles.findByIdForViewer(circleId, admin))
                .thenReturn(Optional.of(stubResponse(circleId, "k8s")));

        CircleResponse r = service.create("k8s", "kubernetes chat", admin);

        assertThat(r.topic()).isEqualTo("k8s");
        verify(memberships, times(1)).insert(admin, circleId, "BoldHawk-07");
    }

    // ===== updateDescription =====

    @Test
    void updateDescription_succeedsForAdmin() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "old", admin, 1, Instant.now())));
        when(circles.findByIdForViewer(circleId, admin))
                .thenReturn(Optional.of(stubResponse(circleId, "t")));

        CircleResponse r = service.updateDescription(circleId, "new copy", admin);

        verify(circles).updateDescription(circleId, "new copy");
        assertThat(r.id()).isEqualTo(circleId);
    }

    @Test
    void updateDescription_throws403ForNonAdmin() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "d", admin, 1, Instant.now())));
        assertThatThrownBy(() -> service.updateDescription(circleId, "hack", other))
                .isInstanceOf(AccessDeniedException.class);
        verify(circles, never()).updateDescription(any(), anyString());
    }

    @Test
    void updateDescription_throws404IfMissing() {
        when(circles.findById(circleId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateDescription(circleId, "x", admin))
                .isInstanceOf(ResponseStatusException.class);
    }

    // ===== delete =====

    @Test
    void delete_succeedsForAdmin() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "d", admin, 1, Instant.now())));
        service.delete(circleId, admin);
        verify(circles).deleteById(circleId);
    }

    @Test
    void delete_throws403ForNonAdmin() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "d", admin, 1, Instant.now())));
        assertThatThrownBy(() -> service.delete(circleId, other))
                .isInstanceOf(AccessDeniedException.class);
        verify(circles, never()).deleteById(any());
    }

    // ===== join =====

    @Test
    void join_throws409IfAdmin() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "d", admin, 1, Instant.now())));
        assertThatThrownBy(() -> service.join(circleId, admin))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(memberships, never()).insert(any(), any(), any());
    }

    @Test
    void join_isIdempotent_returnsExistingHandle() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "d", admin, 1, Instant.now())));
        when(memberships.findHandle(other, circleId)).thenReturn(Optional.of("OldOtter-11"));

        JoinResponse r = service.join(circleId, other);
        assertThat(r.handle()).isEqualTo("OldOtter-11");
        verify(memberships, never()).insert(any(), any(), any());
    }

    @Test
    void join_freshMembership_generatesHandleAndInserts() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "d", admin, 1, Instant.now())));
        when(memberships.findHandle(other, circleId)).thenReturn(Optional.empty());
        when(handles.generate(other, circleId)).thenReturn("FreshFox-77");
        when(memberships.insert(other, circleId, "FreshFox-77"))
                .thenReturn(new Membership(other, circleId, "FreshFox-77", Instant.now()));

        JoinResponse r = service.join(circleId, other);
        assertThat(r.handle()).isEqualTo("FreshFox-77");
    }

    // ===== leave =====

    @Test
    void leave_throws409IfAdmin() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "d", admin, 1, Instant.now())));
        assertThatThrownBy(() -> service.leave(circleId, admin))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
        verify(memberships, never()).delete(any(), any());
    }

    @Test
    void leave_deletesMembership() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "d", admin, 1, Instant.now())));
        when(memberships.delete(other, circleId)).thenReturn(1);
        service.leave(circleId, other);
        verify(memberships).delete(other, circleId);
    }

    @Test
    void leave_noOp_whenNotAMember() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "d", admin, 1, Instant.now())));
        when(memberships.delete(other, circleId)).thenReturn(0);
        service.leave(circleId, other); // no exception
        verify(memberships).delete(other, circleId);
    }

    // ===== helpers =====

    private static CircleResponse stubResponse(UUID id, String topic) {
        return new CircleResponse(id, topic, "desc", 1, UUID.randomUUID(), true, true, Instant.now());
    }
}
