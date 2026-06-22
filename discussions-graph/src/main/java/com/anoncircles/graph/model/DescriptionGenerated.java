package com.anoncircles.graph.model;

/** Success branch of {@link GenerateDescriptionResult}. */
public record DescriptionGenerated(String text) implements GenerateDescriptionResult {
}
