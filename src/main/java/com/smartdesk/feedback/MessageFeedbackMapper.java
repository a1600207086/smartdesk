package com.smartdesk.feedback;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface MessageFeedbackMapper {

    int upsert(MessageFeedbackEntity feedback);

    MessageFeedbackEntity findByTenantIdAndUserIdAndMessageId(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("messageId") Long messageId
    );

    int deleteByTenantIdAndUserIdAndMessageId(
            @Param("tenantId") Long tenantId,
            @Param("userId") Long userId,
            @Param("messageId") Long messageId
    );

    FeedbackSummaryRow summarizeByTenantId(@Param("tenantId") Long tenantId);
}
