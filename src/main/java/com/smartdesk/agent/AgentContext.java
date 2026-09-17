package com.smartdesk.agent;

public record AgentContext(
        Long conversationId,
        Long tenantId,
        Long userId
) {
}