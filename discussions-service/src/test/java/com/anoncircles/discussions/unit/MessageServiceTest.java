package com.anoncircles.discussions.unit;

import com.anoncircles.discussions.domain.Message;
import com.anoncircles.discussions.domain.Thread;
import com.anoncircles.discussions.dto.MessageResponse;
import com.anoncircles.discussions.dto.PageResponse;
import com.anoncircles.discussions.repository.MembershipRepository;
import com.anoncircles.discussions.repository.MessageRepository;
import com.anoncircles.discussions.repository.ThreadRepository;
import com.anoncircles.discussions.service.MessageService;
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

class MessageServiceTest {

    private MessageRepository messages;
    private ThreadRepository threads;
    private MembershipRepository memberships;
    private MessageService service;

    private UUID member;
    private UUID outsider;
    private UUID circleId;
    private UUID threadId;
    private Thread thread;

    @BeforeEach
    void setUp() {
        messages = mock(MessageRepository.class);
        threads = mock(ThreadRepository.class);
        memberships = mock(MembershipRepository.class);
        service = new MessageService(messages, threads, memberships);

        member = UUID.randomUUID();
        outsider = UUID.randomUUID();
        circleId = UUID.randomUUID();
        threadId = UUID.randomUUID();
        thread = new Thread(threadId, circleId, "title", "h", Instant.now());
    }

    @Test
    void list_returnsMessages_forMembers() {
        when(threads.findById(threadId)).thenReturn(Optional.of(thread));
        when(memberships.exists(member, circleId)).thenReturn(true);
        when(messages.listByThread(threadId, 1, 10)).thenReturn(List.of(
                new Message(UUID.randomUUID(), threadId, "hi", "h", Instant.now())));
        when(messages.countByThread(threadId)).thenReturn(1L);

        PageResponse<MessageResponse> page = service.list(threadId, member, 1, 10);
        assertThat(page.total()).isEqualTo(1);
        assertThat(page.data()).hasSize(1);
    }

    @Test
    void list_throws403_forNonMembers() {
        when(threads.findById(threadId)).thenReturn(Optional.of(thread));
        when(memberships.exists(outsider, circleId)).thenReturn(false);
        assertThatThrownBy(() -> service.list(threadId, outsider, 1, 10))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void list_throws404_whenThreadMissing() {
        when(threads.findById(threadId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.list(threadId, member, 1, 10))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void post_snapshotsAuthorHandle() {
        when(threads.findById(threadId)).thenReturn(Optional.of(thread));
        when(memberships.exists(member, circleId)).thenReturn(true);
        when(memberships.findHandle(member, circleId)).thenReturn(Optional.of("BoldHawk-07"));
        UUID mid = UUID.randomUUID();
        when(messages.insert(threadId, "body", "BoldHawk-07"))
                .thenReturn(new Message(mid, threadId, "body", "BoldHawk-07", Instant.now()));

        MessageResponse r = service.post(threadId, "body", member);
        assertThat(r.id()).isEqualTo(mid);
        assertThat(r.author()).isEqualTo("BoldHawk-07");
    }

    @Test
    void post_throws403_forNonMembers() {
        when(threads.findById(threadId)).thenReturn(Optional.of(thread));
        when(memberships.exists(outsider, circleId)).thenReturn(false);
        assertThatThrownBy(() -> service.post(threadId, "body", outsider))
                .isInstanceOf(AccessDeniedException.class);
    }
}
