# SmartDesk 智能售后 Agent 平台

SmartDesk 是一个面向电商售后场景的智能体应用平台，基于 Java 和 Spring Boot 开发，支持 Agent 路由、工具调用、RAG 知识库问答、SSE 流式对话、人工工单和运行监控。

项目主要用于展示 Java 后端、智能体开发、RAG 和系统工程化能力。
## Run tests

```powershell
.\mvnw.cmd test
```

## Run with Docker Compose

Docker Compose starts MySQL, Redis, and the SmartDesk application. The application
uses the Mock Agent by default, so no LLM or Embedding API key is required.

```powershell
docker compose up --build
```

After startup, open:

```text
http://localhost:8080/
http://localhost:8080/swagger-ui.html
```

To stop the services while keeping database data:

```powershell
docker compose down
```

The default Compose passwords are for local development only. Set
`SMARTDESK_DB_PASSWORD`, `MYSQL_ROOT_PASSWORD`, and `SMARTDESK_JWT_SECRET`
before starting the stack when using a shared or deployed environment.

## Start Redis

The Windows Redis service is installed as `Redis`:

```powershell
Start-Service Redis
Get-Service Redis
```

## Configure services

```powershell
$env:SMARTDESK_DB_URL="jdbc:mysql://localhost:3306/smartdesk?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:SMARTDESK_DB_USERNAME="smartdesk_app"
$env:SMARTDESK_DB_PASSWORD="your-local-database-password"
$env:SMARTDESK_JWT_SECRET="replace-with-a-random-secret-at-least-32-bytes-long"

.\mvnw.cmd spring-boot:run
```

## Authentication API

Bootstrap the first tenant. This endpoint is allowed only while the tenant table is empty:

```powershell
$tenant = @{
  code = "acme"
  name = "Acme After Sale"
} | ConvertTo-Json

Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/bootstrap/tenant `
  -ContentType application/json -Body $tenant
```

Register the first user. The first user in a tenant is assigned the `ADMIN` role:

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

Login:

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

Read the current profile:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/v1/auth/me `
  -Headers @{ Authorization = "Bearer $token" }
```

Logout:

```powershell
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/logout `
  -Headers @{ Authorization = "Bearer $token" }
```

## Conversation API

Create a conversation:

```powershell
$conversation = @{
  title = "Order consultation"
} | ConvertTo-Json

$created = Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/conversations `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType application/json `
  -Body $conversation

$conversationId = $created.data.id
```

Append a user message:

```powershell
$message = @{
  content = "Where is my order?"
} | ConvertTo-Json

Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/messages" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType application/json `
  -Body $message
```

Read recent messages:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/messages?limit=20" `
  -Headers @{ Authorization = "Bearer $token" }
```

Assistant messages include a `citations` array. Knowledge citations are stored in MySQL and returned from both database-backed and Redis-cached conversation history. User messages and answers without knowledge sources return an empty array.

## Agent SSE Chat

Send a message to the Agent:

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

The stream emits `start`, `route`, optional `tool` and `citation`, `message`, and `done` events.
Knowledge citations include the document id, title, chunk id, chunk index, similarity score, and a short snippet. The completed `tool` event returns only the query and match count; the full tool result remains in `tool_call_log` for auditing.

```text
event:citation
data:{"citations":[{"index":1,"documentId":5,"documentTitle":"Refund Policy Demo","chunkId":6,"chunkIndex":0,"score":0.5583,"snippet":"..."}]}
```

The current model is deterministic and does not require an API key. A real model provider can be added by implementing `AgentChatModel`.

## Agent Observability

List the latest Agent runs in a conversation. The default limit is 20 and the maximum is 100:

```powershell
$runs = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/agent-runs?limit=20" `
  -Headers @{ Authorization = "Bearer $token" }

$runs.data | Select-Object id, route, status, durationMs, startedAt | Format-Table
```

Inspect one run and its structured tool arguments, result, success flag, and duration:

```powershell
$runId = $runs.data[0].id

