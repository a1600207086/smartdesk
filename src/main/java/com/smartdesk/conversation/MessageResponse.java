package com.smartdesk.conversation;

import java.time.LocalDateTime;

public record MessageResponse(
        Long id,
        Long conversationId,
        MessageRole role,
        String content,
        Integer tokenCount,
        LocalDateTime createdAt
) {

    public static MessageResponse from(MessageEntity message) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getRole(),
                message.getContent(),
                message.getTokenCount(),
                message.getCreatedAt()
        );
    }
}