package com.smartdesk.knowledge;

import com.smartdesk.auth.AuthenticatedUser;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class KnowledgeAsyncDocumentService {

    private final KnowledgeDocumentService documentService;
    private final KnowledgeAsyncProcessor asyncProcessor;

    public KnowledgeAsyncDocumentService(
            KnowledgeDocumentService documentService,
            KnowledgeAsyncProcessor asyncProcessor
    ) {
        this.documentService = documentService;
        this.asyncProcessor = asyncProcessor;
    }

    public KnowledgeDocumentResponse createTextDocument(
            AuthenticatedUser user,
            CreateTextDocumentRequest request
    ) {
        KnowledgeDocumentResponse response = documentService.createPendingTextDocument(user, request);
        asyncProcessor.submit(response.id(), user.tenantId());
        return response;
    }

    public KnowledgeDocumentResponse createFileDocument(
            AuthenticatedUser user,
            String title,
            String sourceUri,
            MultipartFile file
    ) {
        KnowledgeDocumentResponse response = documentService.createPendingFileDocument(
                user,
                title,
                sourceUri,
                file
        );
        asyncProcessor.submit(response.id(), user.tenantId());
        return response;
    }

    public KnowledgeDocumentResponse retry(Long documentId, AuthenticatedUser user) {
        KnowledgeDocumentResponse response = documentService.prepareRetry(documentId, user);
        asyncProcessor.submit(documentId, user.tenantId());
        return response;
    }
}