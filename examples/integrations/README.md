# KastraX 集成示例

本目录包含了 KastraX 与不同 LLM 提供商集成的示例代码，展示了如何使用不同的模型和提供商。

## 主要集成

KastraX 支持以下主要 LLM 提供商：

1. **OpenAI**
   - GPT-3.5 Turbo
   - GPT-4
   - GPT-4o

2. **DeepSeek**
   - DeepSeek Chat
   - DeepSeek Coder

3. **Anthropic**
   - Claude 3 Opus
   - Claude 3 Sonnet
   - Claude 3 Haiku

## 示例说明

### OpenAI 示例

`openai/OpenAIExample.kt` 展示了 OpenAI 集成的使用，包括：

- 配置 OpenAI 模型
- 基本文本生成
- 流式响应
- 工具调用

### DeepSeek 示例

`deepseek/DeepSeekExample.kt` 展示了 DeepSeek 集成的使用，包括：

- 配置 DeepSeek 模型
- 文本生成
- 流式响应
- 代码生成

### Anthropic 示例

`anthropic/AnthropicExample.kt` 展示了 Anthropic 集成的使用，包括：

- 配置 Anthropic 模型
- 文本生成
- 流式响应
- 多模态输入

## 使用方法

要运行示例，请执行以下命令：

```bash
./gradlew :examples:run --args="ai.kastrax.examples.integrations.openai.OpenAIExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.integrations.deepseek.DeepSeekExampleKt"
./gradlew :examples:run --args="ai.kastrax.examples.integrations.anthropic.AnthropicExampleKt"
```

## 注意事项

运行这些示例需要相应的 API 密钥。您可以通过以下方式提供 API 密钥：

1. 设置环境变量：
   ```
   export OPENAI_API_KEY=your-api-key
   export DEEPSEEK_API_KEY=your-api-key
   export ANTHROPIC_API_KEY=your-api-key
   ```

2. 在代码中直接设置（不推荐用于生产环境）：
   ```kotlin
   model = openAi {
       apiKey("your-api-key")
   }
   ```
