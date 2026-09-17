# SmartDesk

SmartDesk is an intelligent after-sales and knowledge-base agent platform built with Java and Spring Boot.

## Current milestone

- Java 21
- Spring Boot 3.3.6
- REST API foundation
- Unified API response
- Actuator health endpoint
- MockMvc smoke test

## Run locally

```powershell
mvn spring-boot:run
```

Then call:

```powershell
Invoke-RestMethod http://localhost:8080/api/v1/system/ping
```

## Health check

```powershell
Invoke-RestMethod http://localhost:8080/actuator/health
```
