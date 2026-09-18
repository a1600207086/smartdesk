# 08 - Real Embedding Model

## Goal

Switch from the deterministic local hash embedding to an OpenAI-compatible embedding API while preserving a no-key fallback.

```text
smartdesk.embedding.enabled=false
  -> HashEmbeddingModel

smartdesk.embedding.enabled=true
  -> OpenAiCompatibleEmbeddingModel
```

## Configuration

```yaml
smartdesk:
  embedding:
    enabled: false
    base-url: https://api.openai.com/v1
    api-key:
    model: text-embedding-3-small
    dimensions: 256
    timeout: 60s
    batch-size: 32
```

Environment variables:

```text
SMARTDESK_EMBEDDING_ENABLED
SMARTDESK_EMBEDDING_BASE_URL
SMARTDESK_EMBEDDING_API_KEY
SMARTDESK_EMBEDDING_MODEL
SMARTDESK_EMBEDDING_DIMENSIONS
```

The provider calls:

```text
POST {baseUrl}/embeddings
Authorization: Bearer {apiKey}
```

## Batch embedding

Document ingestion calls `embedAll(chunks)`. The real provider splits chunk lists into batches and sends multiple inputs in one request where the provider supports it.

## Model version isolation

Migration `V5__add_embedding_model_to_knowledge_document.sql` adds:

```text
knowledge_document.embedding_model
```

Examples:

```text
hash-v1-256
openai:text-embedding-3-small:256
```

Retrieval only compares chunks from documents whose model id matches the currently configured model. This prevents 256-dimensional hash vectors and 1536-dimensional provider vectors from being mixed.

## Switching provider and reindexing

Existing `hash-v1-256` documents are ignored when a different embedding model is enabled. For development, re-upload or rebuild the knowledge documents after switching.

A production reindex workflow should:

1. Mark the old index version as inactive.
2. Build a new index in the background.
3. Swap the active version atomically.
4. Keep the old index for rollback.