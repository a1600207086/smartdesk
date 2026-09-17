package com.smartdesk.conversation;

import java.time.LocalDateTime;

public record ConversationResponse(
        Long id,
        Long tenantId,
        Long userId,
        String title,
        ConversationStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ConversationResponse from(ConversationEntity conversation) {
        return new ConversationResponse(
                conversation.getId(),
                conversation.getTenantId(),
                conversation.getUserId(),
                conversation.getTitle(),
                conversation.getStatus(),
                conversation.getCreatedAt(),
                conversation.getUpdatedAt()
        );
    }
}