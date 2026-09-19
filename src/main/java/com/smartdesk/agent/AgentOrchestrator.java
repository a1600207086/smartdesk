package com.smartdesk.agent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.conversation.ConversationContextService;
import com.smartdesk.conversation.CreateUserMessageRequest;
import com.smartdesk.conversation.MessageResponse;
import com.smartdesk.conversation.MessageService;
import com.smartdesk.knowledge.KnowledgeCitation;
import com.smartdesk.knowledge.KnowledgeSearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class AgentOrchestrator {

    private static final Logger log = LoggerFactory.getLogger(AgentOrchestrator.class);

    private final MessageService messageService;
    private final ConversationContextService contextService;
    private final AgentRouter agentRouter;
    private final ToolRegistry toolRegistry;
    private final AgentChatModel chatModel;
    private final AgentRunService agentRunService;
    private final ObjectMapper objectMapper;

    public AgentOrchestrator(
            MessageService messageService,
            ConversationContextService contextService,
            AgentRouter agentRouter,
            ToolRegistry toolRegistry,
            AgentChatModel chatModel,
            AgentRunService agentRunService,
            ObjectMapper objectMapper
    ) {
        this.messageService = messageService;
        this.contextService = contextService;
        this.agentRouter = agentRouter;
        this.toolRegistry = toolRegistry;
        this.chatModel = chatModel;
        this.agentRunService = agentRunService;
        this.objectMapper = objectMapper;
    }

    public void run(
            Long conversationId,
            AuthenticatedUser user,
            String content,
            SseEmitter emitter
    ) {
        AgentRunEntity run = null;
        try {
            MessageResponse userMessage = messageService.appendUserMessage(
                    conversationId,
                    user,
                    new CreateUserMessageRequest(content)
            );

            run = agentRunService.start(
                    conversationId,
                    user.tenantId(),
                    user.userId(),
                    userMessage.id()
            );

            List<MessageResponse> context = contextService.loadContext(conversationId, user);
            AgentDecision decision = agentRouter.route(context);
            agentRunService.markRoute(run.getId(), decision.route());

            send(emitter, "start", Map.of(
                    "runId", run.getId(),
                    "conversationId", conversationId
            ));
            sendRouteEvent(emitter, decision);

            Map<String, Object> toolResult = executeToolIfNeeded(run, decision, user, emitter);
            List<KnowledgeCitation> citations = extractCitations(decision, toolResult);
            sendCitationEvent(emitter, citations);

            List<AgentModelMessage> modelMessages = buildModelMessages(
                    context,
                    decision,
                    toolResult,
                    citations
            );
            StringBuilder response = new StringBuilder();
            chatModel.stream(new AgentModelRequest(modelMessages), chunk -> {
                response.append(chunk);
                send(emitter, "message", Map.of("content", chunk));
            });

            MessageResponse assistantMessage = messageService.appendAssistantMessage(
                    conversationId,
                    user,
                    response.toString(),
                    citations
            );

            agentRunService.complete(run.getId());
            send(emitter, "done", Map.of(
                    "runId", run.getId(),
                    "messageId", assistantMessage.id()
            ));
            emitter.complete();
        } catch (Exception exception) {
            log.error("Agent execution failed", exception);
            if (run != null) {
                agentRunService.fail(run.getId(), exception.getMessage());
            }
            try {
                send(emitter, "error", Map.of(
                        "message", exception.getMessage() == null
                                ? "Agent execution failed"
                                : exception.getMessage()
                ));
                emitter.complete();
            } catch (Exception sendException) {
                emitter.completeWithError(sendException);
            }
        }
    }

    private Map<String, Object> executeToolIfNeeded(
            AgentRunEntity run,
            AgentDecision decision,
            AuthenticatedUser user,
            SseEmitter emitter
    ) throws IOException {
        if (decision.route() != AgentRoute.TOOL_CALL) {
            return Map.of();
        }

        AgentTool tool = toolRegistry.require(decision.toolName());
        send(emitter, "tool", Map.of(
                "toolName", tool.name(),
                "status", "STARTED",
                "arguments", decision.arguments()
        ));

        LocalDateTime startedAt = LocalDateTime.now();
        long startedNanos = System.nanoTime();
        try {
            Map<String, Object> result = tool.execute(
                    decision.arguments(),
                    new AgentContext(run.getConversationId(), user.tenantId(), user.userId())
            );
            long durationMs = (System.nanoTime() - startedNanos) / 1_000_000;
            agentRunService.recordToolCall(
                    run.getId(),
                    tool.name(),
                    decision.arguments(),
                    result,
                    true,
                    null,
                    durationMs,
                    startedAt,
                    LocalDateTime.now()
            );
            send(emitter, "tool", Map.of(
                    "toolName", tool.name(),
                    "status", "COMPLETED",
                    "result", summarizeToolResult(tool.name(), result)
            ));
            return result;
        } catch (RuntimeException exception) {
            long durationMs = (System.nanoTime() - startedNanos) / 1_000_000;
            agentRunService.recordToolCall(
                    run.getId(),
                    tool.name(),
                    decision.arguments(),
                    null,
                    false,
                    exception.getMessage(),
                    durationMs,
                    startedAt,
                    LocalDateTime.now()
            );
            send(emitter, "tool", Map.of(
                    "toolName", tool.name(),
                    "status", "FAILED",
                    "error", exception.getMessage() == null ? "Tool failed" : exception.getMessage()
            ));
            return Map.of("error", exception.getMessage() == null ? "Tool failed" : exception.getMessage());
        }
    }

    private List<AgentModelMessage> buildModelMessages(
            List<MessageResponse> context,
            AgentDecision decision,
            Map<String, Object> toolResult,
            List<KnowledgeCitation> citations
    ) throws JsonProcessingException {
        List<AgentModelMessage> messages = new java.util.ArrayList<>();
        messages.add(new AgentModelMessage(
                "system",
                "You are SmartDesk, an after-sales assistant. Answer concisely using only the provided "
                        + "tool result. For knowledge answers, cite supporting sources as [1], [2], and say "
                        + "that no reliable answer was found when the matches are empty. Do not invent facts."
        ));
        for (MessageResponse message : context) {
            messages.add(new AgentModelMessage(
                    message.role().name().toLowerCase(),
                    message.content()
            ));
        }
        if (decision.route() == AgentRoute.TOOL_CALL) {
            Map<String, Object> toolContext = new LinkedHashMap<>();
            toolContext.put("toolName", decision.toolName());
            toolContext.put("result", toolResult);
            toolContext.put("citations", citations);
            messages.add(new AgentModelMessage(
                    "system",
                    "TOOL_CONTEXT_JSON:" + objectMapper.writeValueAsString(toolContext)
            ));
        }
        return List.copyOf(messages);
    }

    private Object summarizeToolResult(String toolName, Map<String, Object> result) {
        if (!"searchKnowledge".equals(toolName)) {
            return result;
        }
        Object matches = result.get("matches");
        int matchCount = matches instanceof List<?> list ? list.size() : 0;
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("query", result.get("query"));
        summary.put("matchCount", matchCount);
        return summary;
    }

    private List<KnowledgeCitation> extractCitations(
            AgentDecision decision,
            Map<String, Object> toolResult
    ) {
        if (!"searchKnowledge".equals(decision.toolName())) {
            return List.of();
        }
        Object matchesValue = toolResult.get("matches");
        if (!(matchesValue instanceof List<?> matches)) {
            return List.of();
        }

        List<KnowledgeCitation> citations = new java.util.ArrayList<>();
        for (Object match : matches) {
            if (!(match instanceof KnowledgeSearchResult result)) {
                continue;
            }
            citations.add(new KnowledgeCitation(
                    citations.size() + 1,
                    result.documentId(),
                    result.documentTitle(),
                    result.chunkId(),
                    result.chunkIndex(),
                    result.score(),
                    citationSnippet(result.content())
            ));
            if (citations.size() == 3) {
                break;
            }
        }
        return List.copyOf(citations);
    }

    private String citationSnippet(String content) {
        if (content == null) {
            return "";
        }
        String normalized = content.replaceAll("\\s+", " ").trim();
        return normalized.length() <= 180
                ? normalized
                : normalized.substring(0, 180) + "...";
    }

    private void sendCitationEvent(
            SseEmitter emitter,
            List<KnowledgeCitation> citations
    ) throws IOException {
        if (!citations.isEmpty()) {
            send(emitter, "citation", Map.of("citations", citations));
        }
    }

    private void sendRouteEvent(SseEmitter emitter, AgentDecision decision) throws IOException {
        Map<String, Object> routeData = new LinkedHashMap<>();
        routeData.put("route", decision.route().name());
        if (decision.toolName() != null) {
            routeData.put("toolName", decision.toolName());
        }
        send(emitter, "route", routeData);
    }

    private void send(SseEmitter emitter, String eventName, Object data) throws IOException {
        emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data, MediaType.APPLICATION_JSON));
    }
}
