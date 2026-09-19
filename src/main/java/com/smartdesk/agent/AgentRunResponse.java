package com.smartdesk.agent;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public record AgentRunResponse(
        Long id,
        Long conversationId,
        Long inputMessageId,
        AgentRoute route,
        AgentRunStatus status,
        String errorMessage,
        LocalDateTime startedAt,
        LocalDateTime finishedAt,
        Long durationMs,
        List<ToolCallResponse> toolCalls
) {

    public static AgentRunResponse from(AgentRunEntity run, List<ToolCallResponse> toolCalls) {
        LocalDateTime end = run.getFinishedAt() == null ? LocalDateTime.now() : run.getFinishedAt();
        long durationMs = Math.max(0, Duration.between(run.getStartedAt(), end).toMillis());
        return new AgentRunResponse(
                run.getId(), run.getConversationId(), run.getInputMessageId(), run.getRoute(),
                run.getStatus(), run.getErrorMessage(), run.getStartedAt(), run.getFinishedAt(),
                durationMs, List.copyOf(toolCalls)
        );
    }
}
