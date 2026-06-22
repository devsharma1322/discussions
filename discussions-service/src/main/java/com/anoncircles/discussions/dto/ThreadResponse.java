package com.anoncircles.discussions.dto;

import com.anoncircles.discussions.domain.Thread;

import java.time.Instant;
import java.util.UUID;

public record ThreadResponse(
        UUID id,
        UUID circleId,
        String title,
        String createdBy,
        Instant createdAt
) {
    public static ThreadResponse from(Thread thread) {
        return new ThreadResponse(
                thread.id(),
                thread.circleId(),
                thread.title(),
                thread.createdBy(),
                thread.createdAt()
        );
    }
}
