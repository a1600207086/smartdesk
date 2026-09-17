package com.smartdesk.tenant;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank(message = "租户编码不能为空")
        @Pattern(
                regexp = "^[A-Za-z][A-Za-z0-9_-]{2,63}$",
                message = "租户编码需为 3-64 位字母、数字、下划线或短横线，且以字母开头"
        )
        String code,

        @NotBlank(message = "租户名称不能为空")
        @Size(max = 128, message = "租户名称不能超过 128 个字符")
        String name
) {
}
