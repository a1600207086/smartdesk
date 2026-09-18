package com.smartdesk.knowledge;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeDocumentMapper {

    int insert(KnowledgeDocumentEntity document);

    KnowledgeDocumentEntity findById(@Param("id") Long id);

    List<KnowledgeDocumentEntity> findAllByTenantId(@Param("tenantId") Long tenantId);

    int updateStatus(@Param("id") Long id, @Param("status") DocumentStatus status);

    int resetForProcessing(
            @Param("id") Long id,
            @Param("embeddingModel") String embeddingModel,
            @Param("updatedAt") java.time.LocalDateTime updatedAt
    );

    int markFailed(
            @Param("id") Long id,
            @Param("errorMessage") String errorMessage,
            @Param("updatedAt") java.time.LocalDateTime updatedAt
    );

    int updateForReindex(
            @Param("id") Long id,
            @Param("status") DocumentStatus status,
            @Param("embeddingModel") String embeddingModel,
            @Param("updatedAt") java.time.LocalDateTime updatedAt
    );

    int deleteById(@Param("id") Long id);

    int countByTenantIdAndChecksum(
            @Param("tenantId") Long tenantId,
            @Param("checksum") String checksum
    );
}
