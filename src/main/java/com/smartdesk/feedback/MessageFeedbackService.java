package com.smartdesk.feedback;

import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.common.error.NotFoundException;
import com.smartdesk.conversation.ConversationService;
import com.smartdesk.conversation.MessageEntity;
import com.smartdesk.conversation.MessageMapper;
import com.smartdesk.conversation.MessageRole;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class MessageFeedbackService {

    private final MessageFeedbackMapper feedbackMapper;
    private final MessageMapper messageMapper;
    private final ConversationService conversationService;

    public MessageFeedbackService(
            MessageFeedbackMapper feedbackMapper,
            MessageMapper messageMapper,
            ConversationService conversationService
    ) {
        this.feedbackMapper = feedbackMapper;
        this.messageMapper = messageMapper;
        this.conversationService = conversationService;
    }

    @Transactional
    public MessageFeedbackResponse upsert(
            Long conversationId,
            Long messageId,
            AuthenticatedUser user,
            UpsertMessageFeedbackRequest request
    ) {
        requireOwnedAssistantMessage(conversationId, messageId, user);
        LocalDateTime now = LocalDateTime.now();

        MessageFeedbackEntity feedback = new MessageFeedbackEntity();
        feedback.setTenantId(user.tenantId());
        feedback.setUserId(user.userId());
        feedback.setConversationId(conversationId);
        feedback.setMessageId(messageId);
        feedback.setRating(request.rating());
        feedback.setComment(normalizeComment(request.comment()));
        feedback.setCreatedAt(now);
        feedback.setUpdatedAt(now);
        feedbackMapper.upsert(feedback);

        return MessageFeedbackResponse.from(requireFeedback(messageId, user));
    }

    @Transactional(readOnly = true)
    public MessageFeedbackResponse find(
            Long conversationId,
            Long messageId,
            AuthenticatedUser user
    ) {
        requireOwnedAssistantMessage(conversationId, messageId, user);
        return MessageFeedbackResponse.from(requireFeedback(messageId, user));
    }

    @Transactional
    public void delete(
            Long conversationId,
            Long messageId,
            AuthenticatedUser user
    ) {
        requireOwnedAssistantMessage(conversationId, messageId, user);
        int deleted = feedbackMapper.deleteByTenantIdAndUserIdAndMessageId(
                user.tenantId(),
                user.userId(),
                messageId
        );
        if (deleted == 0) {
            throw new NotFoundException("消息评价不存在: " + messageId);
        }
    }

    @Transactional(readOnly = true)
    public FeedbackSummaryResponse summarize(AuthenticatedUser user) {
        return FeedbackSummaryResponse.from(
                feedbackMapper.summarizeByTenantId(user.tenantId())
        );
    }

    private void requireOwnedAssistantMessage(
            Long conversationId,
            Long messageId,
            AuthenticatedUser user
    ) {
        conversationService.requireOwnedConversation(conversationId, user);
        MessageEntity message = messageMapper.findByIdAndConversationId(messageId, conversationId);
        if (message == null) {
            throw new NotFoundException("消息不存在: " + messageId);
        }
        if (message.getRole() != MessageRole.ASSISTANT) {
            throw new IllegalArgumentException("只能评价助手消息");
        }
    }

    private MessageFeedbackEntity requireFeedback(
            Long messageId,
            AuthenticatedUser user
    ) {
        MessageFeedbackEntity feedback = feedbackMapper.findByTenantIdAndUserIdAndMessageId(
                user.tenantId(),
                user.userId(),
                messageId
        );
        if (feedback == null) {
            throw new NotFoundException("消息评价不存在: " + messageId);
        }
        return feedback;
    }

    private String normalizeComment(String comment) {
        if (comment == null || comment.isBlank()) {
            return null;
        }
        return comment.trim();
    }
}
