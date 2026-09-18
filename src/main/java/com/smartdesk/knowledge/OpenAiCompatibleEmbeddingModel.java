package com.smartdesk.knowledge;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@ConditionalOnProperty(prefix = "smartdesk.embedding", name = "enabled", havingValue = "true")
public class OpenAiCompatibleEmbeddingModel implements EmbeddingModel {

    private final EmbeddingProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleEmbeddingModel(
            EmbeddingProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("embeddingHttpClient") HttpClient httpClient
    ) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("SMARTDESK_EMBEDDING_API_KEY is required when smartdesk.embedding.enabled=true");
        }
        if (properties.model() == null || properties.model().isBlank()) {
            throw new IllegalStateException("SMARTDESK_EMBEDDING_MODEL cannot be blank");
        }
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public String modelId() {
        return "openai:" + properties.model() + ":" + properties.dimensions();
    }

    @Override
    public int dimensions() {
        return properties.dimensions();
    }

    @Override
    public float[] embed(String text) {
        return embedAll(List.of(text)).get(0);
    }

    @Override
    public List<float[]> embedAll(List<String> texts) {
        if (texts.isEmpty()) {
            return List.of();
        }
        List<float[]> result = new ArrayList<>(texts.size());
        int batchSize = Math.max(1, properties.batchSize());
        for (int start = 0; start < texts.size(); start += batchSize) {
            int end = Math.min(texts.size(), start + batchSize);
            result.addAll(requestBatch(texts.subList(start, end)));
        }
        return List.copyOf(result);
    }

    private List<float[]> requestBatch(List<String> texts) {
        try {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", properties.model());
            body.put("input", texts);
            if (properties.dimensions() > 0) {
                body.put("dimensions", properties.dimensions());
            }

            HttpRequest request = HttpRequest.newBuilder(endpoint())
                    .timeout(properties.timeout())
                    .header("Authorization", "Bearer " + properties.apiKey())
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(
                            objectMapper.writeValueAsString(body),
                            StandardCharsets.UTF_8
                    ))
                    .build();

            HttpResponse<String> response = httpClient.send(
                    request,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)
            );
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException(
                        "Embedding request failed with HTTP " + response.statusCode()
                                + ": " + response.body()
                );
            }

            JsonNode data = objectMapper.readTree(response.body()).path("data");
            if (!data.isArray() || data.size() != texts.size()) {
                throw new IllegalStateException("Embedding response count does not match input count");
            }

            List<float[]> vectors = new ArrayList<>(texts.size());
            for (int index = 0; index < texts.size(); index++) {
                JsonNode embedding = data.get(index).path("embedding");
                if (!embedding.isArray()) {
                    throw new IllegalStateException("Embedding response is missing embedding array");
                }
                float[] vector = new float[embedding.size()];
                for (int vectorIndex = 0; vectorIndex < embedding.size(); vectorIndex++) {
                    vector[vectorIndex] = (float) embedding.get(vectorIndex).asDouble();
                }
                if (properties.dimensions() > 0 && vector.length != properties.dimensions()) {
                    throw new IllegalStateException(
                            "Embedding dimension mismatch: expected " + properties.dimensions()
                                    + " but provider returned " + vector.length
                    );
                }
                vectors.add(vector);
            }
            return vectors;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Embedding request interrupted", exception);
        } catch (Exception exception) {
            if (exception instanceof IllegalStateException illegalStateException) {
                throw illegalStateException;
            }
            throw new IllegalStateException("Embedding request failed", exception);
        }
    }

    private URI endpoint() {
        String baseUrl = properties.baseUrl().endsWith("/")
                ? properties.baseUrl().substring(0, properties.baseUrl().length() - 1)
                : properties.baseUrl();
        return URI.create(baseUrl + "/embeddings");
    }
}