# KastraX DeepSeek 集成详解

本文档详细介绍了 KastraX 框架与 DeepSeek 大语言模型的集成。DeepSeek 提供了一系列强大的 AI 模型，包括通用对话模型、代码生成模型和数学模型等。

## 1. 简介

DeepSeek 是一家领先的 AI 公司，提供了多种高性能的大语言模型。KastraX 框架通过 `kastrax-deepseek` 模块提供了与 DeepSeek API 的无缝集成，使开发者能够轻松地在应用中使用 DeepSeek 的模型。

### 1.1 支持的模型

KastraX 支持以下 DeepSeek 模型：

- **DeepSeek Chat**：通用对话模型
  - `deepseek-chat`
  - `deepseek-chat-v1`
  - `deepseek-chat-v1.5`

- **DeepSeek Coder**：代码生成模型
  - `deepseek-coder`
  - `deepseek-coder-v1`
  - `deepseek-coder-v1.5`

- **DeepSeek Math**：数学问题解决模型
  - `deepseek-math`

- **DeepSeek Lite**：轻量级模型
  - `deepseek-lite`
  - `deepseek-lite-v1`
  - `deepseek-lite-v1.5`

此外，KastraX 还支持使用自定义模型 ID。

## 2. 安装和配置

### 2.1 添加依赖

在项目的 `build.gradle.kts` 文件中添加 DeepSeek 集成依赖：

```kotlin
dependencies {
    implementation("ai.kastrax:kastrax-core:0.1.0")
    implementation("ai.kastrax:kastrax-integrations-deepseek:0.1.0")
}
```

### 2.2 配置 API 密钥

有两种方式可以配置 DeepSeek API 密钥：

1. **环境变量**：设置 `DEEPSEEK_API_KEY` 环境变量
   ```bash
   export DEEPSEEK_API_KEY="your-api-key-here"
   ```

2. **代码中显式设置**：
   ```kotlin
   val provider = deepSeek {
       model(DeepSeekModel.DEEPSEEK_CHAT)
       apiKey("your-api-key-here")
   }
   ```

## 3. 基本用法

### 3.1 创建 DeepSeek 提供商

使用 DSL 创建 DeepSeek 提供商：

```kotlin
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek

// 使用默认模型和环境变量中的 API 密钥
val provider1 = deepSeek()

// 指定模型和 API 密钥
val provider2 = deepSeek {
    model(DeepSeekModel.DEEPSEEK_CHAT)
    apiKey("your-api-key-here")
}

// 使用自定义模型 ID
val provider3 = deepSeek {
    model("custom-model-id")
    apiKey("your-api-key-here")
}
```

### 3.2 在代理中使用 DeepSeek

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek

val myAgent = agent {
    name = "DeepSeek 助手"
    instructions = "你是一个有帮助的助手，可以回答问题和提供信息。"
    
    // 使用 DeepSeek 模型
    model = deepSeek {
        model(DeepSeekModel.DEEPSEEK_CHAT)
        // apiKey("your-api-key-here")  // 可选，如果未设置则使用环境变量
    }
    
    // 添加工具（可选）
    tools {
        tool(calculatorTool)
        tool(weatherTool)
    }
}

// 生成回复
val response = myAgent.generate("你好，请介绍一下自己。")
println(response.text)

// 流式生成
myAgent.stream("请解释量子计算的基本原理。").collect { chunk ->
    print(chunk)
}
```

## 4. 高级功能

### 4.1 工具调用

DeepSeek 支持工具调用功能，允许模型使用外部工具来完成任务：

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.core.tools.tool
import ai.kastrax.core.schema.*
import ai.kastrax.integrations.deepseek.deepSeek

// 创建一个计算器工具
val calculatorTool = tool {
    id = "calculator"
    name = "计算器"
    description = "执行数学计算"
    
    input {
        obj {
            field("expression", string()) {
                description = "要计算的数学表达式"
                required = true
            }
        }
    }
    
    output {
        obj {
            field("result", number()) {
                description = "计算结果"
            }
        }
    }
    
    execute { input ->
        val expression = input.getString("expression")
        val result = evaluateExpression(expression)
        
        output {
            "result" to result
        }
    }
}

// 创建使用 DeepSeek 的代理
val myAgent = agent {
    name = "DeepSeek 助手"
    instructions = "你是一个有帮助的助手，可以回答问题和执行计算。"
    
    model = deepSeek()
    
    tools {
        tool(calculatorTool)
    }
}

// 使用工具
val response = myAgent.generate("计算 123 + 456 的结果是多少？")
println(response.text)
```

### 4.2 嵌入生成

