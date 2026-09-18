package com.smartdesk.agent;

import com.smartdesk.conversation.MessageResponse;
import com.smartdesk.conversation.MessageRole;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class AgentRouter {

    private static final Pattern ORDER_NUMBER_PATTERN =
            Pattern.compile("(?i)\\b([a-z]{1,3}\\d{4,})\\b");

    public AgentDecision route(List<MessageResponse> context) {
        String lastUserMessage = context.stream()
                .filter(message -> message.role() == MessageRole.USER)
                .map(MessageResponse::content)
                .reduce((first, second) -> second)
                .orElse("");

        Matcher matcher = ORDER_NUMBER_PATTERN.matcher(lastUserMessage);
        if (matcher.find()) {
            return AgentDecision.toolCall(
                    "queryOrder",
                    Map.of("orderNo", matcher.group(1).toUpperCase())
            );
        }

        String lower = lastUserMessage.toLowerCase();
        if (lower.contains("知识") || lower.contains("政策") || lower.contains("规则")
                || lower.contains("流程") || lower.contains("怎么") || lower.contains("如何")
                || lower.contains("退款") || lower.contains("退货") || lower.contains("售后")
                || lower.contains("说明") || lower.contains("policy") || lower.contains("knowledge")) {
            return AgentDecision.toolCall(
                    "searchKnowledge",
                    Map.of("query", lastUserMessage)
            );
        }

        return AgentDecision.direct();
    }
}
