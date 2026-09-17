package com.smartdesk.auth;

import java.time.Instant;

public record AuthenticatedUser(
        Long userId,
        Long tenantId,
        String username,
        UserRole role,
        String jwtId,
        Instant expiresAt
) {
}