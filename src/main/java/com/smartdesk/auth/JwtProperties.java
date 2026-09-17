package com.smartdesk.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "smartdesk.security.jwt")
public record JwtProperties(
        String issuer,
        String secret,
        Duration expiration
) {
}