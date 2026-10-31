package com.pethop.user.dto;

import com.pethop.user.User;
import com.pethop.user.UserRole;
import java.time.Instant;
import java.util.UUID;

public record UserResponse(UUID id, String fullName, String email, String phone, UserRole role, boolean emailVerified, Instant createdAt) {
    public static UserResponse from(User user) {
        return new UserResponse(user.getId(), user.getFullName(), user.getEmail(), user.getPhone(), user.getRole(), user.isEmailVerified(), user.getCreatedAt());
    }
}
