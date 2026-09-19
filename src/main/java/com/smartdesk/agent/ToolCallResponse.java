package com.smartdesk.agent;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDateTime;

public record ToolCallResponse(
        Long id,
        String toolName,
        JsonNode arguments,
        JsonNode result,
        Boolean success,
        String errorMessage,
        Long durationMs,
        LocalDateTime startedAt,
        LocalDateTime finishedAt
) {
}
