CREATE TABLE message_feedback (
    id BIGINT NOT NULL AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    conversation_id BIGINT NOT NULL,
    message_id BIGINT NOT NULL,
    rating VARCHAR(32) NOT NULL,
    comment VARCHAR(500) NULL,
    created_at DATETIME(6) NOT NULL,
    updated_at DATETIME(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_message_feedback_user_message UNIQUE (user_id, message_id),
    CONSTRAINT fk_message_feedback_tenant FOREIGN KEY (tenant_id) REFERENCES tenant (id),
    CONSTRAINT fk_message_feedback_user FOREIGN KEY (user_id) REFERENCES app_user (id),
    CONSTRAINT fk_message_feedback_conversation FOREIGN KEY (conversation_id) REFERENCES conversation (id),
    CONSTRAINT fk_message_feedback_message FOREIGN KEY (message_id) REFERENCES message (id) ON DELETE CASCADE
);

CREATE INDEX idx_message_feedback_tenant_rating_updated
    ON message_feedback (tenant_id, rating, updated_at);
