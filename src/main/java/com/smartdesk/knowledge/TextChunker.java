package com.smartdesk.knowledge;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    private final KnowledgeProperties properties;

    public TextChunker(KnowledgeProperties properties) {
        this.properties = properties;
        if (properties.chunkOverlap() >= properties.chunkSize()) {
            throw new IllegalStateException("smartdesk.knowledge.chunk-overlap must be smaller than chunk-size");
        }
    }

    public List<String> split(String content) {
        String normalized = normalize(content);
        List<String> chunks = new ArrayList<>();
        if (normalized.isBlank()) {
            return chunks;
        }

        int start = 0;
        while (start < normalized.length()) {
            int hardEnd = Math.min(normalized.length(), start + properties.chunkSize());
            int end = findNaturalEnd(normalized, start, hardEnd);
            String chunk = normalized.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }
            if (end >= normalized.length()) {
                break;
            }
            start = Math.max(start + 1, end - properties.chunkOverlap());
        }
        return chunks;
    }

    private int findNaturalEnd(String content, int start, int hardEnd) {
        if (hardEnd >= content.length()) {
            return content.length();
        }
        int minimum = start + (int) (properties.chunkSize() * 0.6);
        for (int index = hardEnd; index >= minimum; index--) {
            char current = content.charAt(index - 1);
            if (current == '\n' || current == '。' || current == '！' || current == '？'
                    || current == '.' || current == '!' || current == '?') {
                return index;
            }
        }
        return hardEnd;
    }

    private String normalize(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("\r\n", "\n")
                .replace('\r', '\n')
                .replaceAll("[\\t ]+", " ")
                .replaceAll("\\n{3,}", "\n\n")
                .trim();
    }
}