package com.smartdesk.auth;

import java.time.LocalDateTime;

public record UserProfile(
        Long id,
        Long tenantId,
        String username,
        String displayName,
        UserRole role,
        AppUserStatus status,
        LocalDateTime lastLoginAt,
        LocalDateTime createdAt
) {

    public static UserProfile from(UserEntity user) {
        return new UserProfile(
                user.getId(),
                user.getTenantId(),
                user.getUsername(),
                user.getDisplayName(),
                user.getRole(),
                user.getStatus(),
                user.getLastLoginAt(),
                user.getCreatedAt()
        );
    }
}