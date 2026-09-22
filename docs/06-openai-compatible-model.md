# 06 - OpenAI 兼容流式模型

## 目标

在不修改 Agent 编排器的情况下，用真实模型服务替换确定性的 Mock 模型。

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

API Key 从环境变量读取，绝不写入源代码管理。

## 流式协议

模型服务发送：

```text
POST {baseUrl}/chat/completions
Authorization: Bearer {apiKey}
Content-Type: application/json
Accept: text/event-stream
```

程序解析如下格式的行：

```text
data: {"choices":[{"delta":{"content":"你"}}]}
```

遇到以下内容时停止：

```text
data: [DONE]
```

Agent 会将 `message` 事件加入现有的 SSE 响应。

## 降级行为

当 `smartdesk.llm.enabled=false` 时选择 `MockAgentChatModel`，因此无需 API Key 或网络也能运行完整的 Agent、RAG 和工具链。

启用真实模型后，如果 API Key 为空，应用会以明确错误启动失败，而不是悄悄调用未认证接口。
