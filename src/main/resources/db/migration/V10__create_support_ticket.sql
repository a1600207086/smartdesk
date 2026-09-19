CREATE TABLE support_ticket (
    id BIGINT NOT NULL AUTO_INCREMENT,
    ticket_no VARCHAR(64) NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT NULL,
    subject VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    priority VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    resolved_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_support_ticket_no UNIQUE (ticket_no),
    CONSTRAINT fk_support_ticket_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_support_ticket_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_support_ticket_conversation FOREIGN KEY (conversation_id) REFERENCES conversation (id)
);

CREATE INDEX idx_support_ticket_tenant_status_updated
    ON support_ticket (tenant_id, status, updated_at);

CREATE INDEX idx_support_ticket_user_updated
    ON support_ticket (user_id, updated_at);
