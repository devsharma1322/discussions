package com.anoncircles.graph.model;

import java.util.List;

/** Matches {@code MessagePage} in {@code schema.graphqls}. */
public record MessagePage(List<Message> data, long total, int page, int limit) {
}
