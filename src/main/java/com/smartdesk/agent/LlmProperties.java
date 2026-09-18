package com.smartdesk.agent;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "smartdesk.llm")
public record LlmProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        Duration timeout,
        double temperature,
        int maxTokens
) {
}