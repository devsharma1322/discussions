package com.anoncircles.discussions.dto;

import java.util.Locale;

/** Sort order for {@code GET /circles}. */
public enum CircleSort {
    POPULAR,
    NEWEST;

    public static CircleSort parse(String raw) {
        if (raw == null || raw.isBlank()) return POPULAR;
        try {
            return CircleSort.valueOf(raw.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw new IllegalArgumentException(
                    "Unknown sort: " + raw + " (expected POPULAR|NEWEST)");
        }
    }

    public String orderByClause() {
        return switch (this) {
            case POPULAR -> "c.member_count DESC, c.created_at DESC";
            case NEWEST  -> "c.created_at DESC";
        };
    }
}
