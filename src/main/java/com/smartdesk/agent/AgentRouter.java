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

        return AgentDecision.direct();
    }
}