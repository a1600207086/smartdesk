package com.smartdesk.conversation;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "smartdesk.conversation")
public record ConversationProperties(
        int recentMemorySize,
        Duration memoryTtl,
        int defaultPageSize,
        int maxPageSize,
        int contextWindowSize,
        int maxContextChars
) {
}