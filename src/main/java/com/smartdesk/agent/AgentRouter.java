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

        String lower = lastUserMessage.toLowerCase();
        if (lower.contains("转人工") || lower.contains("人工客服")
                || lower.contains("创建工单") || lower.contains("提交工单")
                || lower.contains("我要投诉") || lower.contains("需要投诉")
                || lower.contains("human agent") || lower.contains("support ticket")) {
            return AgentDecision.toolCall(
                    "createSupportTicket",
                    Map.of("reason", lastUserMessage)
            );
        }

        Matcher matcher = ORDER_NUMBER_PATTERN.matcher(lastUserMessage);
        if (matcher.find()) {
            return AgentDecision.toolCall(
                    "queryOrder",
                    Map.of("orderNo", matcher.group(1).toUpperCase())
            );
        }

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
