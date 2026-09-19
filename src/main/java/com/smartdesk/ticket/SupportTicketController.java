package com.smartdesk.ticket;

import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.common.api.ApiResponse;
import com.smartdesk.common.security.AuthenticatedUserSupport;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tickets")
public class SupportTicketController {

    private final SupportTicketService ticketService;

    public SupportTicketController(SupportTicketService ticketService) {
        this.ticketService = ticketService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SupportTicketResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateSupportTicketRequest request
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(ticketService.create(user, request));
    }

    @GetMapping
    public ApiResponse<List<SupportTicketResponse>> findAll(Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(ticketService.findAll(user));
    }

    @GetMapping("/metrics")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ApiResponse<TicketMetricsResponse> metrics(Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(ticketService.summarize(user));
    }

    @GetMapping("/{ticketId}")
    public ApiResponse<SupportTicketResponse> findById(
            Authentication authentication,
            @PathVariable Long ticketId
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(ticketService.findById(ticketId, user));
    }

    @PatchMapping("/{ticketId}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT')")
    public ApiResponse<SupportTicketResponse> updateStatus(
            Authentication authentication,
            @PathVariable Long ticketId,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(ticketService.updateStatus(ticketId, request.status(), user));
    }
}
