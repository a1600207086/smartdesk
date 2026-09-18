package com.smartdesk;

import com.smartdesk.agent.AgentDecision;
import com.smartdesk.agent.AgentRoute;
import com.smartdesk.agent.AgentRouter;
import com.smartdesk.conversation.MessageResponse;
import com.smartdesk.conversation.MessageRole;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AgentRouterTests {

    private final AgentRouter router = new AgentRouter();

    @Test
    void shouldRoutePolicyQuestionToKnowledgeSearch() {
        MessageResponse message = new MessageResponse(
                2L,
                1L,
                MessageRole.USER,
                "退款政策是什么？",
                null,
                LocalDateTime.now()
        );

        AgentDecision decision = router.route(List.of(message));

        assertThat(decision.route()).isEqualTo(AgentRoute.TOOL_CALL);
        assertThat(decision.toolName()).isEqualTo("searchKnowledge");
        assertThat(decision.arguments()).containsEntry("query", "退款政策是什么？");
    }
    @Test
    void shouldDetectOrderNumberEvenWhenChineseTextIsCorrupted() {
        MessageResponse message = new MessageResponse(
                1L,
                1L,
                MessageRole.USER,
                "??? A10001 ??????",
                null,
                LocalDateTime.now()
        );

        AgentDecision decision = router.route(List.of(message));

        assertThat(decision.route()).isEqualTo(AgentRoute.TOOL_CALL);
        assertThat(decision.toolName()).isEqualTo("queryOrder");
        assertThat(decision.arguments()).containsEntry("orderNo", "A10001");
    }
}
