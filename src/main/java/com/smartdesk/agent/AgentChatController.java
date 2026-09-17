package com.smartdesk.agent;

import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.common.security.AuthenticatedUserSupport;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}")
public class AgentChatController {

    private static final Logger log = LoggerFactory.getLogger(AgentChatController.class);

    private final AgentOrchestrator agentOrchestrator;
    private final TaskExecutor agentTaskExecutor;

    public AgentChatController(
            AgentOrchestrator agentOrchestrator,
            @Qualifier("agentTaskExecutor") TaskExecutor agentTaskExecutor
    ) {
        this.agentOrchestrator = agentOrchestrator;
        this.agentTaskExecutor = agentTaskExecutor;
    }

    @PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE + ";charset=UTF-8")
    public SseEmitter chat(
            Authentication authentication,
            @PathVariable Long conversationId,
            @Valid @RequestBody AgentChatRequest request
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        SseEmitter emitter = new SseEmitter(60_000L);
        emitter.onTimeout(emitter::complete);
        emitter.onError(exception ->
                log.debug("Agent SSE connection closed", exception));

        agentTaskExecutor.execute(() ->
                agentOrchestrator.run(
                        conversationId,
                        user,
                        request.message().trim(),
                        emitter
                ));
        return emitter;
    }
}
