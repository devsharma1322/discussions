package com.anoncircles.graph.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Matches the {@code Message} type in {@code schema.graphqls}. */
public record Message(
        UUID id,
        UUID threadId,
        String body,
        String author,
        OffsetDateTime createdAt
) {
}
