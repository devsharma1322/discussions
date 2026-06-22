package com.anoncircles.discussions.dto;

import java.util.List;

/**
 * Generic paginated response envelope.
 *
 * <p>Centralises the pagination contract:
 * <ul>
 *   <li>{@code page} is 1-based.</li>
 *   <li>{@code limit} is the actual page size used after server-side clamping
 *       (hard-capped at {@link #MAX_LIMIT}).</li>
 * </ul>
 */
public record PageResponse<T>(
        List<T> data,
        long total,
        int page,
        int limit
) {
    public static final int MAX_LIMIT = 10;
    public static final int DEFAULT_LIMIT = 10;

    /** Clamp the requested limit into {@code [1, MAX_LIMIT]}. Null → DEFAULT_LIMIT. */
    public static int clampLimit(Integer requested) {
        if (requested == null) {
            return DEFAULT_LIMIT;
        }
        return Math.max(1, Math.min(MAX_LIMIT, requested));
    }

    /** Normalize the requested page to a 1-based value (null → 1, negatives → 1). */
    public static int normalizePage(Integer requested) {
        if (requested == null) {
            return 1;
        }
        return Math.max(1, requested);
    }

    /** Convert a 1-based page + limit to a SQL OFFSET. */
    public static int offset(int page, int limit) {
        return (page - 1) * limit;
    }
}
