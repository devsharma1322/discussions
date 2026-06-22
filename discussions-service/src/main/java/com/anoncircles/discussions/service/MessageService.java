package com.anoncircles.discussions.service;

import com.anoncircles.discussions.domain.Message;
import com.anoncircles.discussions.domain.Thread;
import com.anoncircles.discussions.dto.MessageResponse;
import com.anoncircles.discussions.dto.PageResponse;
import com.anoncircles.discussions.repository.MembershipRepository;
import com.anoncircles.discussions.repository.MessageRepository;
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
 * Messages inside a thread. Both read and write require membership in the
 * parent circle.
 *
 * <p>{@code author} is snapshotted from the member's per-circle handle so
 * each message's authorship is stable in history.
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ThreadRepository threadRepository;
    private final MembershipRepository membershipRepository;

    public PageResponse<MessageResponse> list(UUID threadId, UUID userId, int page, int limit) {
        Thread thread = requireThreadAndMembership(threadId, userId);
        List<Message> rows = messageRepository.listByThread(thread.id(), page, limit);
        long total = messageRepository.countByThread(thread.id());
        return new PageResponse<>(rows.stream().map(MessageResponse::from).toList(),
                total, page, limit);
    }

    @Transactional
    public MessageResponse post(UUID threadId, String body, UUID userId) {
        Thread thread = requireThreadAndMembership(threadId, userId);
        String handle = membershipRepository.findHandle(userId, thread.circleId())
                .orElseThrow(() -> new AccessDeniedException(
                        "Must be a member of the circle to post"));
        Message message = messageRepository.insert(thread.id(), body, handle);
        return MessageResponse.from(message);
    }

    // ----- helpers -----

    private Thread requireThreadAndMembership(UUID threadId, UUID userId) {
        Thread thread = threadRepository.findById(threadId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Thread not found"));
        if (!membershipRepository.exists(userId, thread.circleId())) {
            throw new AccessDeniedException("Must be a member of the circle to view or post");
        }
        return thread;
    }
}
