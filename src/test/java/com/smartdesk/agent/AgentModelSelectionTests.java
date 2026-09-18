package com.smartdesk.agent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class AgentModelSelectionTests {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
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