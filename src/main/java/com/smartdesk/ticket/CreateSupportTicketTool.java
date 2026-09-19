package com.smartdesk.ticket;

import com.smartdesk.agent.AgentContext;
import com.smartdesk.agent.AgentTool;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class CreateSupportTicketTool implements AgentTool {

    private final SupportTicketService ticketService;

    public CreateSupportTicketTool(SupportTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @Override
    public String name() {
        return "createSupportTicket";
    }

    @Override
    public String description() {
        return "Create a human-support ticket for the current conversation. Argument: reason";
    }

    @Override
    public Map<String, Object> execute(
            Map<String, Object> arguments,
            AgentContext context
    ) {
        String reason = String.valueOf(arguments.getOrDefault("reason", "用户请求人工客服"));
        SupportTicketResponse ticket = ticketService.createFromAgent(
                context.tenantId(),
                context.userId(),
                context.conversationId(),
                reason
        );

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("ticketId", ticket.id());
        result.put("ticketNo", ticket.ticketNo());
        result.put("status", ticket.status().name());
        result.put("priority", ticket.priority().name());
        result.put("subject", ticket.subject());
        return result;
    }
}
