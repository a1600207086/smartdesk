# 01 - Bootstrap

## Goal

Create the smallest runnable backend before adding MySQL, Redis, or an LLM.

## Request lifecycle

1. Tomcat receives `GET /api/v1/system/ping`.
2. Spring MVC maps the request to `SystemController.ping()`.
3. Jackson serializes the returned `ApiResponse`.
4. The client receives JSON.

## Concepts

### `@SpringBootApplication`

Combines component scanning, auto-configuration, and configuration support.

### `@RestController`

Registers a class as a web component and writes returned objects directly to the HTTP response.

### Record

An immutable data carrier. It is useful for API responses and request DTOs.

### Actuator

Provides production-oriented endpoints such as `/actuator/health` and `/actuator/metrics`.
