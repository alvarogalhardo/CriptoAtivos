package com.criptoativos.user.dto;

import com.criptoativos.user.User;
import java.time.Instant;
import java.util.UUID;

/** Deliberately omits passwordHash and cpf — neither belongs in an API response. */
public record UserResponse(
        UUID id,
        String name,
        String email,
        String role,
        boolean twoFactorEnabled,
        Instant createdAt) {

    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole().name(),
                user.isTwoFactorEnabled(),
                user.getCreatedAt());
    }
}
