package com.anoncircles.graph.model;

/** Matches {@code AuthResult} in {@code schema.graphqls}. */
public record AuthResult(String engageAuth, User user) {
}
