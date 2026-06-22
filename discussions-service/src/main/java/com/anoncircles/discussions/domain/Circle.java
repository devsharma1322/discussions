package com.anoncircles.discussions.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A discussion circle. {@code topic} is immutable after creation; {@code description}
 * is editable by the admin only. {@code memberCount} is maintained transactionally
 * by a Postgres trigger (migration V2) — never update it in application code.
 */
public record Circle(
        UUID id,
        String topic,
        String description,
        UUID adminUserId,
        int memberCount,
        Instant createdAt
) {
}
