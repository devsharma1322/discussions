package com.anoncircles.discussions.dto;

/**
 * Response from {@code POST /auth/session}. The {@code engageAuth} JWT
 * identifies the anonymous user across subsequent requests.
 */
public record SessionResponse(
        String engageAuth,
        UserResponse user
) {
}
