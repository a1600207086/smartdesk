package com.smartdesk.tenant;

import java.time.LocalDateTime;

public record TenantResponse(
        Long id,
        String code,
        String name,
        TenantStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static TenantResponse from(TenantEntity tenant) {
        return new TenantResponse(
                tenant.getId(),
                tenant.getCode(),
                tenant.getName(),
                tenant.getStatus(),
                tenant.getCreatedAt(),
                tenant.getUpdatedAt()
        );
    }
}
