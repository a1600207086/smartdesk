# 04 - Conversation and Message Memory

## Goal

Persist conversations and messages, enforce tenant/user ownership, paginate messages, and cache recent context in Redis.

## Data ownership

Every request gets identity from the JWT principal:

```text
tenantId
userId
role
```

The client does not provide `tenantId` or `userId`. `ConversationService.requireOwnedConversation()` checks both values before returning or modifying a conversation.

## Message pagination

Messages are queried newest-first for efficient pagination:

```sql
WHERE conversation_id = ?
  AND id < ?
ORDER BY id DESC
LIMIT ?
```

The service requests `limit + 1` rows. The extra row indicates whether older messages exist. The selected rows are then reversed before returning, so clients receive chronological order.

## Redis recent memory

Key:

```text
smartdesk:conversation:recent:{conversationId}
```

Value:

```text
JSON-serialized MessageResponse entries
```

On append:

```text
RPUSH
LTRIM -recentMemorySize -1
EXPIRE
```

The first page reads Redis before MySQL. Pagination with `beforeId` always reads MySQL because older history is not guaranteed to be in Redis.

Redis failure is fail-open: the request falls back to MySQL and logs a warning.

## Context trimming

The future Agent uses `ConversationContextService`.

It loads recent messages and walks backward from the newest message until the character budget is full. It always keeps at least one message, even if that message alone exceeds the budget.

This is a simple first version. Later it can be replaced with model-specific token counting.