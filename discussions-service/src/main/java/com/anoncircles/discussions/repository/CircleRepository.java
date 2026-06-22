package com.anoncircles.discussions.repository;

import com.anoncircles.discussions.domain.Circle;
import com.anoncircles.discussions.dto.CircleResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for circles.
 *
 * <p>Most listing queries also project the {@code is_admin}/{@code is_member}
 * flags for the viewer so the controller can build {@link CircleResponse} in a
 * single round-trip. The scope-aware listing query is added by a separate
 * ticket ({@code Build scope-aware SQL clause builder for circles}); this class
 * provides the foundation methods every later ticket builds on.
 */
@Repository
@RequiredArgsConstructor
public class CircleRepository {

    private final NamedParameterJdbcTemplate jdbc;

    /** Row mapper that pulls Circle + viewer-scoped flags from the same row. */
    private static final RowMapper<CircleResponse> CIRCLE_WITH_VIEWER = (rs, i) ->
            new CircleResponse(
                    RowMappers.uuid(rs, "id"),
                    rs.getString("topic"),
                    rs.getString("description"),
                    rs.getInt("member_count"),
                    RowMappers.uuid(rs, "admin_user_id"),
                    rs.getBoolean("is_admin"),
                    rs.getBoolean("is_member"),
                    RowMappers.instant(rs, "created_at")
            );

    /** Bare-entity fetch — no viewer flags. */
    public Optional<Circle> findById(UUID id) {
        return jdbc.query(
                "SELECT id, topic, description, admin_user_id, member_count, created_at " +
                "FROM circles WHERE id = :id",
                Map.of("id", id),
                RowMappers.CIRCLE
        ).stream().findFirst();
    }

    /** Fetch with viewer-scoped {@code isAdmin}/{@code isMember} computed in SQL. */
    public Optional<CircleResponse> findByIdForViewer(UUID id, UUID viewerId) {
        return jdbc.query(
                "SELECT c.id, c.topic, c.description, c.admin_user_id, c.member_count, c.created_at, " +
                "  (c.admin_user_id = :viewer) AS is_admin, " +
                "  EXISTS (SELECT 1 FROM memberships m " +
                "          WHERE m.circle_id = c.id AND m.user_id = :viewer) AS is_member " +
                "FROM circles c WHERE c.id = :id",
                new MapSqlParameterSource().addValue("id", id).addValue("viewer", viewerId),
                CIRCLE_WITH_VIEWER
        ).stream().findFirst();
    }

    /**
     * Insert a circle owned by {@code adminUserId}. The membership row for the
     * admin (and thus {@code member_count}'s initial value via the trigger) is
     * the caller's responsibility — do both in the same {@code @Transactional}
     * service method.
     */
    public Circle insert(String topic, String description, UUID adminUserId) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO circles (id, topic, description, admin_user_id, member_count) " +
                "VALUES (:id, :topic, :description, :admin, 0)",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("topic", topic)
                        .addValue("description", description)
                        .addValue("admin", adminUserId)
        );
        return findById(id).orElseThrow(() ->
                new IllegalStateException("Circle vanished immediately after insert: " + id));
    }

    public int updateDescription(UUID id, String description) {
        return jdbc.update(
                "UPDATE circles SET description = :description WHERE id = :id",
                Map.of("id", id, "description", description)
        );
    }

    public int deleteById(UUID id) {
        return jdbc.update("DELETE FROM circles WHERE id = :id", Map.of("id", id));
    }

    /**
     * Generic paged query for {@link CircleResponse} rows that takes a
     * pre-built WHERE clause + extra params. The scope/search query builder
     * (a separate ticket) assembles these.
     *
     * @param whereClause SQL fragment that may reference {@code c.*}; pass an
     *                    empty string when no filter applies. Do <b>not</b> include
     *                    the leading {@code WHERE} keyword.
     * @param orderBy     SQL ORDER BY fragment (without the keyword), e.g.
     *                    {@code "c.member_count DESC, c.created_at DESC"}.
     */
    public List<CircleResponse> findPage(
            String whereClause,
            String orderBy,
            Map<String, Object> extraParams,
            UUID viewerId,
            int page,
            int limit
    ) {
        MapSqlParameterSource params = new MapSqlParameterSource(extraParams)
                .addValue("viewer", viewerId)
                .addValue("limit", limit)
                .addValue("offset", (page - 1) * limit);

        String sql =
                "SELECT c.id, c.topic, c.description, c.admin_user_id, c.member_count, c.created_at, " +
                "  (c.admin_user_id = :viewer) AS is_admin, " +
                "  EXISTS (SELECT 1 FROM memberships m " +
                "          WHERE m.circle_id = c.id AND m.user_id = :viewer) AS is_member " +
                "FROM circles c " +
                (whereClause.isBlank() ? "" : "WHERE " + whereClause + " ") +
                "ORDER BY " + orderBy + " " +
                "LIMIT :limit OFFSET :offset";

        return jdbc.query(sql, params, CIRCLE_WITH_VIEWER);
    }

    public long countWhere(String whereClause, Map<String, Object> extraParams) {
        String sql = "SELECT COUNT(*) FROM circles c " +
                (whereClause.isBlank() ? "" : "WHERE " + whereClause);
        Long count = jdbc.queryForObject(sql, new MapSqlParameterSource(extraParams), Long.class);
        return count == null ? 0L : count;
    }
}