DeepSeek 提供了文本嵌入功能，可以将文本转换为向量表示：

```kotlin
import ai.kastrax.integrations.deepseek.deepSeek

// 创建 DeepSeek 提供商
val provider = deepSeek {
    model("deepseek-embedding")  // 使用嵌入模型
}

// 生成单个文本的嵌入
val embedding = provider.embedText("这是一段示例文本")
println("嵌入维度: ${embedding.size}")

// 批量生成嵌入
val texts = listOf(
    "第一段文本",
    "第二段文本",
    "第三段文本"
)
val embeddings = provider.embedTexts(texts)
println("批量嵌入数量: ${embeddings.size}")
```

### 4.3 自定义请求选项

可以通过 `LlmOptions` 自定义请求参数：

```kotlin
import ai.kastrax.core.llm.LlmOptions
import ai.kastrax.integrations.deepseek.deepSeek

val provider = deepSeek()

val options = LlmOptions(
    temperature = 0.7,
    topP = 0.9,
    maxTokens = 1000,
    stop = listOf("###"),
    frequencyPenalty = 0.5,
    presencePenalty = 0.5
)

val response = provider.generate(messages, options)
```

## 5. 最佳实践

### 5.1 模型选择

- **通用对话**：使用 `DeepSeekModel.DEEPSEEK_CHAT` 或 `DeepSeekModel.DEEPSEEK_CHAT_V1_5`
- **代码生成**：使用 `DeepSeekModel.DEEPSEEK_CODER` 或 `DeepSeekModel.DEEPSEEK_CODER_V1_5`
- **数学问题**：使用 `DeepSeekModel.DEEPSEEK_MATH`
- **资源受限环境**：使用 `DeepSeekModel.DEEPSEEK_LITE` 系列

### 5.2 性能优化

- 使用流式响应提高用户体验
- 适当设置 `maxTokens` 参数控制生成长度
- 使用 `temperature` 和 `topP` 参数调整输出的随机性和多样性

### 5.3 错误处理

```kotlin
import ai.kastrax.integrations.deepseek.DeepSeekException
import ai.kastrax.integrations.deepseek.deepSeek

try {
    val provider = deepSeek()
    val response = provider.generate(messages)
    println(response.text)
} catch (e: DeepSeekException) {
    println("DeepSeek API 错误: ${e.message}")
} catch (e: Exception) {
    println("其他错误: ${e.message}")
}
```

## 6. 完整示例

以下是一个完整的示例，展示了如何创建一个使用 DeepSeek 的交互式聊天应用：

```kotlin
import ai.kastrax.core.agent.agent
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建一个使用 DeepSeek 的代理
    val myAgent = agent {
        name = "DeepSeek 助手"
        instructions = "你是一个有帮助的助手，可以回答问题和提供信息。"
        
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
        }
    }
    
    println("DeepSeek 聊天示例")
    println("-------------------")
    println("输入问题，输入 'exit' 退出")
    
    val history = mutableListOf<LlmMessage>()
    
    // 添加系统指令
    history.add(LlmMessage(
        role = LlmMessageRole.SYSTEM,
        content = "你是一个有帮助的助手，可以回答问题和提供信息。"
    ))
    
    while (true) {
        print("\n> ")
        val input = readLine() ?: ""
        
        if (input.equals("exit", ignoreCase = true)) {
            break
        }
        
        // 添加用户消息到历史
        history.add(LlmMessage(
            role = LlmMessageRole.USER,
            content = input
        ))
        
        // 使用流式响应
        println("\nDeepSeek 正在思考...")
        print("Assistant: ")
        
        val responseBuilder = StringBuilder()
        
        myAgent.stream(history).collect { chunk ->
            print(chunk)
            responseBuilder.append(chunk)
        }
        
        // 添加助手回复到历史
        history.add(LlmMessage(
            role = LlmMessageRole.ASSISTANT,
            content = responseBuilder.toString()
        ))
    }
}
```

## 7. 故障排除

### 7.1 常见问题

1. **API 密钥错误**
   - 确保 API 密钥正确设置
   - 检查环境变量是否正确配置

2. **模型不可用**
   - 确认所选模型在 DeepSeek 平台上可用
   - 检查账户是否有权限访问该模型

3. **请求超时**
   - 检查网络连接
   - 考虑增加超时设置

### 7.2 调试技巧

启用详细日志记录：

```kotlin
// 在应用启动时配置日志级别
System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
```

## 8. 参考资料

- [DeepSeek 官方文档](https://api-docs.deepseek.com/)
- [DeepSeek API 参考](https://api-docs.deepseek.com/api/deepseek-api)
- [KastraX 文档](https://kastrax.ai/docs)
