package com.anoncircles.graph.client;

/**
 * Signals that {@code genai-service} could not produce a description — either
 * because it returned a {@code {"status":"UNAVAILABLE",...}} SSE event, threw
 * an upstream error, or timed out.
 *
 * <p>The resolver translates this into the typed
 * {@code DescriptionUnavailable} branch of the {@code GenerateDescriptionResult}
 * union, so callers don't see a generic error — they see a discriminated union.
 */
public class GenaiUnavailableException extends RuntimeException {

    public GenaiUnavailableException(String message) {
        super(message);
    }

    public GenaiUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
