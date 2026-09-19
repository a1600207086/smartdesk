package com.smartdesk.ticket;

import jakarta.validation.constraints.NotNull;

public record UpdateTicketStatusRequest(
        @NotNull(message = "status 不能为空")
        TicketStatus status
) {
}
