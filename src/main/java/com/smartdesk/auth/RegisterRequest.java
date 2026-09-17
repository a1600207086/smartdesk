package com.smartdesk.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "租户编码不能为空")
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z0-9_-]{2,63}$",
                message = "租户编码格式不正确"
        )
        String tenantCode,

        @NotBlank(message = "用户名不能为空")
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z0-9_-]{2,31}$",
                message = "用户名需为 3-32 位字母、数字、下划线或短横线，且以字母开头"
        )
        String username,

        @NotBlank(message = "显示名称不能为空")
        @Size(max = 128, message = "显示名称不能超过 128 个字符")
        String displayName,

        @NotBlank(message = "密码不能为空")
        @Size(min = 8, max = 72, message = "密码长度必须为 8-72 位")
        String password
) {
}