package com.smartdesk.knowledge;

import com.smartdesk.common.error.NotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.core.task.TaskExecutor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class KnowledgeAsyncProcessor {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeAsyncProcessor.class);

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final KnowledgeChunkFactory chunkFactory;
    private final KnowledgeSearchCache searchCache;
    private final TaskExecutor taskExecutor;
    private final TransactionTemplate transactionTemplate;

    public KnowledgeAsyncProcessor(
            KnowledgeDocumentMapper documentMapper,
            KnowledgeChunkMapper chunkMapper,
            KnowledgeChunkFactory chunkFactory,
            KnowledgeSearchCache searchCache,
            @Qualifier("knowledgeTaskExecutor") TaskExecutor taskExecutor,
            PlatformTransactionManager transactionManager
    ) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.chunkFactory = chunkFactory;
        this.searchCache = searchCache;
        this.taskExecutor = taskExecutor;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    public void submit(Long documentId, Long tenantId) {
        try {
            taskExecutor.execute(() -> process(documentId, tenantId));
        } catch (RuntimeException exception) {
            markFailed(documentId, "文档处理任务提交失败: " + exception.getMessage());
            throw exception;
        }
    }

    private void process(Long documentId, Long tenantId) {
        try {
            KnowledgeDocumentEntity document = transactionTemplate.execute(status -> {
                KnowledgeDocumentEntity current = documentMapper.findById(documentId);
                if (current == null || !current.getTenantId().equals(tenantId)) {
                    throw new NotFoundException("知识文档不存在: " + documentId);
                }
                if (current.getContent() == null || current.getContent().isBlank()) {
                    throw new IllegalArgumentException("文档没有原始内容");
                }

                chunkMapper.deleteByDocumentId(documentId);
                documentMapper.resetForProcessing(
                        documentId,
                        chunkFactory.modelId(),
                        LocalDateTime.now()
                );
                return current;
            });

            if (document == null) {
                throw new IllegalStateException("Unable to load document for processing");
            }

            List<KnowledgeChunkEntity> chunks = chunkFactory.createChunks(
                    document.getId(),
                    tenantId,
                    document.getContent(),
                    LocalDateTime.now()
            );

            transactionTemplate.executeWithoutResult(status -> {
                chunkMapper.insertBatch(chunks);
                documentMapper.updateForReindex(
                        documentId,
                        DocumentStatus.READY,
                        chunkFactory.modelId(),
                        LocalDateTime.now()
                );
            });
            searchCache.invalidate(tenantId);
        } catch (Exception exception) {
            log.error("Async knowledge document processing failed: {}", documentId, exception);
            markFailed(documentId, exception.getMessage());
        }
    }

    private void markFailed(Long documentId, String message) {
        String safeMessage = message == null ? "Unknown document processing error" : message;
        if (safeMessage.length() > 1000) {
            safeMessage = safeMessage.substring(0, 1000);
        }
        String finalMessage = safeMessage;
        transactionTemplate.executeWithoutResult(status ->
                documentMapper.markFailed(documentId, finalMessage, LocalDateTime.now())
        );
    }
}