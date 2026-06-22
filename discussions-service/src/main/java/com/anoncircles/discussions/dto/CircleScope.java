package com.anoncircles.discussions.dto;

import java.util.Locale;

/** Listing scope for {@code GET /circles}. */
public enum CircleScope {
    ALL,
    MINE,
    DISCOVER;

    /** Lenient parsing — accepts either case from REST query strings. */
    public static CircleScope parse(String raw) {
        if (raw == null || raw.isBlank()) return ALL;
        try {
            return CircleScope.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unknown scope: " + raw + " (expected ALL|MINE|DISCOVER)");
        }
    }
}
