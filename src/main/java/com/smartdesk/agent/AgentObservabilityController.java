package com.smartdesk.agent;

import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.common.api.ApiResponse;
import com.smartdesk.common.security.AuthenticatedUserSupport;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1")
@Validated
public class AgentObservabilityController {

    private final AgentObservabilityService observabilityService;

    public AgentObservabilityController(AgentObservabilityService observabilityService) {
        this.observabilityService = observabilityService;
    }

    @GetMapping("/conversations/{conversationId}/agent-runs")
    public ApiResponse<List<AgentRunResponse>> findRuns(
            Authentication authentication,
            @PathVariable Long conversationId,
            @RequestParam(required = false)
            @Min(value = 1, message = "limit 必须大于 0")
            @Max(value = 100, message = "limit 不能超过 100")
            Integer limit
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(observabilityService.findRuns(conversationId, limit, user));
    }

    @GetMapping("/conversations/{conversationId}/agent-runs/{runId}")
    public ApiResponse<AgentRunResponse> findRun(
            Authentication authentication,
            @PathVariable Long conversationId,
            @PathVariable Long runId
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(observabilityService.findRun(conversationId, runId, user));
    }

    @GetMapping("/agent/metrics")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AgentMetricsResponse> summarize(Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(observabilityService.summarize(user));
    }
}
