package com.anoncircles.discussions.unit;

import com.anoncircles.discussions.dto.CircleScope;
import com.anoncircles.discussions.service.CircleScopeClauseBuilder;
import com.anoncircles.discussions.service.CircleScopeClauseBuilder.Result;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the scope/search WHERE clause builder.
 * Covers the matrix in {@code repo-1-tickets.md > "Build scope-aware SQL
 * clause builder for circles"}.
 */
class CircleScopeClauseBuilderTest {

    private final CircleScopeClauseBuilder builder = new CircleScopeClauseBuilder();
    private final UUID viewer = UUID.randomUUID();

    @Test
    void all_noSearch_producesNoFilter() {
        Result r = builder.build(CircleScope.ALL, null, viewer);
        assertThat(r.whereClause()).isEmpty();
        assertThat(r.params()).isEmpty();
    }

    @Test
    void all_withSearch_onlyAppliesSearchClause() {
        Result r = builder.build(CircleScope.ALL, "spring", viewer);
        assertThat(r.whereClause()).contains("ILIKE :search");
        assertThat(r.whereClause()).doesNotContain("admin_user_id");
        assertThat(r.params()).containsEntry("search", "%spring%");
    }

    @Test
    void mine_noSearch_appliesScopeOnly() {
        Result r = builder.build(CircleScope.MINE, null, viewer);
        assertThat(r.whereClause()).contains("admin_user_id = :viewer");
        assertThat(r.whereClause()).contains("EXISTS");
        assertThat(r.params()).containsEntry("viewer", viewer);
        assertThat(r.params()).doesNotContainKey("search");
    }

    @Test
    void mine_withSearch_keepsScopeAndAddsSearch() {
        Result r = builder.build(CircleScope.MINE, "spring", viewer);
        assertThat(r.whereClause()).contains("admin_user_id = :viewer");
        assertThat(r.whereClause()).contains("AND");
        assertThat(r.whereClause()).contains("ILIKE :search");
        assertThat(r.params()).containsEntry("viewer", viewer);
        assertThat(r.params()).containsEntry("search", "%spring%");
    }

    @Test
    void discover_noSearch_appliesScope() {
        Result r = builder.build(CircleScope.DISCOVER, null, viewer);
        assertThat(r.whereClause()).contains("admin_user_id <> :viewer");
        assertThat(r.whereClause()).contains("NOT EXISTS");
        assertThat(r.params()).containsEntry("viewer", viewer);
    }

    @Test
    void discover_withSearch_dropsScope_widensToAll() {
        Result r = builder.build(CircleScope.DISCOVER, "spring", viewer);
        // Scope-override rule: search overrides DISCOVER, widening to ALL.
        assertThat(r.whereClause()).doesNotContain("admin_user_id");
        assertThat(r.whereClause()).doesNotContain("NOT EXISTS");
        assertThat(r.whereClause()).contains("ILIKE :search");
        assertThat(r.params()).doesNotContainKey("viewer");
        assertThat(r.params()).containsEntry("search", "%spring%");
    }

    @Test
    void blankSearch_treatedAsNoSearch() {
        Result r = builder.build(CircleScope.DISCOVER, "   ", viewer);
        // Blank search trimmed → scope filter stays active.
        assertThat(r.whereClause()).contains("NOT EXISTS");
        assertThat(r.params()).doesNotContainKey("search");
    }
}
