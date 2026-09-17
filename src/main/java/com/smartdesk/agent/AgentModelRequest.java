package com.smartdesk.agent;

import java.util.List;

public record AgentModelRequest(
        List<AgentModelMessage> messages
) {
}