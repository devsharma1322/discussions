package com.anoncircles.discussions.unit;

import com.anoncircles.discussions.domain.Circle;
import com.anoncircles.discussions.domain.Thread;
import com.anoncircles.discussions.dto.PageResponse;
import com.anoncircles.discussions.dto.ThreadResponse;
import com.anoncircles.discussions.repository.CircleRepository;
import com.anoncircles.discussions.repository.MembershipRepository;
import com.anoncircles.discussions.repository.ThreadRepository;
import com.anoncircles.discussions.service.ThreadService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ThreadServiceTest {

    private ThreadRepository threads;
    private CircleRepository circles;
    private MembershipRepository memberships;
    private ThreadService service;

    private UUID admin;
    private UUID nonMember;
    private UUID circleId;

    @BeforeEach
    void setUp() {
        threads = mock(ThreadRepository.class);
        circles = mock(CircleRepository.class);
        memberships = mock(MembershipRepository.class);
        service = new ThreadService(threads, circles, memberships);

        admin = UUID.randomUUID();
        nonMember = UUID.randomUUID();
        circleId = UUID.randomUUID();
    }

    @Test
    void list_returnsThreadsForExistingCircle() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "d", admin, 1, Instant.now())));
        when(threads.listByCircle(circleId, 1, 10))
                .thenReturn(List.of(new Thread(UUID.randomUUID(), circleId, "first", "h", Instant.now())));
        when(threads.countByCircle(circleId)).thenReturn(1L);

        PageResponse<ThreadResponse> page = service.list(circleId, 1, 10);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.data()).hasSize(1);
    }

    @Test
    void list_throws404IfCircleMissing() {
        when(circles.findById(circleId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.list(circleId, 1, 10))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void create_membership_insertsWithSnapshotHandle() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "d", admin, 1, Instant.now())));
        when(memberships.findHandle(admin, circleId)).thenReturn(Optional.of("BoldHawk-07"));
        UUID threadId = UUID.randomUUID();
        when(threads.insert(circleId, "hello", "BoldHawk-07"))
                .thenReturn(new Thread(threadId, circleId, "hello", "BoldHawk-07", Instant.now()));

        ThreadResponse r = service.create(circleId, "hello", admin);
        assertThat(r.id()).isEqualTo(threadId);
        assertThat(r.createdBy()).isEqualTo("BoldHawk-07");
    }

    @Test
    void create_throws403IfNotMember() {
        when(circles.findById(circleId)).thenReturn(Optional.of(
                new Circle(circleId, "t", "d", admin, 1, Instant.now())));
        when(memberships.findHandle(nonMember, circleId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.create(circleId, "hello", nonMember))
                .isInstanceOf(AccessDeniedException.class);
    }
}
