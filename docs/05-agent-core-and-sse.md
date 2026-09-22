# 05 - Agent 核心与 SSE

## 目标

在不接入真实模型服务的情况下运行 Agent 请求：

```text
save user message
  -> start agent_run
  -> load conversation context
  -> route request
  -> optionally execute a tool
  -> stream model output
  -> save assistant message
  -> complete agent_run
```

## 核心组件

### AgentRouter

第一版采用规则路由。如果用户最新消息提到订单，并包含类似订单号的字符串，就路由到 `queryOrder`。

后续可以将路由器替换为 LLM 意图分类器，或替换为规划与执行型 Planner。

### ToolRegistry

工具实现以下接口：

```java
Map<String, Object> execute(Map<String, Object> arguments, AgentContext context)
```

第一个工具是 `queryOrder`。当前返回确定性的 Mock 数据，因此无需外部服务即可测试完整工具链。

### AgentChatModel

模型抽象接口为：

```java
void stream(AgentModelRequest request, AgentChunkConsumer consumer)
```

`MockAgentChatModel` 按句子输出回答分块。接入真实 OpenAI 兼容模型或本地模型时无需修改编排器。

### AgentOrchestrator

编排器负责执行顺序，并确保每次运行都记录：

- route
- status
- input message
- tool call arguments and result
- errors
- start and finish times

## SSE 事件

对话接口为：

```text
POST /api/v1/conversations/{conversationId}/chat
Accept: text/event-stream
```

事件类型：

```text
start    Agent run started
route    Router decision
tool     Tool execution started, completed, or failed
message  Streamed answer chunk
done     Final message ID and run ID
error    Agent execution error
```

Example:

```text
event:route
data:{"route":"TOOL_CALL","toolName":"queryOrder"}

event:tool
data:{"toolName":"queryOrder","status":"COMPLETED","result":{...}}

event:message
data:{"content":"根据工具查询结果..."}

event:done
data:{"runId":1,"messageId":2}
```

## 异步安全

SSE 完成会触发 Servlet ASYNC 调度。安全配置允许内部 `DispatcherType.ASYNC` 调度，同时原始 HTTP 请求仍要求 JWT 认证。

## 持久化

Migration `V3__create_agent_execution_tables.sql` creates:

- `agent_run`
- `tool_call_log`

这些表为延迟、工具成功率和 Agent 可观测性指标提供基础。
