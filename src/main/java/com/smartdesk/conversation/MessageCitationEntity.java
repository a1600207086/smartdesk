package com.smartdesk.conversation;

import com.smartdesk.knowledge.KnowledgeCitation;

import java.time.LocalDateTime;

public class MessageCitationEntity {

    private Long id;
    private Long messageId;
    private Long tenantId;
    private int citationIndex;
    private Long documentId;
    private String documentTitle;
    private Long chunkId;
    private int chunkIndex;
    private double score;
    private String snippet;
    private LocalDateTime createdAt;

    public KnowledgeCitation toCitation() {
        return new KnowledgeCitation(
                citationIndex,
                documentId,
                documentTitle,
                chunkId,
                chunkIndex,
                score,
                snippet
        );
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMessageId() { return messageId; }
    public void setMessageId(Long messageId) { this.messageId = messageId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public int getCitationIndex() { return citationIndex; }
    public void setCitationIndex(int citationIndex) { this.citationIndex = citationIndex; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }
    public Long getChunkId() { return chunkId; }
    public void setChunkId(Long chunkId) { this.chunkId = chunkId; }
    public int getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(int chunkIndex) { this.chunkIndex = chunkIndex; }
    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }
    public String getSnippet() { return snippet; }
    public void setSnippet(String snippet) { this.snippet = snippet; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
