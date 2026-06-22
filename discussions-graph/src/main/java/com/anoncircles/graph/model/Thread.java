package com.anoncircles.graph.model;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Matches the {@code Thread} type in {@code schema.graphqls}.
 *
 * <p>Shadows {@link java.lang.Thread} — always import explicitly.
 */
public record Thread(
        UUID id,
        UUID circleId,
        String title,
        String createdBy,
        OffsetDateTime createdAt
) {
}
