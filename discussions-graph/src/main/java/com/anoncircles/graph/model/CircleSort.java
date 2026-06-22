package com.anoncircles.graph.model;

/** Matches the {@code CircleSort} enum in {@code schema.graphqls}. */
public enum CircleSort {
    POPULAR,
    NEWEST;

    /** REST query param representation (lowercase). */
    public String restValue() {
        return name().toLowerCase();
    }
}