$run = Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/agent-runs/$runId" `
  -Headers @{ Authorization = "Bearer $token" }

$run.data | Format-List
$run.data.toolCalls | Format-List
```

Only the owner of a conversation can read its traces. An `ADMIN` can inspect tenant-scoped execution metrics:

```powershell
$metrics = Invoke-RestMethod `
  -Uri http://localhost:8080/api/v1/agent/metrics `
  -Headers @{ Authorization = "Bearer $token" }

$metrics.data | Format-List
```

The metrics contain run totals, completion rate, tool-call totals, tool success rate, and average tool duration. A normal `USER` receives HTTP `403` from this endpoint.

## Human Handoff Tickets

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

The router selects `createSupportTicket` before knowledge search. Repeated handoff requests in the same conversation reuse an existing `OPEN` or `IN_PROGRESS` ticket.

List visible tickets:

```powershell
$tickets = Invoke-RestMethod `
  -Uri http://localhost:8080/api/v1/tickets `
  -Headers @{ Authorization = "Bearer $token" }

$tickets.data |
  Select-Object id, ticketNo, subject, priority, status, updatedAt |
  Format-Table
```

A normal user sees only their own tickets. An `ADMIN` or `AGENT` sees all tickets in the tenant and can advance the workflow:

```powershell
$ticketId = $tickets.data[0].id
$statusBody = @{ status = "IN_PROGRESS" } | ConvertTo-Json

Invoke-RestMethod -Method Patch `
  -Uri "http://localhost:8080/api/v1/tickets/$ticketId/status" `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json; charset=utf-8" `
  -Body $statusBody
```

The normal workflow is `OPEN -> IN_PROGRESS -> RESOLVED -> CLOSED`. A resolved ticket can be reopened to `IN_PROGRESS`; a closed ticket is terminal.

Ticket SLA metrics are available to `ADMIN` and `AGENT` users:

```powershell
$ticketMetrics = Invoke-RestMethod `
  -Uri http://localhost:8080/api/v1/tickets/metrics `
  -Headers @{ Authorization = "Bearer $token" }

$ticketMetrics.data | Format-List
```

The response contains counts by status, the number of urgent tickets that are not yet resolved or closed, and the average resolution time in hours. Average resolution time only includes tickets with a non-null `resolvedAt`, and all metrics are scoped to the current tenant.

## Answer Feedback

Find the latest assistant message in a conversation:

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

Create or update the current user's rating. Repeating `PUT` updates the same feedback row instead of inserting a duplicate:

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

Allowed ratings are `HELPFUL` and `NOT_HELPFUL`. The optional comment is limited to 500 characters. Only assistant messages in conversations owned by the authenticated user can be rated.

Read or delete the current user's rating:

```powershell
Invoke-RestMethod `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/messages/$messageId/feedback" `
  -Headers @{ Authorization = "Bearer $token" }

Invoke-RestMethod -Method Delete `
  -Uri "http://localhost:8080/api/v1/conversations/$conversationId/messages/$messageId/feedback" `
  -Headers @{ Authorization = "Bearer $token" }
```

An `ADMIN` can read tenant-scoped quality metrics:

```powershell
$summary = Invoke-RestMethod `
  -Uri http://localhost:8080/api/v1/feedback/summary `
  -Headers @{ Authorization = "Bearer $token" }

$summary.data | Format-List
```

The summary contains `totalCount`, `helpfulCount`, `notHelpfulCount`, and `helpfulRate`. A normal `USER` receives HTTP `403` from this endpoint.

## Real LLM Provider

The project defaults to `MockAgentChatModel`.

To enable an OpenAI-compatible provider:

```powershell
$env:SMARTDESK_LLM_ENABLED="true"
$env:SMARTDESK_LLM_BASE_URL="https://api.deepseek.com/v1"
$env:SMARTDESK_LLM_MODEL="deepseek-chat"
$env:SMARTDESK_LLM_API_KEY="your-api-key"

