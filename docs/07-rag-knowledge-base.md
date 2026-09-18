# 07 - RAG Knowledge Base

## Goal

Provide tenant-isolated document ingestion and retrieval for the Agent.

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

## Current implementation

The first version uses `HashEmbeddingModel`:

- lowercase normalization
- English word tokens
- Chinese characters and adjacent character bigrams
- signed hashing into a fixed-dimension vector
- L2 normalization

This is deterministic and requires no external API key. It is not as semantically strong as a real embedding model, but it exercises the complete RAG pipeline.

The retrieval stage currently loads the tenant's chunks and computes cosine similarity in Java. This is acceptable for a learning project and small datasets. A production version should replace it with a vector database such as Qdrant or Milvus.

## Tables

Migration `V4__create_knowledge_base_tables.sql` creates:

- `knowledge_document`
- `knowledge_chunk`

Chunks store text and `embedding_json`. A unique `(tenant_id, checksum)` constraint prevents duplicate documents in the same tenant.

## Redis cache

Search cache keys include a tenant-specific version:

```text
smartdesk:knowledge:search:{tenantId}:{version}:{topK}:{queryHash}
```

When a document is uploaded, the tenant version is incremented, so old search results are automatically bypassed.

## Agent integration

`KnowledgeSearchTool` is registered as:

```text
searchKnowledge
```

The router sends policy and knowledge-style questions to this tool. The result contains matched chunks and citation metadata.

The response from the model should cite the matching document and chunk instead of inventing policy details.

## Current parser support

The upload endpoint currently supports:

- `.txt`
- `.md`
- `.markdown`
- `text/*` content types

Apache Tika or PDF/DOCX parsing can be added later behind the same document service boundary.