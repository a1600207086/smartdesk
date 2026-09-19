package com.smartdesk.feedback;

import java.time.LocalDateTime;

public record MessageFeedbackResponse(
        Long id,
        Long messageId,
        FeedbackRating rating,
        String comment,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public static MessageFeedbackResponse from(MessageFeedbackEntity entity) {
        return new MessageFeedbackResponse(
                entity.getId(),
                entity.getMessageId(),
                entity.getRating(),
                entity.getComment(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
