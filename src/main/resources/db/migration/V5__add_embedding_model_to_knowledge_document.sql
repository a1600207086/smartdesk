ALTER TABLE knowledge_document
    ADD COLUMN embedding_model VARCHAR(128) NOT NULL DEFAULT 'hash-v1-256';