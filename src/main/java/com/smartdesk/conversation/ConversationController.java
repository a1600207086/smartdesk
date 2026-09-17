package com.smartdesk.conversation;

import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.common.api.ApiResponse;
import com.smartdesk.common.security.AuthenticatedUserSupport;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<ConversationResponse> create(
            Authentication authentication,
            @Valid @RequestBody CreateConversationRequest request
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(conversationService.create(user, request));
    }

    @GetMapping
    public ApiResponse<List<ConversationResponse>> findAll(Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(conversationService.findAll(user));
    }

    @GetMapping("/{conversationId}")
    public ApiResponse<ConversationResponse> findById(
            Authentication authentication,
            @PathVariable Long conversationId
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(conversationService.findById(conversationId, user));
    }
}