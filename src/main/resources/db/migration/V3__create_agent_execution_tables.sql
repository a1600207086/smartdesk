CREATE TABLE agent_run (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    input_message_id BIGINT NOT NULL,
    route VARCHAR(32) NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(1000) NULL,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_agent_run_conversation FOREIGN KEY (conversation_id) REFERENCES conversation (id),
    CONSTRAINT fk_agent_run_input_message FOREIGN KEY (input_message_id) REFERENCES message (id)
);

CREATE INDEX idx_agent_run_conversation_started
    ON agent_run (conversation_id, started_at);

CREATE TABLE tool_call_log (
    id BIGINT NOT NULL AUTO_INCREMENT,
    agent_run_id BIGINT NOT NULL,
    tool_name VARCHAR(128) NOT NULL,
    arguments_json TEXT NULL,
    result_json TEXT NULL,
    success BOOLEAN NOT NULL,
    error_message VARCHAR(1000) NULL,
    duration_ms BIGINT NULL,
    started_at DATETIME(6) NOT NULL,
    finished_at DATETIME(6) NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_tool_call_log_agent_run FOREIGN KEY (agent_run_id) REFERENCES agent_run (id)
);

CREATE INDEX idx_tool_call_log_agent_run_started
    ON tool_call_log (agent_run_id, started_at);