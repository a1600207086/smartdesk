package com.smartdesk.feedback;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpsertMessageFeedbackRequest(
        @NotNull(message = "rating 不能为空")
        FeedbackRating rating,

        @Size(max = 500, message = "comment 不能超过 500 个字符")
        String comment
) {
}
