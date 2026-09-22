# 01 - 项目初始化

## 目标

在接入 MySQL、Redis 或大模型之前，先创建一个最小可运行的后端。

## 请求生命周期

1. Tomcat 接收 `GET /api/v1/system/ping`。
2. Spring MVC 将请求映射到 `SystemController.ping()`。
3. Jackson 将返回的 `ApiResponse` 序列化。
4. 客户端收到 JSON。

## 核心概念

### `@SpringBootApplication`

组合组件扫描、自动配置和配置支持。

### `@RestController`

将类注册为 Web 组件，并把返回对象直接写入 HTTP 响应。

### Record

不可变的数据载体，适合用于 API 响应和请求 DTO。

### Actuator

提供面向生产环境的接口，例如 `/actuator/health` 和 `/actuator/metrics`。
