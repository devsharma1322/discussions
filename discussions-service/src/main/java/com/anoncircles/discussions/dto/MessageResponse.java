package com.anoncircles.discussions.dto;

import com.anoncircles.discussions.domain.Message;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID id,
        UUID threadId,
        String body,
        String author,
        Instant createdAt
) {
    public static MessageResponse from(Message message) {
        return new MessageResponse(
                message.id(),
                message.threadId(),
                message.body(),
                message.author(),
                message.createdAt()
        );
    }
}
