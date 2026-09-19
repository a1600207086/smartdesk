package com.smartdesk.conversation;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface MessageMapper {

    int insert(MessageEntity message);

    MessageEntity findByIdAndConversationId(
            @Param("id") Long id,
            @Param("conversationId") Long conversationId
    );

    List<MessageEntity> findPage(
            @Param("conversationId") Long conversationId,
            @Param("beforeId") Long beforeId,
            @Param("limit") int limit
    );
}
