# 07 - RAG 知识库

## 目标

为 Agent 提供按租户隔离的文档导入和检索能力。

```text
text or txt/markdown file
  -> normalize
  -> chunk with overlap
  -> embedding
  -> store document and chunks in MySQL
  -> cosine similarity retrieval
  -> keyword reranking
  -> return cited chunks
  -> Agent tool searchKnowledge
```

## 当前实现

第一版使用 `HashEmbeddingModel`：

- lowercase normalization
- English word tokens
- Chinese characters and adjacent character bigrams
- signed hashing into a fixed-dimension vector
- L2 normalization

该方案是确定性的，不需要外部 API Key。语义能力不如真实 Embedding 模型，但可以完整验证 RAG 流程。

检索阶段目前加载租户的文本块，并在 Java 中计算余弦相似度。这适合学习项目和小规模数据，生产版本应替换为 Qdrant 或 Milvus 等向量数据库。

## 数据表

Migration `V4__create_knowledge_base_tables.sql` creates:

- `knowledge_document`
- `knowledge_chunk`

文本块保存正文和 `embedding_json`。唯一约束 `(tenant_id, checksum)` 用于防止同一租户重复上传文档。

## Redis 缓存

Search cache keys include a tenant-specific version:

```text
smartdesk:knowledge:search:{tenantId}:{version}:{topK}:{queryHash}
```

上传文档时会递增租户版本号，因此旧的搜索结果会自动失效。

## Agent 集成

`KnowledgeSearchTool` is registered as:

```text
searchKnowledge
```

路由器会将政策类和知识类问题发送给该工具，结果包含匹配文本块和引用元数据。

模型回答应该引用匹配的文档和文本块，而不是自行编造政策细节。

## 当前解析器支持

当前上传接口支持：

- `.txt`
- `.md`
- `.markdown`
- `text/*` content types

后续可以在同一文档服务边界后接入 Apache Tika 或 PDF/DOCX 解析。
