package com.smartdesk.conversation;

import com.smartdesk.knowledge.KnowledgeCitation;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface MessageCitationMapper {

    int insertBatch(
            @Param("messageId") Long messageId,
            @Param("tenantId") Long tenantId,
            @Param("citations") List<KnowledgeCitation> citations,
            @Param("createdAt") LocalDateTime createdAt
    );

    List<MessageCitationEntity> findByMessageIds(
            @Param("tenantId") Long tenantId,
            @Param("messageIds") List<Long> messageIds
    );
}
