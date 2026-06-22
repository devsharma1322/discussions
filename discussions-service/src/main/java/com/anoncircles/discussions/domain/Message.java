package com.anoncircles.discussions.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A message posted in a {@link Thread}. {@code author} is the per-circle handle
 * snapshotted at post time.
 */
public record Message(
        UUID id,
        UUID threadId,
        String body,
        String author,
        Instant createdAt
) {
}
