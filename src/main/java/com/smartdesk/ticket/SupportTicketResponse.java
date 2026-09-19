package com.smartdesk.ticket;

import java.time.LocalDateTime;

public record SupportTicketResponse(
        Long id,
        String ticketNo,
        Long userId,
        Long conversationId,
        String subject,
        String description,
        TicketPriority priority,
        TicketStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime resolvedAt
) {

    public static SupportTicketResponse from(SupportTicketEntity ticket) {
        return new SupportTicketResponse(
                ticket.getId(),
                ticket.getTicketNo(),
                ticket.getUserId(),
                ticket.getConversationId(),
                ticket.getSubject(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt(),
                ticket.getResolvedAt()
        );
    }
}
