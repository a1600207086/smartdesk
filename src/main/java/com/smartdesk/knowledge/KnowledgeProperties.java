package com.smartdesk.knowledge;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "smartdesk.knowledge")
public record KnowledgeProperties(
        int chunkSize,
        int chunkOverlap,
        int embeddingDimensions,
        int defaultTopK,
        int maxTopK,
        double minScore,
        Duration cacheTtl
) {
}