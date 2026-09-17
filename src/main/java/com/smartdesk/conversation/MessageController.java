package com.smartdesk.conversation;

import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.common.api.ApiResponse;
import com.smartdesk.common.security.AuthenticatedUserSupport;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/messages")
public class MessageController {

    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MessageResponse> appendUserMessage(
            Authentication authentication,
            @PathVariable Long conversationId,
            @Valid @RequestBody CreateUserMessageRequest request
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(
                messageService.appendUserMessage(conversationId, user, request)
        );
    }

    @GetMapping
    public ApiResponse<MessagePageResponse> findMessages(
            Authentication authentication,
            @PathVariable Long conversationId,
            @RequestParam(required = false)
            @Min(value = 1, message = "limit 必须大于 0")
            @Max(value = 100, message = "limit 不能超过 100")
            Integer limit,
            @RequestParam(required = false) Long beforeId
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(
                messageService.findMessages(conversationId, user, limit, beforeId)
        );
    }
}