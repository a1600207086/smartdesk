package com.smartdesk.ticket;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SupportTicketMapper {

    int insert(SupportTicketEntity ticket);

    SupportTicketEntity findActiveByConversationId(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("conversationId") Long conversationId
    );

    SupportTicketEntity findByIdAndTenantId(
            @Param("id") Long id,
            @Param("tenantId") Long tenantId
    );

    List<SupportTicketEntity> findAllByTenantId(@Param("tenantId") Long tenantId);

    List<SupportTicketEntity> findAllByTenantIdAndUserId(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId
    );

    int updateStatus(
            @Param("id") Long id,
            @Param("tenantId") Long tenantId,
            @Param("status") TicketStatus status,
            @Param("updatedAt") LocalDateTime updatedAt,
            @Param("resolvedAt") LocalDateTime resolvedAt
    );

    TicketMetricsRow summarizeByTenantId(@Param("tenantId") Long tenantId);
}
