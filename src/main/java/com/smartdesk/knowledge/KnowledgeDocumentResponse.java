package com.smartdesk.knowledge;

import java.time.LocalDateTime;

public record KnowledgeDocumentResponse(
        Long id,
        String title,
        DocumentSourceType sourceType,
        String sourceUri,
        DocumentStatus status,
        int chunkCount,
        LocalDateTime createdAt
) {
}