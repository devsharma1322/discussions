package com.anoncircles.discussions.exception;

import java.time.Duration;

/**
 * Thrown by the Bucket4j rate-limit filter / aspect when the caller exceeds
 * their quota. Mapped to HTTP 429 with a {@code Retry-After} header.
 */
public class RateLimitExceededException extends RuntimeException {

    private final Duration retryAfter;

    public RateLimitExceededException(String message, Duration retryAfter) {
        super(message);
        this.retryAfter = retryAfter;
    }

    public Duration retryAfter() {
        return retryAfter;
    }
}
