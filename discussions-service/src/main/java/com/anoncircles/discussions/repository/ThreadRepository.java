package com.anoncircles.discussions.repository;

import com.anoncircles.discussions.domain.Thread;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class ThreadRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public Optional<Thread> findById(UUID id) {
        return jdbc.query(
                "SELECT id, circle_id, title, created_by, created_at " +
                "FROM threads WHERE id = :id",
                Map.of("id", id),
                RowMappers.THREAD
        ).stream().findFirst();
    }

    /** Insert a thread; {@code createdBy} is the author's per-circle handle snapshot. */
    public Thread insert(UUID circleId, String title, String createdBy) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO threads (id, circle_id, title, created_by) " +
                "VALUES (:id, :circleId, :title, :createdBy)",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("circleId", circleId)
                        .addValue("title", title)
                        .addValue("createdBy", createdBy)
        );
        return findById(id).orElseThrow(() ->
                new IllegalStateException("Thread vanished immediately after insert: " + id));
    }

    public List<Thread> listByCircle(UUID circleId, int page, int limit) {
        return jdbc.query(
                "SELECT id, circle_id, title, created_by, created_at " +
                "FROM threads WHERE circle_id = :circleId " +
                "ORDER BY created_at DESC " +
                "LIMIT :limit OFFSET :offset",
                new MapSqlParameterSource()
                        .addValue("circleId", circleId)
                        .addValue("limit", limit)
                        .addValue("offset", (page - 1) * limit),
                RowMappers.THREAD
        );
    }

    public long countByCircle(UUID circleId) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM threads WHERE circle_id = :circleId",
                Map.of("circleId", circleId),
                Long.class
        );
        return n == null ? 0L : n;
    }
}
