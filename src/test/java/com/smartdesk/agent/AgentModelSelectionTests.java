package com.smartdesk.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AgentModelSelectionTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(ObjectMapper.class, ObjectMapper::new)
            .withUserConfiguration(MockAgentChatModel.class);

    @Test
    void shouldRegisterMockWhenLlmIsDisabled() {
        contextRunner
                .withPropertyValues("smartdesk.llm.enabled=false")
                .run(context -> assertThat(context).hasSingleBean(MockAgentChatModel.class));
    }

    @Test
    void shouldNotRegisterMockWhenLlmIsEnabled() {
        contextRunner
                .withPropertyValues("smartdesk.llm.enabled=true")
                .run(context -> assertThat(context).doesNotHaveBean(MockAgentChatModel.class));
    }
}
