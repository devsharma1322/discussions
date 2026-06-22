package com.anoncircles.graph.model;

/** Matches {@code GenericResult} in {@code schema.graphqls}. */
public record GenericResult(boolean success) {

    public static GenericResult ok() {
        return new GenericResult(true);
    }
}
