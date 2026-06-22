package com.anoncircles.discussions.repository;

import com.anoncircles.discussions.domain.Circle;
import com.anoncircles.discussions.domain.Membership;
import com.anoncircles.discussions.domain.Message;
import com.anoncircles.discussions.domain.Thread;
import com.anoncircles.discussions.domain.User;
import org.springframework.jdbc.core.RowMapper;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;

/**
 * Centralised {@link RowMapper} instances for every {@code domain/} record.
 */
public final class RowMappers {

    private RowMappers() {}

    public static final RowMapper<User> USER = (rs, i) -> new User(
            uuid(rs, "id"),
            rs.getString("display_name"),
            instant(rs, "created_at")
    );

    public static final RowMapper<Circle> CIRCLE = (rs, i) -> new Circle(
            uuid(rs, "id"),
            rs.getString("topic"),
            rs.getString("description"),
            uuid(rs, "admin_user_id"),
            rs.getInt("member_count"),
            instant(rs, "created_at")
    );

    public static final RowMapper<Membership> MEMBERSHIP = (rs, i) -> new Membership(
            uuid(rs, "user_id"),
            uuid(rs, "circle_id"),
            rs.getString("handle"),
            instant(rs, "joined_at")
    );

    public static final RowMapper<Thread> THREAD = (rs, i) -> new Thread(
            uuid(rs, "id"),
            uuid(rs, "circle_id"),
            rs.getString("title"),
            rs.getString("created_by"),
            instant(rs, "created_at")
    );

    public static final RowMapper<Message> MESSAGE = (rs, i) -> new Message(
            uuid(rs, "id"),
            uuid(rs, "thread_id"),
            rs.getString("body"),
            rs.getString("author"),
            instant(rs, "created_at")
    );

    // ---- helpers ----

    static UUID uuid(ResultSet rs, String column) throws SQLException {
        Object value = rs.getObject(column);
        if (value == null) return null;
        if (value instanceof UUID u) return u;
        return UUID.fromString(value.toString());
    }

    static Instant instant(ResultSet rs, String column) throws SQLException {
        Timestamp ts = rs.getTimestamp(column);
        return ts == null ? null : ts.toInstant();
    }
}
