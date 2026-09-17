# 05 - Agent Core and SSE

## Goal

Run an Agent-style request without a real model provider:

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

## Components

### AgentRouter

The first version is rule-based. If the last user message mentions an order and contains an order-number-like token, it routes to `queryOrder`.

Later this router can be replaced by an LLM intent classifier or a plan-and-execute planner.

### ToolRegistry

Tools implement:

```java
Map<String, Object> execute(Map<String, Object> arguments, AgentContext context)
```

The first tool is `queryOrder`. It currently returns deterministic mock data so the complete tool chain can be tested without external services.

### AgentChatModel

The model abstraction is:

```java
void stream(AgentModelRequest request, AgentChunkConsumer consumer)
```

`MockAgentChatModel` emits response chunks by sentence. A real OpenAI-compatible or local model implementation can replace it without changing the orchestrator.

### AgentOrchestrator

The orchestrator owns the execution sequence and ensures that every run records:

- route
- status
- input message
- tool call arguments and result
- errors
- start and finish times

## SSE events

The chat endpoint is:

```text
POST /api/v1/conversations/{conversationId}/chat
Accept: text/event-stream
```

Event types:

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

## Async security

SSE completion triggers a Servlet ASYNC dispatch. The security configuration permits internal `DispatcherType.ASYNC` dispatches while the original HTTP request still requires JWT authentication.

## Persistence

Migration `V3__create_agent_execution_tables.sql` creates:

- `agent_run`
- `tool_call_log`

These tables provide the foundation for latency, tool success rate, and Agent observability metrics.