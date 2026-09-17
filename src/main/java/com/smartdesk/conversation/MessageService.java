package com.smartdesk.conversation;

import com.smartdesk.auth.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MessageService {

    private final MessageMapper messageMapper;
    private final ConversationMapper conversationMapper;
    private final ConversationService conversationService;
    private final ConversationMemoryService memoryService;
    private final ConversationProperties properties;

    public MessageService(
            MessageMapper messageMapper,
            ConversationMapper conversationMapper,
            ConversationService conversationService,
            ConversationMemoryService memoryService,
            ConversationProperties properties
    ) {
        this.messageMapper = messageMapper;
        this.conversationMapper = conversationMapper;
        this.conversationService = conversationService;
        this.memoryService = memoryService;
        this.properties = properties;
    }

    @Transactional
    public MessageResponse appendUserMessage(
            Long conversationId,
            AuthenticatedUser user,
            CreateUserMessageRequest request
    ) {
        conversationService.requireOwnedConversation(conversationId, user);
        return appendMessage(conversationId, MessageRole.USER, request.content().trim());
    }

    @Transactional
    public MessageResponse appendAssistantMessage(
            Long conversationId,
            AuthenticatedUser user,
            String content
    ) {
        conversationService.requireOwnedConversation(conversationId, user);
        return appendMessage(conversationId, MessageRole.ASSISTANT, content);
    }

    @Transactional(readOnly = true)
    public MessagePageResponse findMessages(
            Long conversationId,
            AuthenticatedUser user,
            Integer requestedLimit,
            Long beforeId
    ) {
        conversationService.requireOwnedConversation(conversationId, user);
        int limit = normalizeLimit(requestedLimit);

        if (beforeId == null) {
            List<MessageResponse> cached = memoryService.getRecent(conversationId, limit);
            if (!cached.isEmpty()) {
                return fromCache(cached, limit);
            }
        }

        List<MessageEntity> rows = messageMapper.findPage(conversationId, beforeId, limit + 1);
        boolean hasMore = rows.size() > limit;
        if (hasMore) {
            rows = new ArrayList<>(rows.subList(0, limit));
        }

        Collections.reverse(rows);
        List<MessageResponse> items = rows.stream()
                .map(MessageResponse::from)
                .toList();

        if (beforeId == null) {
            memoryService.replaceRecent(conversationId, items);
        }

        Long nextBeforeId = hasMore && !items.isEmpty() ? items.get(0).id() : null;
        return new MessagePageResponse(items, nextBeforeId, hasMore);
    }

    private MessageResponse appendMessage(
            Long conversationId,
            MessageRole role,
            String content
    ) {
        LocalDateTime now = LocalDateTime.now();
        MessageEntity message = new MessageEntity();
        message.setConversationId(conversationId);
        message.setRole(role);
        message.setContent(content);
        message.setCreatedAt(now);

        messageMapper.insert(message);
        conversationMapper.touchUpdatedAt(conversationId, now);

        MessageResponse response = MessageResponse.from(message);
        memoryService.append(conversationId, response);
        return response;
    }

    private MessagePageResponse fromCache(List<MessageResponse> cached, int limit) {
        int fromIndex = Math.max(0, cached.size() - limit);
        List<MessageResponse> items = List.copyOf(cached.subList(fromIndex, cached.size()));
        boolean hasMore = cached.size() >= limit;
        Long nextBeforeId = hasMore && !items.isEmpty() ? items.get(0).id() : null;
        return new MessagePageResponse(items, nextBeforeId, hasMore);
    }

    private int normalizeLimit(Integer requestedLimit) {
        if (requestedLimit == null || requestedLimit <= 0) {
            return properties.defaultPageSize();
        }
        return Math.min(requestedLimit, properties.maxPageSize());
    }
}