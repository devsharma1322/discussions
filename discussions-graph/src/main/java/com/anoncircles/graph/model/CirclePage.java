package com.anoncircles.graph.model;

import java.util.List;

/** Matches {@code CirclePage} in {@code schema.graphqls}. */
public record CirclePage(List<Circle> data, long total, int page, int limit) {
}
