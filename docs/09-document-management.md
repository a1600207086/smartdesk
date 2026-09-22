# 09 - 知识库文档管理

## 目标

围绕 RAG 流程增加文档运维管理能力：

- view one document
- delete a document
- reindex a document after changing the embedding model
- preserve original content for reindexing

## 原始内容存储

Migration `V6__add_document_content.sql` adds:

```text
knowledge_document.content
```

重建索引需要原始文档，而不是带重叠内容的文本块。新文档始终保存标准化后的原始内容。

V6 之前创建的旧文档可能没有内容字段。在删除并重新上传前，这些文档无法重建索引。

## 删除流程

```text
DELETE /api/v1/knowledge/documents/{id}
  -> verify tenant ownership
  -> delete knowledge_chunk rows
  -> delete knowledge_document row
  -> increment Redis knowledge cache version
```

缓存版本失效机制可以防止继续使用旧搜索结果。

## 重建索引流程

```text
POST /api/v1/knowledge/documents/{id}/reindex
  -> verify tenant ownership and original content
  -> mark document PROCESSING
  -> delete old chunks
  -> chunk original content again
  -> embed with the currently selected EmbeddingModel
  -> insert new chunks
  -> mark READY and record embedding model id
  -> increment Redis cache version
```

这是第一版重建索引流程。生产系统应异步执行重建索引，并支持进度、重试和索引原子切换。

## API 接口

```text
GET    /api/v1/knowledge/documents/{id}
GET    /api/v1/knowledge/documents
POST   /api/v1/knowledge/documents/text
POST   /api/v1/knowledge/documents/upload
POST   /api/v1/knowledge/documents/{id}/reindex
DELETE /api/v1/knowledge/documents/{id}
```
