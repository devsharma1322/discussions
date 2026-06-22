package com.anoncircles.graph.model;

/** Matches the {@code CircleScope} enum in {@code schema.graphqls}. */
public enum CircleScope {
    ALL,
    MINE,
    DISCOVER;

    /** REST query param representation (lowercase) used by discussions-service. */
    public String restValue() {
        return name().toLowerCase();
    }
}
