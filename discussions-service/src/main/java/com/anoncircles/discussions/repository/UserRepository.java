package com.anoncircles.discussions.repository;

import com.anoncircles.discussions.domain.User;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for the {@code users} table. Pure-anonymous: id + display_name +
 * created_at. All queries use named parameters (no string concatenation) so
 * SQL injection is structurally impossible.
 */
@Repository
@RequiredArgsConstructor
public class UserRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public Optional<User> findById(UUID id) {
        return jdbc.query(
                "SELECT id, display_name, created_at FROM users WHERE id = :id",
                Map.of("id", id),
                RowMappers.USER
        ).stream().findFirst();
    }

    /**
     * Insert a new anonymous user. Returns the persisted row.
     */
    public User insert(String displayName) {
        UUID id = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO users (id, display_name) VALUES (:id, :displayName)",
                new MapSqlParameterSource()
                        .addValue("id", id)
                        .addValue("displayName", displayName)
        );
        return findById(id).orElseThrow(() ->
                new IllegalStateException("User vanished immediately after insert: " + id));
    }
}
