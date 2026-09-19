package com.smartdesk.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Component
@ConditionalOnProperty(prefix = "smartdesk.llm", name = "enabled", havingValue = "false", matchIfMissing = true)
public class MockAgentChatModel implements AgentChatModel {

    private static final String TOOL_CONTEXT_PREFIX = "TOOL_CONTEXT_JSON:";

    private final ObjectMapper objectMapper;

    public MockAgentChatModel(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void stream(AgentModelRequest request, AgentChunkConsumer consumer) throws Exception {
        String response = buildResponse(request.messages());
        for (String chunk : splitIntoSentenceChunks(response)) {
            consumer.accept(chunk);
            Thread.sleep(10);
        }
    }

    private List<String> splitIntoSentenceChunks(String text) {
        List<String> chunks = new ArrayList<>();
        int start = 0;
        for (int index = 0; index < text.length(); index++) {
            if (text.charAt(index) == '。') {
                chunks.add(text.substring(start, index + 1));
                start = index + 1;
            }
        }
        if (start < text.length()) {
            chunks.add(text.substring(start));
        }
        return chunks;
    }

    private String buildResponse(List<AgentModelMessage> messages) {
        String toolContext = messages.stream()
                .filter(message -> "system".equals(message.role()))
                .filter(message -> message.content().startsWith(TOOL_CONTEXT_PREFIX))
                .map(AgentModelMessage::content)
                .reduce((first, second) -> second)
                .orElse(null);

        if (toolContext != null) {
            return buildToolResponse(toolContext.substring(TOOL_CONTEXT_PREFIX.length()));
        }

        String lastUserMessage = messages.stream()
                .filter(message -> "user".equals(message.role()))
                .map(AgentModelMessage::content)
                .reduce((first, second) -> second)
                .orElse("");

        return "Mock Agent 已收到你的问题：" + lastUserMessage
                + "。当前已完成会话记忆和工具执行链路验证。";
    }

    private String buildToolResponse(String contextJson) {
        try {
            JsonNode context = objectMapper.readTree(contextJson);
            String toolName = context.path("toolName").asText();
            JsonNode result = context.path("result");
            if ("searchKnowledge".equals(toolName)) {
                return buildKnowledgeResponse(result);
            }
            if ("queryOrder".equals(toolName)) {
                return buildOrderResponse(result);
            }
            if ("createSupportTicket".equals(toolName)) {
                return buildTicketResponse(result);
            }
            return "工具调用已经完成。";
        } catch (Exception exception) {
            return "工具结果暂时无法解析，请稍后重试。";
        }
    }

    private String buildKnowledgeResponse(JsonNode result) {
        JsonNode matches = result.path("matches");
        if (!matches.isArray() || matches.isEmpty()) {
            return "知识库中没有找到足够的信息，请补充问题或联系人工客服。";
        }

        String query = result.path("query").asText("");
        BestSentence best = findBestSentence(query, matches);
        JsonNode source = matches.get(best.matchIndex());
        String title = source.path("documentTitle").asText("未命名文档");
        long documentId = source.path("documentId").asLong();
        int chunkIndex = source.path("chunkIndex").asInt();
        int citationIndex = best.matchIndex() + 1;

        return "根据知识库，" + best.text() + "[" + citationIndex + "]\n"
                + "来源：[" + citationIndex + "] " + title
                + "（文档 ID " + documentId + "，片段 " + chunkIndex + "）";
    }

    private BestSentence findBestSentence(String query, JsonNode matches) {
        BestSentence best = null;
        for (int matchIndex = 0; matchIndex < matches.size(); matchIndex++) {
            String content = matches.get(matchIndex).path("content").asText("");
            for (String candidate : content.split("(?<=[。！？!?])|\\R")) {
                String sentence = candidate.trim();
                if (sentence.isEmpty()) {
                    continue;
                }
                int score = relevanceScore(query, sentence);
                if (best == null || score > best.score()) {
                    best = new BestSentence(matchIndex, sentence, score);
                }
            }
        }
        if (best != null) {
            return best;
        }
        return new BestSentence(0, matches.get(0).path("content").asText("未找到答案"), 0);
    }

    private int relevanceScore(String query, String candidate) {
        Set<Integer> queryCharacters = new HashSet<>();
        query.codePoints()
                .filter(Character::isLetterOrDigit)
                .forEach(queryCharacters::add);

        int score = 0;
        Set<Integer> matched = new HashSet<>();
        for (int codePoint : candidate.codePoints().toArray()) {
            if (queryCharacters.contains(codePoint) && matched.add(codePoint)) {
                score++;
            }
        }

        String normalizedQuery = query.replaceAll("[^\\p{L}\\p{N}]", "");
        for (int index = 0; index + 1 < normalizedQuery.length(); index++) {
            String pair = normalizedQuery.substring(index, index + 2);
            if (candidate.contains(pair)) {
                score += 3;
            }
        }
        return score;
    }

    private String buildOrderResponse(JsonNode result) {
        return "订单 " + result.path("orderNo").asText("未知")
                + " 当前状态为 " + result.path("status").asText("未知")
                + "，承运商为 " + result.path("carrier").asText("未知")
                + "，预计送达日期为 " + result.path("estimatedDelivery").asText("未知") + "。";
    }

    private String buildTicketResponse(JsonNode result) {
        return "已为你创建人工客服工单 " + result.path("ticketNo").asText("未知")
                + "，当前状态为 " + result.path("status").asText("OPEN")
                + "，优先级为 " + result.path("priority").asText("HIGH")
                + "。客服人员会继续跟进。";
    }

    private record BestSentence(int matchIndex, String text, int score) {
    }
}
