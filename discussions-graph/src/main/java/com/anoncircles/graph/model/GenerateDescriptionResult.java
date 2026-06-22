package com.anoncircles.graph.model;

/**
 * Java sealed model for the GraphQL union
 * {@code GenerateDescriptionResult = DescriptionGenerated | DescriptionUnavailable}.
 *
 * <p>A {@code TypeResolver} is registered in {@code GraphQlConfig} so Spring for
 * GraphQL knows which schema typename to emit for each implementation.
 */
public sealed interface GenerateDescriptionResult
        permits DescriptionGenerated, DescriptionUnavailable {
}
