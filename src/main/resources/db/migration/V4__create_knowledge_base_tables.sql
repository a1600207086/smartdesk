CREATE TABLE knowledge_document (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    title VARCHAR(255) NOT NULL,
    source_type VARCHAR(32) NOT NULL,
    source_uri VARCHAR(1000) NULL,
    status VARCHAR(32) NOT NULL,
    checksum VARCHAR(64) NOT NULL,
    created_by BIGINT NOT NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_knowledge_document_tenant_checksum UNIQUE (tenant_id, checksum),
    CONSTRAINT fk_knowledge_document_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_knowledge_document_creator FOREIGN KEY (created_by) REFERENCES app_user (id)
);

CREATE INDEX idx_knowledge_document_tenant_created
    ON knowledge_document (tenant_id, created_at);

CREATE TABLE knowledge_chunk (
    id BIGINT NOT NULL AUTO_INCREMENT,
    document_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    embedding_json TEXT NOT NULL,
    token_count INT NULL,
    created_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_knowledge_chunk_document_index UNIQUE (document_id, chunk_index),
    CONSTRAINT fk_knowledge_chunk_document FOREIGN KEY (document_id) REFERENCES knowledge_document (id),
    CONSTRAINT fk_knowledge_chunk_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id)
);

CREATE INDEX idx_knowledge_chunk_tenant_document
    ON knowledge_chunk (tenant_id, document_id);