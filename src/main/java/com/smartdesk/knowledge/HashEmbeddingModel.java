package com.smartdesk.knowledge;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@ConditionalOnProperty(prefix = "smartdesk.embedding", name = "enabled", havingValue = "false", matchIfMissing = true)
public class HashEmbeddingModel implements EmbeddingModel {

    private static final Pattern WORD_PATTERN = Pattern.compile("[a-z0-9]+");
    private static final Pattern HAN_PATTERN = Pattern.compile("\\p{IsHan}+");

    private final KnowledgeProperties properties;

    public HashEmbeddingModel(KnowledgeProperties properties) {
        this.properties = properties;
    }

    @Override
    public String modelId() {
        return "hash-v1-" + properties.embeddingDimensions();
    }

    @Override
    public float[] embed(String text) {
        float[] vector = new float[properties.embeddingDimensions()];
        for (String token : tokenize(text)) {
            int hash = token.hashCode();
            int index = Math.floorMod(hash, vector.length);
            float sign = (hash & 1) == 0 ? 1.0f : -1.0f;
            vector[index] += sign;
        }
        normalize(vector);
        return vector;
    }

    @Override
    public int dimensions() {
        return properties.embeddingDimensions();
    }

    private List<String> tokenize(String text) {
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        List<String> tokens = new ArrayList<>();

        Matcher wordMatcher = WORD_PATTERN.matcher(normalized);
        while (wordMatcher.find()) {
            tokens.add(wordMatcher.group());
        }

        Matcher hanMatcher = HAN_PATTERN.matcher(normalized);
        while (hanMatcher.find()) {
            String run = hanMatcher.group();
            for (int index = 0; index < run.length(); index++) {
                tokens.add(String.valueOf(run.charAt(index)));
            }
            for (int index = 0; index + 1 < run.length(); index++) {
                tokens.add(run.substring(index, index + 2));
            }
        }
        return tokens;
    }

    private void normalize(float[] vector) {
        double sum = 0.0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0.0) {
            return;
        }
        float norm = (float) Math.sqrt(sum);
        for (int index = 0; index < vector.length; index++) {
            vector[index] /= norm;
        }
    }
}
