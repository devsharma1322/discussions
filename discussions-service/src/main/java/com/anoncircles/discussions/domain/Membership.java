package com.anoncircles.discussions.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Join row between a {@link User} and a {@link Circle}. {@code handle} is the
 * user's per-circle pseudonym, deterministically derived from
 * {@code userId + circleId}.
 */
public record Membership(
        UUID userId,
        UUID circleId,
        String handle,
        Instant joinedAt
) {
}
