package com.smartdesk.agent;

import java.util.Map;

public interface AgentTool {

    String name();

    String description();

    Map<String, Object> execute(
            Map<String, Object> arguments,
            AgentContext context
    );
}