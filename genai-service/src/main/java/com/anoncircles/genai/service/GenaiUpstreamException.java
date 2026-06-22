package com.anoncircles.genai.service;

/**
 * Thrown when an upstream GenAI provider (e.g. Gemini) fails. The controller's
 * {@code onErrorResume} branch catches this and emits a single SSE
 * {@code {"status":"UNAVAILABLE"}} event so downstream callers can render a
 * graceful fallback.
 */
public class GenaiUpstreamException extends RuntimeException {
    public GenaiUpstreamException(String message, Throwable cause) {
        super(message, cause);
    }
}
