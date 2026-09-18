package com.smartdesk.knowledge;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class KnowledgeRetrievalService {

    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeDocumentMapper documentMapper;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingVectorCodec vectorCodec;
    private final KnowledgeSearchCache searchCache;
    private final KnowledgeProperties properties;

    public KnowledgeRetrievalService(
            KnowledgeChunkMapper chunkMapper,
            KnowledgeDocumentMapper documentMapper,
            EmbeddingModel embeddingModel,
            EmbeddingVectorCodec vectorCodec,
            KnowledgeSearchCache searchCache,
            KnowledgeProperties properties
    ) {
        this.chunkMapper = chunkMapper;
        this.documentMapper = documentMapper;
        this.embeddingModel = embeddingModel;
        this.vectorCodec = vectorCodec;
        this.searchCache = searchCache;
        this.properties = properties;
    }

    @Transactional(readOnly = true)
    public List<KnowledgeSearchResult> search(Long tenantId, String query, Integer requestedTopK) {
        String normalizedQuery = query == null ? "" : query.trim();
        if (normalizedQuery.isEmpty()) {
            return List.of();
        }

        int topK = normalizeTopK(requestedTopK);
        String version = searchCache.currentVersion(tenantId);
        List<KnowledgeSearchResult> cached = searchCache.get(tenantId, normalizedQuery, topK, version);
        if (!cached.isEmpty()) {
            return cached;
        }

        List<KnowledgeChunkEntity> chunks = chunkMapper.findAllByTenantId(tenantId);
        if (chunks.isEmpty()) {
            return List.of();
        }

        Map<Long, KnowledgeDocumentEntity> documents = documentMapper.findAllByTenantId(tenantId)
                .stream()
                .collect(Collectors.toMap(KnowledgeDocumentEntity::getId, Function.identity()));

        float[] queryVector = embeddingModel.embed(normalizedQuery);
        List<Candidate> candidates = new ArrayList<>();
        for (KnowledgeChunkEntity chunk : chunks) {
            KnowledgeDocumentEntity document = documents.get(chunk.getDocumentId());
            if (document == null || document.getStatus() != DocumentStatus.READY) {
                continue;
            }
            double vectorScore = cosineSimilarity(queryVector, vectorCodec.decode(chunk.getEmbeddingJson()));
            double keywordScore = keywordScore(normalizedQuery, chunk.getContent());
            double score = vectorScore * 0.85 + keywordScore * 0.15;
            if (score >= properties.minScore()) {
                candidates.add(new Candidate(chunk, document, score));
            }
        }

        List<KnowledgeSearchResult> results = candidates.stream()
                .sorted(Comparator.comparingDouble(Candidate::score).reversed())
                .limit(topK)
                .map(candidate -> new KnowledgeSearchResult(
                        candidate.document().getId(),
                        candidate.document().getTitle(),
                        candidate.chunk().getId(),
                        candidate.chunk().getChunkIndex(),
                        candidate.chunk().getContent(),
                        round(candidate.score())
                ))
                .toList();

        searchCache.put(tenantId, normalizedQuery, topK, version, results);
        return results;
    }

    private int normalizeTopK(Integer requestedTopK) {
        if (requestedTopK == null || requestedTopK <= 0) {
            return properties.defaultTopK();
        }
        return Math.min(requestedTopK, properties.maxTopK());
    }

    private double cosineSimilarity(float[] left, float[] right) {
        int length = Math.min(left.length, right.length);
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int index = 0; index < length; index++) {
            dot += left[index] * right[index];
            leftNorm += left[index] * left[index];
            rightNorm += right[index] * right[index];
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private double keywordScore(String query, String content) {
        String compactQuery = query.toLowerCase(Locale.ROOT).replaceAll("[\\s\\p{Punct}]+", "");
        String normalizedContent = content.toLowerCase(Locale.ROOT);
        if (compactQuery.isEmpty()) {
            return 0.0;
        }

        int matched = 0;
        for (int index = 0; index < compactQuery.length(); index++) {
            if (normalizedContent.contains(String.valueOf(compactQuery.charAt(index)))) {
                matched++;
            }
        }
        return matched / (double) compactQuery.length();
    }

    private double round(double value) {
        return Math.round(value * 10000.0) / 10000.0;
    }

    private record Candidate(
            KnowledgeChunkEntity chunk,
            KnowledgeDocumentEntity document,
            double score
    ) {
    }
}