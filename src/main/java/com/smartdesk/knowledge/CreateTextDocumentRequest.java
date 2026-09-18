package com.smartdesk.knowledge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTextDocumentRequest(
        @NotBlank(message = "文档标题不能为空")
        @Size(max = 255, message = "文档标题不能超过 255 个字符")
        String title,

        @NotBlank(message = "文档内容不能为空")
        String content,

        @Size(max = 1000, message = "来源地址不能超过 1000 个字符")
        String sourceUri
) {
}