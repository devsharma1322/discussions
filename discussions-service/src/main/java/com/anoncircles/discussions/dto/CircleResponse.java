package com.anoncircles.discussions.dto;

import com.anoncircles.discussions.domain.Circle;

import java.time.Instant;
import java.util.UUID;

public record CircleResponse(
        UUID id,
        String topic,
        String description,
        int memberCount,
        UUID adminUserId,
        boolean isAdmin,
        boolean isMember,
        Instant createdAt
) {
    public static CircleResponse of(Circle circle, boolean isAdmin, boolean isMember) {
        return new CircleResponse(
                circle.id(),
                circle.topic(),
                circle.description(),
                circle.memberCount(),
                circle.adminUserId(),
                isAdmin,
                isMember,
                circle.createdAt()
        );
    }
}
