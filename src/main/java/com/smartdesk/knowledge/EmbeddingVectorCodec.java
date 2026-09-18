package com.smartdesk.knowledge;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

@Component
public class EmbeddingVectorCodec {

    private final ObjectMapper objectMapper;

    public EmbeddingVectorCodec(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String encode(float[] vector) {
        try {
            return objectMapper.writeValueAsString(vector);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to encode embedding vector", exception);
        }
    }

    public float[] decode(String value) {
        try {
            return objectMapper.readValue(value, float[].class);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Unable to decode embedding vector", exception);
        }
    }
}