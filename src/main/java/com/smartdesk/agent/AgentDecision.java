package com.smartdesk.agent;

import java.util.Map;

public record AgentDecision(
        AgentRoute route,
        String toolName,
        Map<String, Object> arguments
) {

    public static AgentDecision direct() {
        return new AgentDecision(AgentRoute.DIRECT, null, Map.of());
    }

    public static AgentDecision toolCall(String toolName, Map<String, Object> arguments) {
        return new AgentDecision(AgentRoute.TOOL_CALL, toolName, Map.copyOf(arguments));
    }
}