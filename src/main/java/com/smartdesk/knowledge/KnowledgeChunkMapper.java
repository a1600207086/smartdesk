package com.smartdesk.knowledge;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface KnowledgeChunkMapper {

    int insertBatch(@Param("chunks") List<KnowledgeChunkEntity> chunks);

    int countByDocumentId(@Param("documentId") Long documentId);

    int deleteByDocumentId(@Param("documentId") Long documentId);

    List<KnowledgeChunkEntity> findAllByTenantId(@Param("tenantId") Long tenantId);
}
