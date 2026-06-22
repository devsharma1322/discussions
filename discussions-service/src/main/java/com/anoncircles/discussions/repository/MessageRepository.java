package com.anoncircles.discussions.repository;

import com.anoncircles.discussions.domain.Message;
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
public class MessageRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public Optional<Message> findById(UUID id) {
        return jdbc.query(
                "SELECT id, thread_id, body, author, created_at " +
                "FROM messages WHERE id = :id",
                Map.of("id", id),
                RowMappers.MESSAGE
        ).stream().findFirst();
    }

    /** Insert a message; {@code author} is the poster's per-circle handle snapshot. */
    public Message insert(UUID threadId, String body, String author) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO messages (id, thread_id, body, author) " +
                "VALUES (:id, :threadId, :body, :author)",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("threadId", threadId)
                        .addValue("body", body)
                        .addValue("author", author)
        );
        return findById(id).orElseThrow(() ->
                new IllegalStateException("Message vanished immediately after insert: " + id));
    }

    public List<Message> listByThread(UUID threadId, int page, int limit) {
        return jdbc.query(
                "SELECT id, thread_id, body, author, created_at " +
                "FROM messages WHERE thread_id = :threadId " +
                "ORDER BY created_at ASC " +
                "LIMIT :limit OFFSET :offset",
                new MapSqlParameterSource()
                        .addValue("threadId", threadId)
                        .addValue("limit", limit)
                        .addValue("offset", (page - 1) * limit),
                RowMappers.MESSAGE
        );
    }

    public long countByThread(UUID threadId) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM messages WHERE thread_id = :threadId",
                Map.of("threadId", threadId),
                Long.class
        );
        return n == null ? 0L : n;
    }
}
