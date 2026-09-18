# 09 - Knowledge Document Management

## Goal

Add operational document management around the RAG pipeline:

- view one document
- delete a document
- reindex a document after changing the embedding model
- preserve original content for reindexing

## Original content storage

Migration `V6__add_document_content.sql` adds:

```text
knowledge_document.content
```

Reindexing needs the original document, not overlapped chunks. New documents always save the normalized original content.

Existing documents created before V6 may have a null content field. They cannot be reindexed until deleted and uploaded again.

## Delete flow

```text
DELETE /api/v1/knowledge/documents/{id}
  -> verify tenant ownership
  -> delete knowledge_chunk rows
  -> delete knowledge_document row
  -> increment Redis knowledge cache version
```

The cache version invalidation prevents old search results from being reused.

## Reindex flow

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

This is the first version of a reindex workflow. Production systems should run reindex asynchronously and support progress, retry, and atomic index switching.

## API endpoints

```text
GET    /api/v1/knowledge/documents/{id}
GET    /api/v1/knowledge/documents
POST   /api/v1/knowledge/documents/text
POST   /api/v1/knowledge/documents/upload
POST   /api/v1/knowledge/documents/{id}/reindex
DELETE /api/v1/knowledge/documents/{id}
```