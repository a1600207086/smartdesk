package com.smartdesk.conversation;

import java.util.List;

public record MessagePageResponse(
        List<MessageResponse> items,
        Long nextBeforeId,
        boolean hasMore
) {
}