package com.smartdesk.knowledge;

public record KnowledgeCitation(
        int index,
        Long documentId,
        String documentTitle,
        Long chunkId,
        int chunkIndex,
        double score,
        String snippet
) {
}
