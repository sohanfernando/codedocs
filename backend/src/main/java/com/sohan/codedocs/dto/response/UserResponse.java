package com.sohan.codedocs.dto.response;

import com.sohan.codedocs.entity.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String email, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getEmail(), user.getCreatedAt());
    }
}
