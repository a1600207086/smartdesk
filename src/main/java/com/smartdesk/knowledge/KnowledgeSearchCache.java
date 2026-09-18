package com.smartdesk.knowledge;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@Component
public class KnowledgeSearchCache {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeSearchCache.class);
    private static final String VERSION_PREFIX = "smartdesk:knowledge:version:";
    private static final String SEARCH_PREFIX = "smartdesk:knowledge:search:";
    private static final TypeReference<List<KnowledgeSearchResult>> RESULT_LIST = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final KnowledgeProperties properties;

    public KnowledgeSearchCache(StringRedisTemplate redisTemplate, ObjectMapper objectMapper, KnowledgeProperties properties) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String currentVersion(Long tenantId) {
        try {
            String value = redisTemplate.opsForValue().get(VERSION_PREFIX + tenantId);
            return value == null ? "0" : value;
        } catch (RuntimeException exception) {
            log.warn("Unable to read knowledge cache version from Redis", exception);
            return "0";
        }
    }

    public void invalidate(Long tenantId) {
        try {
            redisTemplate.opsForValue().increment(VERSION_PREFIX + tenantId);
        } catch (RuntimeException exception) {
            log.warn("Unable to invalidate knowledge cache in Redis", exception);
        }
    }

    public List<KnowledgeSearchResult> get(Long tenantId, String query, int topK, String version) {
        try {
            String value = redisTemplate.opsForValue().get(searchKey(tenantId, query, topK, version));
            if (value == null) {
                return List.of();
            }
            return objectMapper.readValue(value, RESULT_LIST);
        } catch (RuntimeException | java.io.IOException exception) {
            log.warn("Unable to read knowledge search cache", exception);
            return List.of();
        }
    }

    public void put(Long tenantId, String query, int topK, String version, List<KnowledgeSearchResult> results) {
        try {
            redisTemplate.opsForValue().set(
                    searchKey(tenantId, query, topK, version),
                    objectMapper.writeValueAsString(results),
                    properties.cacheTtl()
            );
        } catch (RuntimeException | com.fasterxml.jackson.core.JsonProcessingException exception) {
            log.warn("Unable to write knowledge search cache", exception);
        }
    }

    private String searchKey(Long tenantId, String query, int topK, String version) {
        return SEARCH_PREFIX + tenantId + ":" + version + ":" + topK + ":" + sha256(query);
    }

    private String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to hash knowledge query", exception);
        }
    }
}