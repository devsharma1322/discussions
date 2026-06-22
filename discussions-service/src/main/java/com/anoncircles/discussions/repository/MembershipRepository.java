package com.anoncircles.discussions.repository;

import com.anoncircles.discussions.domain.Membership;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Data access for the {@code memberships} join table. Every insert/delete here
 * fires the {@code member_count} trigger (migration V2) inside the same
 * transaction.
 */
@Repository
@RequiredArgsConstructor
public class MembershipRepository {

    private final NamedParameterJdbcTemplate jdbc;

    public Optional<Membership> find(UUID userId, UUID circleId) {
        return jdbc.query(
                "SELECT user_id, circle_id, handle, joined_at " +
                "FROM memberships WHERE user_id = :userId AND circle_id = :circleId",
                Map.of("userId", userId, "circleId", circleId),
                RowMappers.MEMBERSHIP
        ).stream().findFirst();
    }

    public boolean exists(UUID userId, UUID circleId) {
        Boolean present = jdbc.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM memberships " +
                "               WHERE user_id = :userId AND circle_id = :circleId)",
                Map.of("userId", userId, "circleId", circleId),
                Boolean.class
        );
        return Boolean.TRUE.equals(present);
    }

    /** Returns the handle for an existing membership, or empty if not a member. */
    public Optional<String> findHandle(UUID userId, UUID circleId) {
        return find(userId, circleId).map(Membership::handle);
    }

    /**
     * Insert a new membership row. The {@code member_count} trigger handles
     * counter bookkeeping. Returns the inserted row.
     */
    public Membership insert(UUID userId, UUID circleId, String handle) {
        jdbc.update(
                "INSERT INTO memberships (user_id, circle_id, handle) " +
                "VALUES (:userId, :circleId, :handle)",
                new MapSqlParameterSource()
                        .addValue("userId", userId)
                        .addValue("circleId", circleId)
                        .addValue("handle", handle)
        );
        return find(userId, circleId).orElseThrow(() ->
                new IllegalStateException("Membership vanished immediately after insert"));
    }

    public int delete(UUID userId, UUID circleId) {
        return jdbc.update(
                "DELETE FROM memberships WHERE user_id = :userId AND circle_id = :circleId",
                Map.of("userId", userId, "circleId", circleId)
        );
    }
}
