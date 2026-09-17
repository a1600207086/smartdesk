package com.smartdesk.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "smartdesk.login-rate-limit")
public record LoginRateLimitProperties(
        int maxAttempts,
        Duration window
) {
}