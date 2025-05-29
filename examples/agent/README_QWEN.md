# Qwen Agent 示例

本目录包含使用 Qwen 模型作为 LLM 提供商的 Agent 示例。

## 示例文件

### 1. QwenAgentExample.kt
完整的 Qwen Agent 示例，展示了：
- 如何配置 Qwen LLM 提供商
- 如何创建和配置 Agent
- 如何处理流式响应
- 如何处理工具调用（如果有）

### 2. SimpleQwenAgent.kt
简化的 Qwen Agent 示例，适合快速入门：
- 基本的 Agent 创建和配置
- 简单的问答交互
- 流式响应处理

## 运行前准备

### 1. 设置 API 密钥
在运行示例之前，需要设置 Qwen API 密钥：

```bash
export QWEN_API_KEY="your-qwen-api-key-here"
```

或者直接在代码中替换 `"your-api-key-here"` 为你的实际 API 密钥。

### 2. 安装依赖
确保项目已经包含了 `kastrax-qwen` 依赖（已在 `build.gradle.kts` 中配置）。

## 运行示例

### 运行完整示例
```bash
./gradlew :examples:agent:run -PmainClass=ai.kastrax.examples.agent.QwenAgentExampleKt
```

### 运行简单示例
```bash
./gradlew :examples:agent:run -PmainClass=ai.kastrax.examples.agent.SimpleQwenAgentKt
```

## 可用的 Qwen 模型

示例中使用的是 `QWEN2_5_72B_INSTRUCT` 模型，你也可以选择其他可用模型：

- **Qwen2.5 系列**：
  - `QWEN2_5_72B_INSTRUCT` - 72B 参数模型（推荐）
  - `QWEN2_5_32B_INSTRUCT` - 32B 参数模型
  - `QWEN2_5_14B_INSTRUCT` - 14B 参数模型
  - `QWEN2_5_7B_INSTRUCT` - 7B 参数模型
  - `QWEN2_5_3B_INSTRUCT` - 3B 参数模型

- **Qwen2.5 Coder 系列**（专门用于代码生成）：
  - `QWEN2_5_CODER_32B_INSTRUCT`
  - `QWEN2_5_CODER_14B_INSTRUCT`
  - `QWEN2_5_CODER_7B_INSTRUCT`

- **Qwen2.5 Math 系列**（专门用于数学问题）：
  - `QWEN2_5_MATH_72B_INSTRUCT`
  - `QWEN2_5_MATH_7B_INSTRUCT`

- **QwQ 系列**（推理模型）：
  - `QWQ_32B_PREVIEW`

- **Qwen-VL 系列**（多模态模型）：
  - `QWEN_VL_MAX`
  - `QWEN_VL_PLUS`

## 配置选项

### LLM 提供商配置
```kotlin
val llm = qwen {
    apiKey("your-api-key")           // API 密钥
    model(QwenModel.QWEN2_5_72B_INSTRUCT)  // 模型选择
    temperature(0.7)                  // 温度参数 (0.0-1.0)
    maxTokens(2000)                   // 最大生成令牌数
    topP(0.95)                        // Top-p 采样参数
    timeout(60)                       // 超时时间（秒）
}
```

### Agent 配置
```kotlin
val agent = agent {
    name = "AgentName"                // Agent 名称
    instructions = "..."              // Agent 指令
    model = llm                       // LLM 提供商
    defaultStreamOptions {            // 默认流式选项
        temperature(0.7)
        maxTokens(2000)
    }
}
```

## 注意事项

1. **API 密钥安全**：不要在代码中硬编码 API 密钥，建议使用环境变量。
2. **模型选择**：根据你的需求选择合适的模型，较大的模型通常性能更好但成本更高。
3. **超时设置**：根据网络情况和模型响应时间调整超时设置。
4. **错误处理**：示例中包含了基本的错误处理，实际使用时可能需要更详细的错误处理逻辑。

## 扩展示例

你可以基于这些示例创建更复杂的应用，比如：
- 添加工具调用功能
- 集成外部 API
- 实现对话历史管理
- 添加用户界面
- 实现多轮对话