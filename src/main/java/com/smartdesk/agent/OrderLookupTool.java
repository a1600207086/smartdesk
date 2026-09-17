package com.smartdesk.agent;

import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class OrderLookupTool implements AgentTool {

    @Override
    public String name() {
        return "queryOrder";
    }

    @Override
    public String description() {
        return "Query order status by order number. Argument: orderNo";
    }

    @Override
    public Map<String, Object> execute(
            Map<String, Object> arguments,
            AgentContext context
    ) {
        Object orderNo = arguments.get("orderNo");
        if (orderNo == null || orderNo.toString().isBlank()) {
            throw new IllegalArgumentException("orderNo is required");
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("orderNo", orderNo.toString());
        result.put("status", "SHIPPED");
        result.put("carrier", "SF Express");
        result.put("estimatedDelivery", "2026-09-20");
        result.put("message", "Mock order data for agent tool execution");
        return result;
    }
}