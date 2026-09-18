package com.smartdesk.knowledge;

import com.smartdesk.auth.AuthenticatedUser;
import com.smartdesk.common.error.ConflictException;
import com.smartdesk.common.error.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

@Service
public class KnowledgeDocumentService {

    private final KnowledgeDocumentMapper documentMapper;
    private final KnowledgeChunkMapper chunkMapper;
    private final TextChunker textChunker;
    private final EmbeddingModel embeddingModel;
    private final EmbeddingVectorCodec vectorCodec;
    private final KnowledgeSearchCache searchCache;

    public KnowledgeDocumentService(
            KnowledgeDocumentMapper documentMapper,
            KnowledgeChunkMapper chunkMapper,
            TextChunker textChunker,
            EmbeddingModel embeddingModel,
            EmbeddingVectorCodec vectorCodec,
            KnowledgeSearchCache searchCache
    ) {
        this.documentMapper = documentMapper;
        this.chunkMapper = chunkMapper;
        this.textChunker = textChunker;
        this.embeddingModel = embeddingModel;
        this.vectorCodec = vectorCodec;
        this.searchCache = searchCache;
    }

    @Transactional
    public KnowledgeDocumentResponse createTextDocument(
            AuthenticatedUser user,
            CreateTextDocumentRequest request
    ) {
        return createDocument(
                user,
                request.title(),
                DocumentSourceType.TEXT,
                request.sourceUri(),
                request.content()
        );
    }

