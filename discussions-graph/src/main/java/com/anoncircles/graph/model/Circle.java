package com.anoncircles.graph.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Matches the {@code Circle} type in {@code schema.graphqls}. */
public record Circle(
        UUID id,
        String topic,
        String description,
        int memberCount,
        UUID adminUserId,
        boolean isAdmin,
        boolean isMember,
        OffsetDateTime createdAt
) {
}
