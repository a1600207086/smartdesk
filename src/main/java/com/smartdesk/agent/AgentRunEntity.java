package com.smartdesk.agent;

import java.time.LocalDateTime;

public class AgentRunEntity {

    private Long id;
    private Long conversationId;
    private Long tenantId;
    private Long userId;
    private Long inputMessageId;
    private AgentRoute route;
    private AgentRunStatus status;
    private String errorMessage;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getConversationId() { return conversationId; }
    public void setConversationId(Long conversationId) { this.conversationId = conversationId; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getInputMessageId() { return inputMessageId; }
    public void setInputMessageId(Long inputMessageId) { this.inputMessageId = inputMessageId; }
    public AgentRoute getRoute() { return route; }
    public void setRoute(AgentRoute route) { this.route = route; }
    public AgentRunStatus getStatus() { return status; }
    public void setStatus(AgentRunStatus status) { this.status = status; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
    public LocalDateTime getStartedAt() { return startedAt; }
    public void setStartedAt(LocalDateTime startedAt) { this.startedAt = startedAt; }
    public LocalDateTime getFinishedAt() { return finishedAt; }
    public void setFinishedAt(LocalDateTime finishedAt) { this.finishedAt = finishedAt; }
}