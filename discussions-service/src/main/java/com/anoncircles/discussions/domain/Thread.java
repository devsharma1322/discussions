package com.anoncircles.discussions.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * A discussion thread inside a {@link Circle}. {@code createdBy} stores the
 * author's per-circle handle as a snapshot at creation time — if the handle
 * scheme ever changes, history stays stable.
 *
 * <p>Note: this name shadows {@link java.lang.Thread}. Always import explicitly:
 * {@code import com.anoncircles.discussions.domain.Thread;}
 */
public record Thread(
        UUID id,
        UUID circleId,
        String title,
        String createdBy,
        Instant createdAt
) {
}
