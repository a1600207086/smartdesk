package com.smartdesk.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.common.error.NotFoundException;
import com.smartdesk.conversation.ConversationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class AgentObservabilityService {

    private final AgentRunMapper agentRunMapper;
    private final ToolCallLogMapper toolCallLogMapper;
    private final ConversationService conversationService;
    private final ObjectMapper objectMapper;

    public AgentObservabilityService(
            AgentRunMapper agentRunMapper,
            ToolCallLogMapper toolCallLogMapper,
            ConversationService conversationService,
            ObjectMapper objectMapper
    ) {
        this.agentRunMapper = agentRunMapper;
        this.toolCallLogMapper = toolCallLogMapper;
        this.conversationService = conversationService;
        this.objectMapper = objectMapper;
    }

    @Transactional(readOnly = true)
    public List<AgentRunResponse> findRuns(
            Long conversationId,
            Integer requestedLimit,
            AuthenticatedUser user
    ) {
        conversationService.requireOwnedConversation(conversationId, user);
        int limit = requestedLimit == null ? 20 : requestedLimit;
        return agentRunMapper.findAllByConversationId(conversationId, limit).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AgentRunResponse findRun(Long conversationId, Long runId, AuthenticatedUser user) {
        conversationService.requireOwnedConversation(conversationId, user);
        AgentRunEntity run = agentRunMapper.findByIdAndConversationId(runId, conversationId);
        if (run == null) {
            throw new NotFoundException("Agent 运行记录不存在: " + runId);
        }
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public AgentMetricsResponse summarize(AuthenticatedUser user) {
        return AgentMetricsResponse.from(agentRunMapper.summarizeByTenantId(user.tenantId()));
    }

    private AgentRunResponse toResponse(AgentRunEntity run) {
        List<ToolCallResponse> tools = toolCallLogMapper.findAllByAgentRunId(run.getId()).stream()
                .map(this::toToolResponse).toList();
        return AgentRunResponse.from(run, tools);
    }

    private ToolCallResponse toToolResponse(ToolCallLogEntity log) {
        return new ToolCallResponse(
                log.getId(), log.getToolName(), parseJson(log.getArgumentsJson()), parseJson(log.getResultJson()),
                log.getSuccess(), log.getErrorMessage(), log.getDurationMs(), log.getStartedAt(), log.getFinishedAt()
        );
    }

    private JsonNode parseJson(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return objectMapper.readTree(json);
        } catch (Exception exception) {
            return objectMapper.getNodeFactory().textNode(json);
        }
    }
}
