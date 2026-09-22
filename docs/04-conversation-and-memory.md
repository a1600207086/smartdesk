# 04 - 会话与消息记忆

## 目标

持久化会话和消息，校验租户及用户所有权，实现消息分页，并在 Redis 中缓存近期上下文。

## 数据归属

每个请求都从 JWT 主体中获取身份信息：

```text
tenantId
userId
role
```

客户端不提供 `tenantId` 或 `userId`。`ConversationService.requireOwnedConversation()` 会在读取或修改会话前校验这两个值。

## 消息分页

消息按最新优先查询，以便高效分页：

```sql
WHERE conversation_id = ?
  AND id < ?
ORDER BY id DESC
LIMIT ?
```

服务会请求 `limit + 1` 行，多出来的一行用于判断是否还有更早消息。返回前再将选中的记录反转，使客户端按时间正序接收。

## Redis 近期记忆

Key:

```text
smartdesk:conversation:recent:{conversationId}
```

Value:

```text
JSON-serialized MessageResponse entries
```

追加消息时：

```text
RPUSH
LTRIM -recentMemorySize -1
EXPIRE
```

第一页会优先读取 Redis。带 `beforeId` 的分页始终读取 MySQL，因为 Redis 不保证保存更早的历史消息。

Redis 故障时采用降级放行：请求回退到 MySQL，并记录警告日志。

## 上下文裁剪

Agent 使用 `ConversationContextService` 获取上下文。

它加载近期消息，从最新消息开始向前遍历，直到达到字符预算。即使单条消息超过预算，也至少保留一条消息。

这是第一版的简单实现，后续可以替换为针对具体模型的 Token 计数方式。
