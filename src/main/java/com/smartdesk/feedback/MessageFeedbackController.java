package com.smartdesk.feedback;

import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.common.api.ApiResponse;
import com.smartdesk.common.security.AuthenticatedUserSupport;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/conversations/{conversationId}/messages/{messageId}/feedback")
public class MessageFeedbackController {

    private final MessageFeedbackService feedbackService;

    public MessageFeedbackController(MessageFeedbackService feedbackService) {
        this.feedbackService = feedbackService;
    }

    @PutMapping
    public ApiResponse<MessageFeedbackResponse> upsert(
            Authentication authentication,
            @PathVariable Long conversationId,
            @PathVariable Long messageId,
            @Valid @RequestBody UpsertMessageFeedbackRequest request
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(
                feedbackService.upsert(conversationId, messageId, user, request)
        );
    }

    @GetMapping
    public ApiResponse<MessageFeedbackResponse> find(
            Authentication authentication,
            @PathVariable Long conversationId,
            @PathVariable Long messageId
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(feedbackService.find(conversationId, messageId, user));
    }

    @DeleteMapping
    public ApiResponse<Void> delete(
            Authentication authentication,
            @PathVariable Long conversationId,
            @PathVariable Long messageId
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        feedbackService.delete(conversationId, messageId, user);
        return ApiResponse.success(null);
    }
}
