package com.smartdesk.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Service
public class AgentRunService {

    private final AgentRunMapper agentRunMapper;
    private final ToolCallLogMapper toolCallLogMapper;
    private final ObjectMapper objectMapper;

    public AgentRunService(
            AgentRunMapper agentRunMapper,
            ToolCallLogMapper toolCallLogMapper,
            ObjectMapper objectMapper
    ) {
        this.agentRunMapper = agentRunMapper;
        this.toolCallLogMapper = toolCallLogMapper;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AgentRunEntity start(
            Long conversationId,
            Long tenantId,
            Long userId,
            Long inputMessageId
    ) {
        AgentRunEntity run = new AgentRunEntity();
        run.setConversationId(conversationId);
        run.setTenantId(tenantId);
        run.setUserId(userId);
        run.setInputMessageId(inputMessageId);
        run.setStatus(AgentRunStatus.RUNNING);
        run.setStartedAt(LocalDateTime.now());
        agentRunMapper.insert(run);
        return run;
    }

    @Transactional
    public void markRoute(Long runId, AgentRoute route) {
        agentRunMapper.updateRoute(runId, route);
    }

    @Transactional
    public void complete(Long runId) {
        agentRunMapper.complete(runId, LocalDateTime.now());
    }

    @Transactional
    public void fail(Long runId, String errorMessage) {
        String safeMessage = errorMessage == null ? "Unknown agent error" : errorMessage;
        if (safeMessage.length() > 1000) {
            safeMessage = safeMessage.substring(0, 1000);
        }
        agentRunMapper.fail(runId, safeMessage, LocalDateTime.now());
    }

    @Transactional
    public void recordToolCall(
            Long runId,
            String toolName,
            Map<String, Object> arguments,
            Map<String, Object> result,
            boolean success,
            String errorMessage,
            long durationMs,
            LocalDateTime startedAt,
            LocalDateTime finishedAt
    ) {
        ToolCallLogEntity log = new ToolCallLogEntity();
        log.setAgentRunId(runId);
        log.setToolName(toolName);
        log.setArgumentsJson(toJson(arguments));
        log.setResultJson(result == null ? null : toJson(result));
        log.setSuccess(success);
        log.setErrorMessage(errorMessage);
        log.setDurationMs(durationMs);
        log.setStartedAt(startedAt);
        log.setFinishedAt(finishedAt);
        toolCallLogMapper.insert(log);
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "{\"serializationError\":\"true\"}";
        }
    }
}