package com.smartdesk.knowledge;

public record KnowledgeSearchResult(
        Long documentId,
        String documentTitle,
        Long chunkId,
        int chunkIndex,
        String content,
        double score
) {
}