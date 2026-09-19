package com.smartdesk.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateSupportTicketRequest(
        Long conversationId,

        @NotBlank(message = "subject 不能为空")
        @Size(max = 255, message = "subject 不能超过 255 个字符")
        String subject,

        @NotBlank(message = "description 不能为空")
        @Size(max = 5000, message = "description 不能超过 5000 个字符")
        String description,

        TicketPriority priority
) {
}
