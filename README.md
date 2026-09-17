# SmartDesk

SmartDesk is an intelligent after-sales and knowledge-base agent platform built with Java and Spring Boot.

## Current milestone

- Java 21
- Spring Boot 3.3.6
- REST API foundation
- MySQL-ready datasource configuration
- Flyway database migrations
- MyBatis persistence layer
- Tenant create/read API
- H2 integration tests

## Run tests

```powershell
.\mvnw.cmd test
```

## Configure MySQL

Set these environment variables in the terminal that starts the application:

```powershell
$env:SMARTDESK_DB_URL="jdbc:mysql://localhost:3306/smartdesk?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true"
$env:SMARTDESK_DB_USERNAME="smartdesk_app"
$env:SMARTDESK_DB_PASSWORD="your-local-password"
.\mvnw.cmd spring-boot:run
```

Flyway creates the database tables on startup.

## Tenant API

Create:

```powershell
$body = @{
  code = "acme"
  name = "Acme After Sale"
} | ConvertTo-Json

Invoke-RestMethod `
  -Method Post `
  -Uri http://localhost:8080/api/v1/tenants `
  -ContentType application/json `
  -Body $body
```

Query:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/tenants
Invoke-RestMethod http://localhost:8080/api/v1/tenants/1
```
