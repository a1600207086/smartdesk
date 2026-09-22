# 10 - 异步文档处理

## 目标

上传接口快速返回，并由后台工作线程处理文本分块和向量化。

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

## 线程池

`KnowledgeExecutorConfig` defines:

```text
core pool size: 2
max pool size:  4
queue capacity: 50
thread prefix: smartdesk-knowledge-
```

调用外部 Embedding API 时，解析和向量化任务不能长期占用 HTTP 请求线程或数据库事务。

## 事务边界

`KnowledgeAsyncProcessor` uses `TransactionTemplate` in two phases:

1. Start transaction: load document, delete old chunks, mark PROCESSING.
2. Outside transaction: chunk and embed content.
3. Finish transaction: insert chunks and mark READY.
4. Failure transaction: mark FAILED and save error_message.

这种方式可以避免缓慢的 Embedding API 调用长期占用数据库连接和事务。

## API 接口

```text
POST /api/v1/knowledge/documents/text/async      202
POST /api/v1/knowledge/documents/upload/async    202
POST /api/v1/knowledge/documents/{id}/retry     202
GET  /api/v1/knowledge/documents/{id}           status polling
```

接口可能立即返回 `PROCESSING`。请轮询文档状态，直到变为 `READY` 或 `FAILED`。

## 错误处理

Failures are stored in:

```text
knowledge_document.error_message
```

重试会将文档重置为 `PROCESSING`，清除之前的错误信息，并重新提交给后台工作线程。
