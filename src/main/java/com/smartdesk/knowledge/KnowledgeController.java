package com.smartdesk.knowledge;

import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.common.api.ApiResponse;
import com.smartdesk.common.security.AuthenticatedUserSupport;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/knowledge")
public class KnowledgeController {

    private final KnowledgeDocumentService documentService;
    private final KnowledgeRetrievalService retrievalService;
    private final KnowledgeAsyncDocumentService asyncDocumentService;

    public KnowledgeController(
            KnowledgeDocumentService documentService,
            KnowledgeRetrievalService retrievalService,
            KnowledgeAsyncDocumentService asyncDocumentService
    ) {
        this.documentService = documentService;
        this.retrievalService = retrievalService;
        this.asyncDocumentService = asyncDocumentService;
    }

    @PostMapping("/documents/text")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<KnowledgeDocumentResponse> createTextDocument(
            Authentication authentication,
            @Valid @RequestBody CreateTextDocumentRequest request
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(documentService.createTextDocument(user, request));
    }

    @PostMapping("/documents/text/async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<KnowledgeDocumentResponse> createTextDocumentAsync(
            Authentication authentication,
            @Valid @RequestBody CreateTextDocumentRequest request
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(asyncDocumentService.createTextDocument(user, request));
    }

    @PostMapping("/documents/upload/async")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<KnowledgeDocumentResponse> uploadDocumentAsync(
            Authentication authentication,
            @RequestParam String title,
            @RequestParam(required = false) String sourceUri,
            @RequestPart("file") MultipartFile file
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(asyncDocumentService.createFileDocument(user, title, sourceUri, file));
    }

    @PostMapping("/documents/{documentId}/retry")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<KnowledgeDocumentResponse> retryDocument(
            Authentication authentication,
            @PathVariable Long documentId
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(asyncDocumentService.retry(documentId, user));
    }

    @PostMapping("/documents/upload")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<KnowledgeDocumentResponse> uploadDocument(
            Authentication authentication,
            @RequestParam String title,
            @RequestParam(required = false) String sourceUri,
            @RequestPart("file") MultipartFile file
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(documentService.createFileDocument(user, title, sourceUri, file));
    }

    @GetMapping("/documents/{documentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<KnowledgeDocumentResponse> findDocument(
            Authentication authentication,
            @PathVariable Long documentId
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(documentService.findById(documentId, user));
    }

    @DeleteMapping("/documents/{documentId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteDocument(
            Authentication authentication,
            @PathVariable Long documentId
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        documentService.deleteDocument(documentId, user);
        return ApiResponse.success(null);
    }

    @PostMapping("/documents/{documentId}/reindex")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<KnowledgeDocumentResponse> reindexDocument(
            Authentication authentication,
            @PathVariable Long documentId
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(documentService.reindexDocument(documentId, user));
    }

    @GetMapping("/documents")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<KnowledgeDocumentResponse>> listDocuments(Authentication authentication) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(documentService.findAll(user));
    }

    @PostMapping("/search")
    public ApiResponse<List<KnowledgeSearchResult>> search(
            Authentication authentication,
            @Valid @RequestBody KnowledgeSearchRequest request
    ) {
        AuthenticatedUser user = AuthenticatedUserSupport.require(authentication);
        return ApiResponse.success(
                retrievalService.search(user.tenantId(), request.query(), request.topK())
        );
    }
}
