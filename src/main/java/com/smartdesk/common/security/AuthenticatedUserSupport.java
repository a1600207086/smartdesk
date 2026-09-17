package com.smartdesk.common.security;

import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.common.error.UnauthorizedException;
import org.springframework.security.core.Authentication;

public final class AuthenticatedUserSupport {

    private AuthenticatedUserSupport() {
    }

    public static AuthenticatedUser require(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof AuthenticatedUser user)) {
            throw new UnauthorizedException("请先登录");
        }
        return user;
    }
}