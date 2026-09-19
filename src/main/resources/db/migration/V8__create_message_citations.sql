CREATE TABLE message_citation (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    citation_order INT NOT NULL,
    document_id BIGINT NOT NULL,
    document_title VARCHAR(255) NOT NULL,
    chunk_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    score DOUBLE NOT NULL,
    snippet VARCHAR(512) NOT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_message_citation_order UNIQUE (message_id, citation_order),
    CONSTRAINT fk_message_citation_message FOREIGN KEY (message_id) REFERENCES message (id) ON DELETE CASCADE,
    CONSTRAINT fk_message_citation_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE INDEX idx_message_citation_tenant_message
    ON message_citation (tenant_id, message_id);
