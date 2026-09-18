package com.smartdesk.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.Iterator;
import java.util.stream.Stream;

@Component
@ConditionalOnProperty(prefix = "smartdesk.llm", name = "enabled", havingValue = "true")
public class OpenAiCompatibleAgentChatModel implements AgentChatModel {

    private static final Logger log = LoggerFactory.getLogger(OpenAiCompatibleAgentChatModel.class);
    private static final String DATA_PREFIX = "data:";

    private final LlmProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public OpenAiCompatibleAgentChatModel(
            LlmProperties properties,
            ObjectMapper objectMapper,
            @Qualifier("llmHttpClient") HttpClient httpClient
    ) {
        if (properties.apiKey() == null || properties.apiKey().isBlank()) {
            throw new IllegalStateException("SMARTDESK_LLM_API_KEY is required when smartdesk.llm.enabled=true");
        }
        if (properties.model() == null || properties.model().isBlank()) {
            throw new IllegalStateException("SMARTDESK_LLM_MODEL cannot be blank");
        }
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    @Override
    public void stream(AgentModelRequest request, AgentChunkConsumer consumer) throws Exception {
        String requestJson = objectMapper.writeValueAsString(buildRequestBody(request));
        HttpRequest httpRequest = HttpRequest.newBuilder(endpoint())
                .timeout(properties.timeout())
                .header("Authorization", "Bearer " + properties.apiKey())
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")
                .POST(HttpRequest.BodyPublishers.ofString(requestJson, StandardCharsets.UTF_8))
                .build();

        HttpResponse<Stream<String>> response = httpClient.send(
                httpRequest,
                HttpResponse.BodyHandlers.ofLines()
        );

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            String errorBody;
            try (Stream<String> lines = response.body()) {
                errorBody = lines.collect(Collectors.joining("\n"));
            }
            throw new IllegalStateException(
                    "LLM request failed with HTTP " + response.statusCode() + ": " + errorBody
            );
        }

        try (Stream<String> lines = response.body()) {
            Iterator<String> iterator = lines.iterator();
            while (iterator.hasNext()) {
                processLine(iterator.next(), consumer);
            }
        }
    }

    private Map<String, Object> buildRequestBody(AgentModelRequest request) {
        List<Map<String, String>> messages = request.messages().stream()
                .map(message -> Map.of(
                        "role", message.role(),
                        "content", message.content()
                ))
                .toList();

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", properties.model());
        body.put("stream", true);
        body.put("temperature", properties.temperature());
        body.put("max_tokens", properties.maxTokens());
        body.put("messages", messages);
        return body;
    }

    private void processLine(String line, AgentChunkConsumer consumer) throws IOException {
        if (line == null || line.isBlank() || !line.startsWith(DATA_PREFIX)) {
            return;
        }

        String payload = line.substring(DATA_PREFIX.length()).trim();
        if ("[DONE]".equals(payload)) {
            return;
        }

        try {
            JsonNode root = objectMapper.readTree(payload);
            JsonNode choices = root.path("choices");
            if (!choices.isArray() || choices.isEmpty()) {
                return;
            }

            JsonNode firstChoice = choices.get(0);
            JsonNode content = firstChoice.path("delta").path("content");
            if (!content.isTextual()) {
                content = firstChoice.path("text");
            }
            if (content.isTextual() && !content.asText().isEmpty()) {
                consumer.accept(content.asText());
            }
        } catch (JsonProcessingException exception) {
            log.warn("Unable to parse LLM stream event: {}", payload, exception);
            return;
        }

        // Consumer callbacks are intentionally outside the JSON catch block so
        // client disconnects propagate instead of being silently swallowed.
    }

    private URI endpoint() {
        String baseUrl = properties.baseUrl().endsWith("/")
                ? properties.baseUrl().substring(0, properties.baseUrl().length() - 1)
                : properties.baseUrl();
        return URI.create(baseUrl + "/chat/completions");
    }
}
