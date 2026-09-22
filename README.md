# SmartDesk 智能售后 Agent 平台

SmartDesk 是一个面向电商售后场景的智能体应用平台，基于 Java 和 Spring Boot 开发，支持 Agent 路由、工具调用、RAG 知识库问答、SSE 流式对话、人工工单和运行监控。

项目主要用于展示 Java 后端、智能体开发、RAG 和系统工程化能力。

## 项目体验

- 本地控制台：[打开 SmartDesk](http://localhost:8080/)
- API 健康检查：[查看服务状态](http://localhost:8080/actuator/health)
- API 文档：[打开 Swagger UI](http://localhost:8080/swagger-ui.html)
- OpenAPI JSON：[查看接口定义](http://localhost:8080/v3/api-docs)

以上链接需要先在本地启动项目。部署到云服务器后，将链接中的
`http://localhost:8080` 替换为公网访问地址即可作为在线演示入口。

## 核心能力

- Agent 路由：根据订单、政策咨询和转人工意图选择对应工具。
- 工具调用：支持订单查询、知识库检索和人工客服工单。
- RAG 知识库：支持文档解析、分块、Embedding、相似度检索和回答引用。
- 流式对话：基于 SSE 返回 `route`、`tool`、`citation`、`message` 和 `done` 事件。
- 可视化控制台：支持登录、会话管理、知识库管理、Agent 运行轨迹、工单和评价指标。
- 工程化能力：支持 MySQL、Redis、Flyway、Docker Compose 和 OpenAPI 文档。

## 运行测试

```powershell
.\mvnw.cmd test
```

## 使用 Docker Compose 运行

Docker Compose 会启动 MySQL、Redis 和 SmartDesk 应用。项目默认使用 Mock Agent，
不需要配置大模型或 Embedding API Key。

```powershell
docker compose up --build
```

启动后访问：

```text
http://localhost:8080/
http://localhost:8080/swagger-ui.html
```

停止服务但保留数据库数据：

```powershell
docker compose down
```

Compose 中的默认密码仅用于本地开发。用于共享或部署环境时，请设置
`SMARTDESK_DB_PASSWORD`、`MYSQL_ROOT_PASSWORD` 和 `SMARTDESK_JWT_SECRET`
后再启动服务。

## 启动 Redis

Windows Redis 服务名称为 `Redis`：

```powershell
Start-Service Redis
Get-Service Redis
```

## 配置服务

```powershell
$env:SMARTDESK_DB_URL="jdbc:mysql://localhost:3306/smartdesk?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:SMARTDESK_DB_USERNAME="smartdesk_app"
$env:SMARTDESK_DB_PASSWORD="your-local-database-password"
$env:SMARTDESK_JWT_SECRET="replace-with-a-random-secret-at-least-32-bytes-long"

.\mvnw.cmd spring-boot:run
```

## 认证接口

初始化第一个租户。只有租户表为空时才允许调用该接口：

```powershell
$tenant = @{
  code = "acme"
  name = "智能售后演示租户"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/bootstrap/tenant `
  -ContentType application/json -Body $tenant
```

注册第一个用户。租户中的第一个用户会自动获得 `ADMIN` 角色：

```powershell
$body = @{
  tenantCode = "acme"
  username = "alice"
  displayName = "Alice"
  password = "Password123!"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/register `
  -ContentType application/json -Body $body
```

登录：

```powershell
$login = @{
  tenantCode = "acme"
  username = "alice"
  password = "Password123!"
} | ConvertTo-Json

$response = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login `
  -ContentType application/json -Body $login

$token = $response.data.accessToken
```

查询当前用户信息：

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/v1/auth/me `
  -Headers @{ Authorization = "Bearer $token" }
```

退出登录：

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/logout `
  -Headers @{ Authorization = "Bearer $token" }
```

## 会话接口

创建会话：

```powershell
$conversation = @{
  title = "订单咨询"
} | ConvertTo-Json

$created = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/conversations `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType application/json `
  -Body $conversation

$conversationId = $created.data.id
```

追加用户消息：

```powershell
$message = @{
  content = "我的订单到哪里了？"
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/messages" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType application/json `
  -Body $message
```

查询最近消息：

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/messages?limit=20" `
  -Headers @{ Authorization = "Bearer $token" }
```

助手消息包含 `citations` 数组。知识库引用会保存到 MySQL，并在从数据库读取或从 Redis 缓存读取会话历史时一并返回。用户消息以及没有知识来源的回答会返回空数组。

## Agent SSE 对话

向 Agent 发送消息：

```powershell
$chat = @{
  message = "订单号 A10001 什么时候到？"
} | ConvertTo-Json

curl.exe -N -X POST `
  "http://localhost:8080/api/v1/conversations/$conversationId/chat" `
  -H "Authorization: Bearer $token" `
  -H "Content-Type: application/json" `
  -H "Accept: text/event-stream" `
  -d $chat
```

数据流会依次发送 `start`、`route`、可选的 `tool` 和 `citation`、`message` 以及 `done` 事件。
知识库引用包含文档 ID、标题、分块 ID、分块序号、相似度分数和文本摘要。工具执行完成后的 `tool` 事件只返回查询条件和匹配数量，完整工具结果会保存在 `tool_call_log` 中用于审计。

```text
event:citation
data:{"citations":[{"index":1,"documentId":5,"documentTitle":"Refund Policy Demo","chunkId":6,"chunkIndex":0,"score":0.5583,"snippet":"..."}]}
```

当前模型是确定性的 Mock 模型，不需要 API Key。实现 `AgentChatModel` 接口即可接入真实模型服务。

## Agent 可观测性

查询会话中最近的 Agent 运行记录。默认返回 20 条，最多返回 100 条：

```powershell
$runs = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/agent-runs?limit=20" `
  -Headers @{ Authorization = "Bearer $token" }

$runs.data | Select-Object id, route, status, durationMs, startedAt | Format-Table
```

查看某次运行记录，以及结构化的工具参数、结果、成功状态和耗时：

```powershell
$runId = $runs.data[0].id

$run = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/agent-runs/$runId" `
  -Headers @{ Authorization = "Bearer $token" }

$run.data | Format-List
$run.data.toolCalls | Format-List
```

只有会话所有者可以读取该会话的执行轨迹。`ADMIN` 可以查看当前租户范围内的执行指标：

```powershell
$metrics = Invoke-RestMethod `
  -Uri http://localhost:8080/api/v1/agent/metrics `
  -Headers @{ Authorization = "Bearer $token" }

$metrics.data | Format-List
```

指标包括运行总数、完成率、工具调用总数、工具成功率和平均工具耗时。普通 `USER` 访问该接口会收到 HTTP `403`。

## 人工客服工单

Ask the Agent to create a human-support ticket:

```powershell
$chatBody = @{
  message = "退款问题一直没有解决，请帮我转人工客服"
} | ConvertTo-Json

$chatResponse = Invoke-WebRequest `
  -UseBasicParsing `
  -Method Post `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/chat" `
  -Headers @{
    Authorization = "Bearer $token"
    Accept = "text/event-stream"
  } `
  -ContentType "application/json; charset=utf-8" `
  -Body ([Text.Encoding]::UTF8.GetBytes($chatBody))

$chatResponse.Content
```

路由器会优先选择 `createSupportTicket`，再考虑知识库检索。同一会话中重复请求转人工时，会复用已有的 `OPEN` 或 `IN_PROGRESS` 工单。

查询当前用户可见的工单：

```powershell
$tickets = Invoke-RestMethod `
  -Uri http://localhost:8080/api/v1/tickets `
  -Headers @{ Authorization = "Bearer $token" }

$tickets.data |
  Select-Object id, ticketNo, subject, priority, status, updatedAt |
  Format-Table
```

普通用户只能看到自己的工单。`ADMIN` 或 `AGENT` 可以看到当前租户的全部工单，并推进工单状态：

```powershell
$ticketId = $tickets.data[0].id
$statusBody = @{ status = "IN_PROGRESS" } | ConvertTo-Json

Invoke-RestMethod -Method Patch `
  -Uri "http://localhost:8080/api/v1/tickets/$ticketId/status" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json; charset=utf-8" `
  -Body $statusBody
```

正常状态流转为 `OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED`。已解决的工单可以重新打开为 `IN_PROGRESS`，已关闭的工单不能再次流转。

`ADMIN` 和 `AGENT` 用户可以查看工单 SLA 指标：

```powershell
$ticketMetrics = Invoke-RestMethod `
  -Uri http://localhost:8080/api/v1/tickets/metrics `
  -Headers @{ Authorization = "Bearer $token" }

$ticketMetrics.data | Format-List
```

响应包含各状态工单数量、尚未解决或关闭的紧急工单数量，以及平均解决时长（小时）。平均解决时长只统计 `resolvedAt` 不为空的工单，所有指标都限定在当前租户范围内。

## 回答评价

查询会话中最近的一条助手消息：

```powershell
$messages = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/messages?limit=50" `
  -Headers @{ Authorization = "Bearer $token" }

$assistantMessage = $messages.data.items |
  Where-Object { $_.role -eq "ASSISTANT" } |
  Select-Object -Last 1

$messageId = $assistantMessage.id
$messageId
```

创建或更新当前用户的评价。重复执行 `PUT` 会更新同一条评价记录，而不会插入重复数据：

```powershell
$feedback = @{
  rating = "HELPFUL"
  comment = "答案准确，并且给出了知识库引用"
} | ConvertTo-Json

Invoke-RestMethod -Method Put `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/messages/$messageId/feedback" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json; charset=utf-8" `
  -Body ([Text.Encoding]::UTF8.GetBytes($feedback))
```

允许的评价值为 `HELPFUL` 和 `NOT_HELPFUL`。可选评论最多 500 个字符。只有当前认证用户拥有的会话中的助手消息可以被评价。

读取或删除当前用户的评价：

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/messages/$messageId/feedback" `
  -Headers @{ Authorization = "Bearer $token" }

Invoke-RestMethod -Method Delete `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/messages/$messageId/feedback" `
  -Headers @{ Authorization = "Bearer $token" }
```

`ADMIN` 可以读取当前租户范围内的回答质量指标：

```powershell
$summary = Invoke-RestMethod `
  -Uri http://localhost:8080/api/v1/feedback/summary `
  -Headers @{ Authorization = "Bearer $token" }

$summary.data | Format-List
```

汇总结果包含 `totalCount`、`helpfulCount`、`notHelpfulCount` 和 `helpfulRate`。普通 `USER` 访问该接口会收到 HTTP `403`。

## 接入真实大模型

项目默认使用 `MockAgentChatModel`。

启用 OpenAI 兼容模型服务：

```powershell
$env:SMARTDESK_LLM_ENABLED="true"
$env:SMARTDESK_LLM_BASE_URL="https://api.deepseek.com/v1"
$env:SMARTDESK_LLM_MODEL="deepseek-chat"
$env:SMARTDESK_LLM_API_KEY="your-api-key"

.\mvnw.cmd spring-boot:run
```

模型服务调用以下接口：

```text
POST {baseUrl}/chat/completions
```

并解析 OpenAI 兼容的流式事件。设置 `SMARTDESK_LLM_ENABLED=false` 或不设置该变量，即可继续使用 Mock 模型。

## 知识库

使用已认证的 `ADMIN` 用户上传文本：

```powershell
$document = @{
  title = "退款政策"
  content = "商品签收后七天内可以申请无理由退款，审核通过后三个工作日到账。"
  sourceUri = "internal://policy/refund"
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/knowledge/documents/text `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json; charset=utf-8" `
  -Body ([Text.Encoding]::UTF8.GetBytes($document))
```

搜索知识库：

```powershell
$search = @{
  query = "退货后多久退款？"
  topK = 3
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/knowledge/search `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json; charset=utf-8" `
  -Body ([Text.Encoding]::UTF8.GetBytes($search))
```

政策类问题会路由到 Agent 工具 `searchKnowledge`，检索到的文本块会加入模型上下文。

上传 `txt`、`md`、`pdf` 或 `docx` 文件（最大 10 MB）：

```powershell
$filePath = "C:\path\to\refund-policy.pdf"

curl.exe -X POST "http://localhost:8080/api/v1/knowledge/documents/upload/async" `
  -H "Authorization: Bearer $token" `
  -F "title=Refund Policy" `
  -F "sourceUri=internal://policy/refund-pdf" `
  -F "file=@$filePath"
```

Apache Tika 会检测文件真实类型，然后由 PDFBox 或 Apache POI 提取 PDF/DOCX 文本，再进入现有的分块和向量化流程。文件扩展名必须与检测到的内容类型一致。

## 接入真实 Embedding 模型

知识库默认使用 `HashEmbeddingModel`。使用真实的 OpenAI 兼容 Embedding 接口：

```powershell
$env:SMARTDESK_EMBEDDING_ENABLED="true"
$env:SMARTDESK_EMBEDDING_BASE_URL="https://api.openai.com/v1"
$env:SMARTDESK_EMBEDDING_API_KEY="your-embedding-api-key"
$env:SMARTDESK_EMBEDDING_MODEL="text-embedding-3-small"
$env:SMARTDESK_EMBEDDING_DIMENSIONS="256"

.\mvnw.cmd spring-boot:run
```

每个文档都会记录 Embedding 模型 ID。启用真实 Embedding 模型后，已有的 Hash 文档会被忽略；切换模型后请重新上传或执行重建索引。

## 知识库文档管理

查询和查看文档：

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/v1/knowledge/documents `
  -Headers @{ Authorization = "Bearer $token" }

Invoke-RestMethod -Uri http://localhost:8080/api/v1/knowledge/documents/1 `
  -Headers @{ Authorization = "Bearer $token" }
```

切换 Embedding 模型后重建索引：

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/knowledge/documents/1/reindex `
  -Headers @{ Authorization = "Bearer $token" }
```

删除文档：

```powershell
Invoke-RestMethod -Method Delete `
  -Uri http://localhost:8080/api/v1/knowledge/documents/1 `
  -Headers @{ Authorization = "Bearer $token" }
```

## 异步知识库处理

## OpenAPI 接口文档

启动应用后，打开交互式 API 文档：

```text
http://localhost:8080/swagger-ui.html
```

OpenAPI JSON 地址：

```text
http://localhost:8080/v3/api-docs
```

在 Swagger UI 中点击 `Authorize`，输入登录接口返回的 JWT，并添加 `Bearer ` 前缀。登录和健康检查等公开接口无需 Token 即可测试，受保护接口使用已配置的 JWT 安全方案。

异步上传：

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/knowledge/documents/text/async `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json; charset=utf-8" `
  -Body ([Text.Encoding]::UTF8.GetBytes($documentBody))
```

响应状态为 `202 Accepted`，通常包含：

```text
status: PROCESSING
```

轮询处理状态：

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/knowledge/documents/$documentId" `
  -Headers @{ Authorization = "Bearer $token" }
```

重试处理失败的文档：

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/knowledge/documents/$documentId/retry" `
  -Headers @{ Authorization = "Bearer $token" }
```
