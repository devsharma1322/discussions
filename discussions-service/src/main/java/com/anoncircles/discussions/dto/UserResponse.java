package com.anoncircles.discussions.dto;

import com.anoncircles.discussions.domain.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String displayName,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(user.id(), user.displayName(), user.createdAt());
    }
}
