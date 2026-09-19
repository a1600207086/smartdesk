package com.smartdesk.conversation;

import com.smartdesk.knowledge.KnowledgeCitation;

import java.time.LocalDateTime;
import java.util.List;

public record MessageResponse(
        Long id,
        Long conversationId,
        MessageRole role,
        String content,
        Integer tokenCount,
        LocalDateTime createdAt,
        List<KnowledgeCitation> citations
) {

    public MessageResponse {
        citations = citations == null ? List.of() : List.copyOf(citations);
    }

    public MessageResponse(
            Long id,
            Long conversationId,
            MessageRole role,
            String content,
            Integer tokenCount,
            LocalDateTime createdAt
    ) {
        this(id, conversationId, role, content, tokenCount, createdAt, List.of());
    }

    public static MessageResponse from(MessageEntity message) {
        return from(message, List.of());
    }

    public static MessageResponse from(
            MessageEntity message,
            List<KnowledgeCitation> citations
    ) {
        return new MessageResponse(
                message.getId(),
                message.getConversationId(),
                message.getRole(),
                message.getContent(),
                message.getTokenCount(),
                message.getCreatedAt(),
                citations
        );
    }
}
