# SmartDesk

SmartDesk is an intelligent after-sales and knowledge-base agent platform built with Java and Spring Boot.

## Current milestone

- Java 21 and Spring Boot 3.3.6
- MySQL, Flyway, and MyBatis
- Tenant API
- Spring Security and BCrypt
- JWT registration, login, logout, and profile API
- Redis failed-login rate limiting and token blacklist
- H2 integration tests

## Run tests

```powershell
.\mvnw.cmd test
```

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