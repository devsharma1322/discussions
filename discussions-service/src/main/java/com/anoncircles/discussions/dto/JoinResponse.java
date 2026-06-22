package com.anoncircles.discussions.dto;

/**
 * Response from {@code POST /circles/{id}/join}. The handle is the caller's
 * per-circle pseudonym; from then on it can be looked up via the membership row.
 */
public record JoinResponse(String handle) {
}
