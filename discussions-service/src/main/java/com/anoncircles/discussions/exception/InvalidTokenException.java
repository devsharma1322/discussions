package com.anoncircles.discussions.exception;

/**
 * Thrown by {@code EngageAuthTokenService.verify} for any non-expiry validation
 * failure (bad signature, malformed payload, missing subject, …). Mapped to
 * HTTP 401 by {@code GlobalExceptionHandler}.
 */
public class InvalidTokenException extends RuntimeException {
    public InvalidTokenException(String message) {
        super(message);
    }
}
