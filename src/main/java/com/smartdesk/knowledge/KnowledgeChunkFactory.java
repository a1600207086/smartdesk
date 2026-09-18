package com.smartdesk.knowledge;

import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component
public class KnowledgeChunkFactory {

    private final TextChunker textChunker;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingVectorCodec vectorCodec;

    public KnowledgeChunkFactory(
            TextChunker textChunker,
            EmbeddingModel embeddingModel,
            EmbeddingVectorCodec vectorCodec
    ) {
        this.textChunker = textChunker;
        this.embeddingModel = embeddingModel;
        this.vectorCodec = vectorCodec;
    }

    public String modelId() {
        return embeddingModel.modelId();
    }

    public List<KnowledgeChunkEntity> createChunks(
            Long documentId,
            Long tenantId,
            String content,
            LocalDateTime createdAt
    ) {
        List<String> chunks = textChunker.split(content);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("文档内容无法生成有效切片");
        }

        List<float[]> vectors = embeddingModel.embedAll(chunks);
        if (vectors.size() != chunks.size()) {
            throw new IllegalStateException("Embedding count does not match chunk count");
        }

        List<KnowledgeChunkEntity> entities = new ArrayList<>(chunks.size());
        for (int index = 0; index < chunks.size(); index++) {
            String chunk = chunks.get(index);
            KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
            entity.setDocumentId(documentId);
            entity.setTenantId(tenantId);
            entity.setChunkIndex(index);
            entity.setContent(chunk);
            entity.setEmbeddingJson(vectorCodec.encode(vectors.get(index)));
            entity.setTokenCount(Math.max(1, chunk.length() / 4));
            entity.setCreatedAt(createdAt);
            entities.add(entity);
        }
        return entities;
    }
}