    @Transactional
    public KnowledgeDocumentResponse createFileDocument(
            AuthenticatedUser user,
            String title,
            String sourceUri,
            MultipartFile file
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("上传文件不能为空");
        }

        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String lowerName = filename.toLowerCase();
        boolean textFile = lowerName.endsWith(".txt")
                || lowerName.endsWith(".md")
                || lowerName.endsWith(".markdown");
        boolean textContentType = file.getContentType() != null
                && file.getContentType().startsWith("text/");
        if (!textFile && !textContentType) {
            throw new IllegalArgumentException("当前版本仅支持 txt 和 markdown 文件");
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            return createDocument(user, title, DocumentSourceType.FILE, sourceUri, content);
        } catch (IOException exception) {
            throw new IllegalArgumentException("无法读取上传文件", exception);
        }
    }

    @Transactional(readOnly = true)
    public List<KnowledgeDocumentResponse> findAll(AuthenticatedUser user) {
        return documentMapper.findAllByTenantId(user.tenantId())
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public KnowledgeDocumentResponse findById(Long documentId, AuthenticatedUser user) {
        return toResponse(requireOwnedDocument(documentId, user));
    }

    @Transactional
    public void deleteDocument(Long documentId, AuthenticatedUser user) {
        requireOwnedDocument(documentId, user);
        chunkMapper.deleteByDocumentId(documentId);
        documentMapper.deleteById(documentId);
        searchCache.invalidate(user.tenantId());
    }

    @Transactional
    public KnowledgeDocumentResponse reindexDocument(Long documentId, AuthenticatedUser user) {
        KnowledgeDocumentEntity document = requireOwnedDocument(documentId, user);
        if (document.getContent() == null || document.getContent().isBlank()) {
            throw new ConflictException("旧文档没有保存原始内容，请删除后重新上传");
        }

        LocalDateTime now = LocalDateTime.now();
        documentMapper.updateStatus(documentId, DocumentStatus.PROCESSING);
        chunkMapper.deleteByDocumentId(documentId);

        List<KnowledgeChunkEntity> chunks = createChunks(
                documentId,
                user.tenantId(),
                document.getContent(),
                now
        );
        chunkMapper.insertBatch(chunks);
        documentMapper.updateForReindex(
                documentId,
                DocumentStatus.READY,
                embeddingModel.modelId(),
                now
        );
        searchCache.invalidate(user.tenantId());

        document.setStatus(DocumentStatus.READY);
        document.setEmbeddingModel(embeddingModel.modelId());
        document.setUpdatedAt(now);
        return toResponse(document, chunks.size());
    }

    private KnowledgeDocumentResponse createDocument(
            AuthenticatedUser user,
            String title,
            DocumentSourceType sourceType,
            String sourceUri,
            String content
    ) {
        String normalizedTitle = title == null ? "" : title.trim();
        String normalizedContent = normalizeContent(content);
        if (normalizedTitle.isEmpty()) {
            throw new IllegalArgumentException("文档标题不能为空");
        }
        if (normalizedContent.isEmpty()) {
            throw new IllegalArgumentException("文档内容不能为空");
        }

        String checksum = sha256(normalizedContent);
        if (documentMapper.countByTenantIdAndChecksum(user.tenantId(), checksum) > 0) {
            throw new ConflictException("相同内容的文档已经存在");
        }

        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocumentEntity document = new KnowledgeDocumentEntity();
        document.setTenantId(user.tenantId());
        document.setTitle(normalizedTitle);
        document.setContent(normalizedContent);
        document.setSourceType(sourceType);
        document.setSourceUri(sourceUri);
        document.setStatus(DocumentStatus.PROCESSING);
        document.setChecksum(checksum);
        document.setEmbeddingModel(embeddingModel.modelId());
        document.setCreatedBy(user.userId());
        document.setCreatedAt(now);
        document.setUpdatedAt(now);
        documentMapper.insert(document);

        List<KnowledgeChunkEntity> entities = createChunks(
                document.getId(),
                user.tenantId(),
                normalizedContent,
                now
        );
        chunkMapper.insertBatch(entities);
        documentMapper.updateStatus(document.getId(), DocumentStatus.READY);
        searchCache.invalidate(user.tenantId());

        document.setStatus(DocumentStatus.READY);
        return toResponse(document, entities.size());
    }

    private List<KnowledgeChunkEntity> createChunks(
            Long documentId,
            Long tenantId,
            String content,
            LocalDateTime createdAt
    ) {
        List<String> chunks = textChunker.split(content);
        if (chunks.isEmpty()) {
            throw new IllegalArgumentException("文档内容无法生成有效切片");
        }

        List<float[]> vectors = embeddingModel.embedAll(chunks);
        if (vectors.size() != chunks.size()) {
            throw new IllegalStateException("Embedding count does not match chunk count");
        }

        List<KnowledgeChunkEntity> entities = new ArrayList<>(chunks.size());
        for (int index = 0; index < chunks.size(); index++) {
            String chunk = chunks.get(index);
            KnowledgeChunkEntity entity = new KnowledgeChunkEntity();
            entity.setDocumentId(documentId);
            entity.setTenantId(tenantId);
            entity.setChunkIndex(index);
            entity.setContent(chunk);
            entity.setEmbeddingJson(vectorCodec.encode(vectors.get(index)));
            entity.setTokenCount(Math.max(1, chunk.length() / 4));
            entity.setCreatedAt(createdAt);
            entities.add(entity);
        }
        return entities;
    }

    private KnowledgeDocumentEntity requireOwnedDocument(
            Long documentId,
            AuthenticatedUser user
    ) {
        KnowledgeDocumentEntity document = documentMapper.findById(documentId);
        if (document == null || !document.getTenantId().equals(user.tenantId())) {
            throw new NotFoundException("知识文档不存在: " + documentId);
        }
        return document;
    }

    private KnowledgeDocumentResponse toResponse(KnowledgeDocumentEntity document) {
        return toResponse(document, chunkMapper.countByDocumentId(document.getId()));
    }

    private KnowledgeDocumentResponse toResponse(
            KnowledgeDocumentEntity document,
            int chunkCount
    ) {
        return new KnowledgeDocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getSourceType(),
                document.getSourceUri(),
                document.getStatus(),
                document.getEmbeddingModel(),
                chunkCount,
                document.getCreatedAt()
        );
    }

    private String normalizeContent(String content) {
        if (content == null) {
            return "";
        }
        return content.replace("\r\n", "\n").replace('\r', '\n').trim();
    }

    private String sha256(String content) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("Unable to calculate document checksum", exception);
        }
    }
}