package com.anoncircles.discussions.service;

import com.anoncircles.discussions.dto.CircleScope;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Builds the SQL {@code WHERE} fragment + params for the scope-aware circles
 * listing query.
 *
 * <p>Matrix:
 * <pre>
 *   scope       search     →  WHERE
 *   ─────────   ────────      ───────────────────────────────────────────────────
 *   ALL         (any)         search-only OR empty
 *   MINE        (any)         (admin OR member) AND search?
 *   DISCOVER    empty         NOT admin AND NOT member
 *   DISCOVER    non-empty     search-only (scope widens to ALL — "search overrides scope")
 * </pre>
 *
 * <p>Search matches {@code topic OR description} via {@code pg_trgm} ILIKE.
 * The clause uses named parameters; the returned map is meant to be merged
 * into the caller's {@code MapSqlParameterSource}.
 */
@Component
public class CircleScopeClauseBuilder {

    /** Result of {@link #build(CircleScope, String, UUID)}. */
    public record Result(String whereClause, Map<String, Object> params) {}

    public Result build(CircleScope scope, String search, UUID viewerId) {
        String normalisedSearch = search == null ? null : search.trim();
        boolean hasSearch = normalisedSearch != null && !normalisedSearch.isEmpty();

        Map<String, Object> params = new HashMap<>();
        StringBuilder where = new StringBuilder();

        // 1) Scope filter
        boolean applyScope = switch (scope) {
            case MINE     -> true;
            case DISCOVER -> !hasSearch;     // search overrides DISCOVER
            case ALL      -> false;
        };
        if (applyScope) {
            params.put("viewer", viewerId);
            switch (scope) {
                case MINE -> where.append(
                        "(c.admin_user_id = :viewer " +
                        " OR EXISTS (SELECT 1 FROM memberships m " +
                        "            WHERE m.circle_id = c.id AND m.user_id = :viewer))");
                case DISCOVER -> where.append(
                        "c.admin_user_id <> :viewer " +
                        " AND NOT EXISTS (SELECT 1 FROM memberships m " +
                        "                 WHERE m.circle_id = c.id AND m.user_id = :viewer)");
                default -> throw new IllegalStateException("unreachable");
            }
        }

        // 2) Search filter (topic OR description, trigram-friendly ILIKE)
        if (hasSearch) {
            if (where.length() > 0) where.append(" AND ");
            where.append("(c.topic ILIKE :search OR c.description ILIKE :search)");
            params.put("search", "%" + normalisedSearch + "%");
        }

        return new Result(where.toString(), params);
    }
}
