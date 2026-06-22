package com.anoncircles.discussions.exception;

/**
 * Thrown by {@code EngageAuthTokenService.verify} when the JWT's {@code exp}
 * claim is in the past. Mapped to HTTP 401 by {@code GlobalExceptionHandler}.
 */
public class TokenExpiredException extends RuntimeException {
    public TokenExpiredException(String message) {
        super(message);
    }
}
