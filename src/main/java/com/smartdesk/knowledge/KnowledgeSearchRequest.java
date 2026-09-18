package com.smartdesk.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record KnowledgeSearchRequest(
        @NotBlank(message = "检索问题不能为空")
        String query,

        @Min(value = 1, message = "topK 必须大于 0")
        @Max(value = 20, message = "topK 不能超过 20")
        Integer topK
) {
}