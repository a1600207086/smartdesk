# 06 - OpenAI-Compatible Streaming Model

## Goal

Replace the deterministic Mock model with a real model provider without changing the Agent orchestrator.

```text
AgentOrchestrator
  -> AgentChatModel
       -> MockAgentChatModel                  when llm.enabled=false
       -> OpenAiCompatibleAgentChatModel      when llm.enabled=true
```

## Configuration

```yaml
smartdesk:
  llm:
    enabled: false
    base-url: https://api.deepseek.com/v1
    api-key:
    model: deepseek-chat
    timeout: 60s
    temperature: 0.3
    max-tokens: 1024
```

Environment variables:

```text
SMARTDESK_LLM_ENABLED
SMARTDESK_LLM_BASE_URL
SMARTDESK_LLM_API_KEY
SMARTDESK_LLM_MODEL
```

Examples:

DeepSeek:

```powershell
$env:SMARTDESK_LLM_ENABLED="true"
$env:SMARTDESK_LLM_BASE_URL="https://api.deepseek.com/v1"
$env:SMARTDESK_LLM_MODEL="deepseek-chat"
$env:SMARTDESK_LLM_API_KEY="your-api-key"
```

Qwen compatible mode:

```powershell
$env:SMARTDESK_LLM_ENABLED="true"
$env:SMARTDESK_LLM_BASE_URL="https://dashscope.aliyuncs.com/compatible-mode/v1"
$env:SMARTDESK_LLM_MODEL="qwen-plus"
$env:SMARTDESK_LLM_API_KEY="your-api-key"
```

OpenAI:

```powershell
$env:SMARTDESK_LLM_ENABLED="true"
$env:SMARTDESK_LLM_BASE_URL="https://api.openai.com/v1"
$env:SMARTDESK_LLM_MODEL="gpt-4o-mini"
$env:SMARTDESK_LLM_API_KEY="your-api-key"
```

The API key is read from the environment and is never written into source control.

## Streaming protocol

The provider sends:

```text
POST {baseUrl}/chat/completions
Authorization: Bearer {apiKey}
Content-Type: application/json
Accept: text/event-stream
```

It parses lines such as:

```text
data: {"choices":[{"delta":{"content":"你"}}]}
```

and stops on:

```text
data: [DONE]
```

The Agent adds `message` events to the existing SSE response.

## Fallback behavior

When `smartdesk.llm.enabled=false`, `MockAgentChatModel` is selected. This lets the complete Agent, RAG, and tool chain run without an API key or network access.

When enabled, a blank API key causes startup to fail with a clear error instead of silently calling an unauthenticated endpoint.