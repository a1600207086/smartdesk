# 02 - MySQL、Flyway 与 MyBatis

## 目标

增加持久化关系型存储，并实现第一个真实业务接口。

## 数据库分层

```text
Controller
  -> Service
    -> TenantMapper
      -> MyBatis XML SQL
        -> MySQL
```

## 为什么使用 MySQL 而不是 Redis？

- MySQL 是用户、租户、会话和消息的最终数据源。
- Redis 用于保存会话近期记忆、锁和缓存等短生命周期数据。
- Redis 故障可能降低性能，但不应导致持久化业务数据丢失。
- MySQL 故障则意味着持久化业务数据不可用。

## Flyway 数据库迁移

Flyway 将每个 SQL 迁移文件视为不可变版本。

```text
V1__create_core_tables.sql
V2__...
V3__...
```

迁移应用到共享环境后不要修改原文件，应新增迁移版本。

## Index example

```sql
CREATE INDEX idx_conversation_tenant_user_created
    ON conversation (tenant_id, user_id, created_at);
```

列顺序很重要。按 `tenant_id`、`user_id` 过滤并按 `created_at` 排序的查询可以使用该联合索引。

## 事务

`TenantService.create()` 使用事务。重复编码检查和插入在同一事务中执行，`tenant.code` 的唯一约束则是防止并发竞争的最后一道保护。
