package com.smartdesk.conversation;

import com.smartdesk.auth.AuthenticatedUser;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.List;

@Service
public class ConversationContextService {

    private final MessageService messageService;
    private final ConversationProperties properties;

    public ConversationContextService(
            MessageService messageService,
            ConversationProperties properties
    ) {
        this.messageService = messageService;
        this.properties = properties;
    }

    public List<MessageResponse> loadContext(
            Long conversationId,
            AuthenticatedUser user
    ) {
        List<MessageResponse> recent = messageService.findMessages(
                conversationId,
                user,
                properties.contextWindowSize(),
                null
        ).items();

        LinkedList<MessageResponse> selected = new LinkedList<>();
        int usedChars = 0;
        for (int index = recent.size() - 1; index >= 0; index--) {
            MessageResponse message = recent.get(index);
            int messageChars = message.content().length();
            if (!selected.isEmpty()
                    && usedChars + messageChars > properties.maxContextChars()) {
                break;
            }
            selected.addFirst(message);
            usedChars += messageChars;
        }
        return List.copyOf(selected);
    }
}