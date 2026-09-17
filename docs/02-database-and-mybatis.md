# 02 - MySQL, Flyway and MyBatis

## Goal

Add durable relational storage and expose the first real business API.

## Database layers

```text
Controller
  -> Service
    -> TenantMapper
      -> MyBatis XML SQL
        -> MySQL
```

## Why MySQL instead of Redis?

- MySQL is the source of truth for users, tenants, conversations, and messages.
- Redis will later store short-lived data such as recent conversation memory, locks, and cache entries.
- Losing Redis may reduce performance, but should not lose durable business records.
- Losing MySQL means losing durable business records.

## Flyway

Flyway treats every SQL migration as an immutable version.

```text
V1__create_core_tables.sql
V2__...
V3__...
```

After a migration has been applied to a shared environment, do not edit it. Add a new migration instead.

## Index example

```sql
CREATE INDEX idx_conversation_tenant_user_created
    ON conversation (tenant_id, user_id, created_at);
```

The column order matters. A query filtering by `tenant_id` and `user_id` and sorting by `created_at` can use this composite index.

## Transaction

`TenantService.create()` is transactional. The duplicate-code check and insert execute in one transaction. The unique constraint on `tenant.code` remains the final protection against race conditions.
