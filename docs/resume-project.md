# SmartDesk 智能售后 Agent 平台

## 简历项目描述

基于 Java 21、Spring Boot、MySQL、Redis 和 MyBatis 开发智能售后客服平台，支持多租户认证、Agent 工具调用、RAG 知识库检索、SSE 流式对话和人工工单流转。

## 技术亮点

- 使用 JWT、BCrypt 和 Redis 实现登录认证、失败限流及 Token 黑名单；通过 `tenant_id` 实现多租户数据隔离。
- 设计 Agent 路由机制，根据用户意图调用订单查询、知识库检索和转人工工具，并持久化 Agent Run 与工具调用轨迹。
- 实现 TXT、Markdown、PDF、DOCX 文档异步解析、分块和 Embedding，使用相似度检索完成 RAG 问答，并保存回答引用。
- 基于 SSE 实现 Agent 流式响应，支持 `route`、`tool`、`citation`、`message` 和 `done` 事件。
- 设计人工工单状态机，限制 `OPEN → IN_PROGRESS → RESOLVED → CLOSED` 的非法回退，并实现 SLA 统计。
- 使用 Flyway 管理 10 个数据库版本迁移，编写 34 项自动化测试，测试通过率 100%。

## 面试时的项目难点

1. 如何保证 Agent 工具调用安全：对工具参数进行校验，并记录参数、结果、耗时和成功状态。
2. 如何实现 RAG：文档解析后分块，生成向量，计算查询与文本块相似度，再将高相关片段作为回答上下文。
3. 如何保证多租户隔离：所有业务查询携带当前用户的 `tenantId`，禁止仅凭资源 ID 查询数据。
4. Redis 在项目中的作用：登录失败限流、JWT 黑名单和会话近期记忆。

