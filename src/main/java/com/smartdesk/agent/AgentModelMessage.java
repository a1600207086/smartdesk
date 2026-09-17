package com.smartdesk.agent;

public record AgentModelMessage(
        String role,
        String content
) {
}