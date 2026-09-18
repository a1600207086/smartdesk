package com.smartdesk.knowledge;

import java.time.LocalDateTime;

public class KnowledgeDocumentEntity {

    private Long id;
    private Long tenantId;
    private String title;
    private String content;
    private DocumentSourceType sourceType;
    private String sourceUri;
    private DocumentStatus status;
    private String checksum;
    private String errorMessage;
    private String embeddingModel;
    private Long createdBy;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getTitle() { return title; }
    public String getContent() { return content; }
    public void setTitle(String title) { this.title = title; }
    public void setContent(String content) { this.content = content; }
    public DocumentSourceType getSourceType() { return sourceType; }
    public void setSourceType(DocumentSourceType sourceType) { this.sourceType = sourceType; }
    public String getSourceUri() { return sourceUri; }
    public void setSourceUri(String sourceUri) { this.sourceUri = sourceUri; }
    public DocumentStatus getStatus() { return status; }
    public void setStatus(DocumentStatus status) { this.status = status; }
    public String getChecksum() { return checksum; }
    public String getErrorMessage() { return errorMessage; }
    public String getEmbeddingModel() { return embeddingModel; }
    public void setChecksum(String checksum) { this.checksum = checksum; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public void setEmbeddingModel(String embeddingModel) { this.embeddingModel = embeddingModel; }
    public Long getCreatedBy() { return createdBy; }
    public void setCreatedBy(Long createdBy) { this.createdBy = createdBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
