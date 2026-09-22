# 08 - 真实 Embedding 模型

## 目标

在保留无 Key 降级方案的同时，从本地确定性 Hash 向量切换到 OpenAI 兼容 Embedding API。

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

模型服务调用：

```text
POST {baseUrl}/embeddings
Authorization: Bearer {apiKey}
```

## 批量向量化

文档导入会调用 `embedAll(chunks)`。真实模型服务会将文本块列表拆成批次，在服务支持时通过一次请求发送多个输入。

## 模型版本隔离

Migration `V5__add_embedding_model_to_knowledge_document.sql` adds:

```text
knowledge_document.embedding_model
```

Examples:

```text
hash-v1-256
openai:text-embedding-3-small:256
```

检索只比较模型 ID 与当前配置一致的文档文本块，避免混用 256 维 Hash 向量和 1536 维模型服务向量。

## 切换模型服务与重建索引

启用其他 Embedding 模型后，已有 `hash-v1-256` 文档会被忽略。开发环境中切换模型后请重新上传或重建知识库文档。

A 生产环境的重建索引流程应当：

1. Mark the old index version as inactive.
2. Build a new index in the background.
3. Swap the active version atomically.
4. Keep the old index for rollback.
