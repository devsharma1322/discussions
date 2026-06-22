package com.anoncircles.discussions.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * An anonymous user. Identified by a UUID + display name. No PII — the JWT
 * tracks the same user across sessions, but if they clear local storage they
 * become a new user.
 */
public record User(
        UUID id,
        String displayName,
        Instant createdAt
) {
}
