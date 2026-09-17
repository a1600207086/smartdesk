package com.smartdesk.agent;

import com.smartdesk.common.error.NotFoundException;
import org.springframework.stereotype.Component;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class ToolRegistry {

    private final Map<String, AgentTool> tools;

    public ToolRegistry(List<AgentTool> registeredTools) {
        Map<String, AgentTool> indexed = new LinkedHashMap<>();
        for (AgentTool tool : registeredTools) {
            indexed.put(tool.name(), tool);
        }
        this.tools = Map.copyOf(indexed);
    }

    public AgentTool require(String name) {
        AgentTool tool = tools.get(name);
        if (tool == null) {
            throw new NotFoundException("Agent 工具不存在: " + name);
        }
        return tool;
    }

    public Collection<AgentTool> all() {
        return tools.values();
    }
}