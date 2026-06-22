package com.anoncircles.graph.model;

/**
 * Failure branch of {@link GenerateDescriptionResult}. Returned when the
 * downstream {@code genai-service} signals {@code UNAVAILABLE} or throws.
 */
public record DescriptionUnavailable(String message) implements GenerateDescriptionResult {
}
