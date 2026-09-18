package com.smartdesk.knowledge;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "smartdesk.embedding")
public record EmbeddingProperties(
        boolean enabled,
        String baseUrl,
        String apiKey,
        String model,
        int dimensions,
        Duration timeout,
        int batchSize
) {
}