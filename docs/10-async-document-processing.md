# 10 - Async Document Processing

## Goal

Return from upload quickly and process chunks and embeddings on a background worker.

```text
upload request
  -> validate and save document as PROCESSING
  -> HTTP 202 Accepted
  -> background worker
      -> delete old chunks
      -> chunk original content
      -> batch embedding
      -> save chunks
      -> mark READY
  -> on failure mark FAILED and save error_message
```

## Thread pool

`KnowledgeExecutorConfig` defines:

```text
core pool size: 2
max pool size:  4
queue capacity: 50
thread prefix: smartdesk-knowledge-
```

Embedding and parsing work must not hold the HTTP request thread or a database transaction while an external embedding API is called.

## Transaction boundaries

`KnowledgeAsyncProcessor` uses `TransactionTemplate` in two phases:

1. Start transaction: load document, delete old chunks, mark PROCESSING.
2. Outside transaction: chunk and embed content.
3. Finish transaction: insert chunks and mark READY.
4. Failure transaction: mark FAILED and save error_message.

This pattern prevents a slow embedding API call from holding a database connection and transaction.

## API

```text
POST /api/v1/knowledge/documents/text/async      202
POST /api/v1/knowledge/documents/upload/async    202
POST /api/v1/knowledge/documents/{id}/retry     202
GET  /api/v1/knowledge/documents/{id}           status polling
```

A response may immediately be PROCESSING. Poll the document until status becomes READY or FAILED.

## Error handling

Failures are stored in:

```text
knowledge_document.error_message
```

A retry resets the document to PROCESSING, clears the previous error, and submits the document to the worker again.