.\mvnw.cmd spring-boot:run
```

The provider calls:

```text
POST {baseUrl}/chat/completions
```

and parses OpenAI-compatible streaming events. Set `SMARTDESK_LLM_ENABLED=false` or omit it to keep the Mock model.

## Knowledge Base

Upload text as an authenticated ADMIN:

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

Search:

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

Policy questions are routed to the Agent tool `searchKnowledge` and retrieved chunks are included in the model context.

Upload a `txt`, `md`, `pdf`, or `docx` file (maximum 10 MB):

```powershell
$filePath = "C:\path\to\refund-policy.pdf"

curl.exe -X POST "http://localhost:8080/api/v1/knowledge/documents/upload/async" `
  -H "Authorization: Bearer $token" `
  -F "title=Refund Policy" `
  -F "sourceUri=internal://policy/refund-pdf" `
  -F "file=@$filePath"
```

Apache Tika detects the real file type, then PDFBox or Apache POI extracts PDF/DOCX text before the existing chunking and embedding pipeline runs. The extension and detected content type must match.

## Real Embedding Model

The knowledge base defaults to `HashEmbeddingModel`. To use a real OpenAI-compatible embedding endpoint:

```powershell
$env:SMARTDESK_EMBEDDING_ENABLED="true"
$env:SMARTDESK_EMBEDDING_BASE_URL="https://api.openai.com/v1"
$env:SMARTDESK_EMBEDDING_API_KEY="your-embedding-api-key"
$env:SMARTDESK_EMBEDDING_MODEL="text-embedding-3-small"
$env:SMARTDESK_EMBEDDING_DIMENSIONS="256"

.\mvnw.cmd spring-boot:run
```

Each document records its embedding model id. Existing Hash documents are ignored when a real embedding model is active; re-upload or reindex them after switching models.

## Knowledge Document Management

List and inspect documents:

```powershell
Invoke-RestMethod -Uri http://localhost:8080/api/v1/knowledge/documents `
  -Headers @{ Authorization = "Bearer $token" }

Invoke-RestMethod -Uri http://localhost:8080/api/v1/knowledge/documents/1 `
  -Headers @{ Authorization = "Bearer $token" }
```

Reindex after switching embedding models:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/knowledge/documents/1/reindex `
  -Headers @{ Authorization = "Bearer $token" }
```

Delete a document:

```powershell
Invoke-RestMethod -Method Delete `
  -Uri http://localhost:8080/api/v1/knowledge/documents/1 `
  -Headers @{ Authorization = "Bearer $token" }
```

## Async Knowledge Processing

## OpenAPI Documentation

After starting the application, open the interactive API documentation:

```text
http://localhost:8080/swagger-ui.html
```

The OpenAPI JSON is available at:

```text
http://localhost:8080/v3/api-docs
```

Click `Authorize` in Swagger UI and enter the JWT returned by the login API with the `Bearer ` prefix. Public endpoints such as login and health checks can be tested without a token; protected endpoints use the configured JWT security scheme.

Upload asynchronously:

```powershell
Invoke-RestMethod -Method Post `
  -Uri http://localhost:8080/api/v1/knowledge/documents/text/async `
  -Headers @{ Authorization = "Bearer $token" } `
  -ContentType "application/json; charset=utf-8" `
  -Body ([Text.Encoding]::UTF8.GetBytes($documentBody))
```

The response returns `202 Accepted` and typically has:

```text
status: PROCESSING
```

Poll:

```powershell
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/knowledge/documents/$documentId" `
  -Headers @{ Authorization = "Bearer $token" }
```

Retry a failed document:

```powershell
Invoke-RestMethod -Method Post `
  -Uri "http://localhost:8080/api/v1/knowledge/documents/$documentId/retry" `
  -Headers @{ Authorization = "Bearer $token" }
```
