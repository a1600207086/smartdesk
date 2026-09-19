package com.smartdesk.agent;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface AgentRunMapper {

    int insert(AgentRunEntity run);

    int updateRoute(@Param("id") Long id, @Param("route") AgentRoute route);

    int complete(@Param("id") Long id, @Param("finishedAt") LocalDateTime finishedAt);

    int fail(
            @Param("id") Long id,
            @Param("errorMessage") String errorMessage,
            @Param("finishedAt") LocalDateTime finishedAt
    );

    List<AgentRunEntity> findAllByConversationId(
            @Param("conversationId") Long conversationId,
            @Param("limit") int limit
    );

    AgentRunEntity findByIdAndConversationId(
            @Param("id") Long id,
            @Param("conversationId") Long conversationId
    );

    AgentMetricsRow summarizeByTenantId(@Param("tenantId") Long tenantId);
}
