package com.anoncircles.graph.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/** Matches the {@code User} type in {@code schema.graphqls} — anonymous flow. */
public record User(
        UUID id,
        String displayName,
        OffsetDateTime createdAt
) {
}
