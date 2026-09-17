package com.smartdesk.agent;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class MockAgentChatModel implements AgentChatModel {

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
        String toolResult = messages.stream()
                .filter(message -> "system".equals(message.role()))
                .filter(message -> message.content().startsWith("TOOL_RESULT:"))
                .map(AgentModelMessage::content)
                .reduce((first, second) -> second)
                .orElse(null);

        if (toolResult != null) {
            return "根据工具查询结果，" + toolResult.substring("TOOL_RESULT:".length())
                    + "。当前回答由 Mock Agent 生成，后续可替换为真实大模型。";
        }

        String lastUserMessage = messages.stream()
                .filter(message -> "user".equals(message.role()))
                .map(AgentModelMessage::content)
                .reduce((first, second) -> second)
                .orElse("");

        return "Mock Agent 已收到你的问题：" + lastUserMessage
                + "。当前已完成会话记忆和工具执行链路验证。";
    }
}