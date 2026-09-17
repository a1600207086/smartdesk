package com.smartdesk.conversation;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUserMessageRequest(
        @NotBlank(message = "消息内容不能为空")
        @Size(max = 10000, message = "单条消息不能超过 10000 个字符")
        String content
) {
}