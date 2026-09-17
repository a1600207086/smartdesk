package com.smartdesk.conversation;

import jakarta.validation.constraints.Size;

public record CreateConversationRequest(
        @Size(max = 255, message = "会话标题不能超过 255 个字符")
        String title
) {
}