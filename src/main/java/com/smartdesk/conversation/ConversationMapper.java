package com.smartdesk.conversation;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface ConversationMapper {

    int insert(ConversationEntity conversation);

    ConversationEntity findById(@Param("id") Long id);

    List<ConversationEntity> findAllByTenantIdAndUserId(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId
    );

    int touchUpdatedAt(
            @Param("id") Long id,
            @Param("updatedAt") LocalDateTime updatedAt
    );
}