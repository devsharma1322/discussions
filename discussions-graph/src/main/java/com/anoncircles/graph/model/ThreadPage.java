package com.anoncircles.graph.model;

import java.util.List;

/** Matches {@code ThreadPage} in {@code schema.graphqls}. */
public record ThreadPage(List<Thread> data, long total, int page, int limit) {
}
