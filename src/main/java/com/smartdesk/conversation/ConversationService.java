package com.smartdesk.conversation;

import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class ConversationService {

    private final ConversationMapper conversationMapper;

    public ConversationService(ConversationMapper conversationMapper) {
        this.conversationMapper = conversationMapper;
    }

    @Transactional
    public ConversationResponse create(
            AuthenticatedUser user,
            CreateConversationRequest request
    ) {
        String title = request.title() == null || request.title().isBlank()
                ? "新会话"
                : request.title().trim();

        LocalDateTime now = LocalDateTime.now();
        ConversationEntity conversation = new ConversationEntity();
        conversation.setTenantId(user.tenantId());
        conversation.setUserId(user.userId());
        conversation.setTitle(title);
        conversation.setStatus(ConversationStatus.ACTIVE);
        conversation.setCreatedAt(now);
        conversation.setUpdatedAt(now);

        conversationMapper.insert(conversation);
        return ConversationResponse.from(conversation);
    }

    @Transactional(readOnly = true)
    public List<ConversationResponse> findAll(AuthenticatedUser user) {
        return conversationMapper.findAllByTenantIdAndUserId(user.tenantId(), user.userId())
                .stream()
                .map(ConversationResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public ConversationResponse findById(Long conversationId, AuthenticatedUser user) {
        return ConversationResponse.from(requireOwnedConversation(conversationId, user));
    }

    @Transactional(readOnly = true)
    public ConversationEntity requireOwnedConversation(
            Long conversationId,
            AuthenticatedUser user
    ) {
        ConversationEntity conversation = conversationMapper.findById(conversationId);
        if (conversation == null
                || !conversation.getTenantId().equals(user.tenantId())
                || !conversation.getUserId().equals(user.userId())) {
            throw new NotFoundException("会话不存在: " + conversationId);
        }
        return conversation;
    }
}