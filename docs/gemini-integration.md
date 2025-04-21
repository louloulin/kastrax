# Google Gemini 集成文档

本文档详细介绍了 KastraX 框架中的 Google Gemini 集成功能，包括基本用法、流式响应处理和工具调用支持。

## 1. 概述

Google Gemini 集成是 KastraX 框架的一部分，允许开发者使用 Google 的 Gemini 系列大语言模型进行文本生成、流式响应、工具调用和嵌入生成。该集成提供了以下核心功能：

- Gemini 模型支持（包括 Gemini 1.5 Pro、Flash 和 Gemini 1.0 系列）
- 流式响应处理
- 工具调用支持
- 文本嵌入生成

## 2. 基本用法

### 2.1 创建 Gemini 提供商

可以通过两种方式创建 Gemini 提供商：

#### 简单创建方式

```kotlin
val provider = gemini(
    model = "gemini-1.5-pro",
    apiKey = "your-api-key"
)
```

#### DSL 创建方式

```kotlin
val provider = gemini {
    model(GeminiModel.GEMINI_1_5_PRO)
    apiKey("your-api-key")
    useEnhancedStreaming(true) // 启用增强的流式处理
    embeddingModel("models/embedding-001") // 设置嵌入模型
}
```

### 2.2 在代理中使用 Gemini

```kotlin
val myAgent = agent {
    name = "Gemini 助手"
    instructions = "你是一个有帮助的助手，可以回答问题。"
    
    // 使用 Gemini 模型
    model = gemini {
        model(GeminiModel.GEMINI_1_5_PRO)
        apiKey("your-api-key")
    }
}

// 使用代理生成回答
val response = myAgent.generate("你好，请介绍一下自己。")
println(response.content)
```

## 3. 流式响应处理

Gemini 集成提供了两种流式响应处理方式：通过代理层和直接使用 GeminiStreamingClient。

### 3.1 通过代理层使用流式响应

```kotlin
val myAgent = agent {
    name = "Gemini 流式助手"
    instructions = "你是一个有帮助的助手，可以回答问题。"
    
    // 使用 Gemini 模型
    model = gemini {
        model(GeminiModel.GEMINI_1_5_PRO)
        apiKey("your-api-key")
        useEnhancedStreaming(true) // 启用增强的流式处理
    }
}

// 使用流式响应
val response = myAgent.stream("请解释量子力学的基本原理")
response.textStream?.collect { chunk ->
    print(chunk)
    System.out.flush() // 立即刷新输出缓冲区，确保实时显示
}
```

### 3.2 直接使用 GeminiStreamingClient

```kotlin
// 创建流式客户端
val client = GeminiStreamingClient("https://generativelanguage.googleapis.com/v1", "your-api-key")

// 创建请求
val request = GeminiChatRequest(
    contents = listOf(
        GeminiContent(
            role = "user",
            parts = listOf(
                GeminiPart(
                    text = "请解释量子力学的基本原理"
                )
            )
        )
    ),
    system = "你是一个有帮助的助手。"
)

// 使用流式响应
client.createChatCompletionStream(GeminiModel.GEMINI_1_5_PRO.id, request).collect { chunk ->
    when (chunk) {
        is GeminiStreamChunk.Content -> {
            print(chunk.text)
            System.out.flush() // 立即刷新输出缓冲区
        }
        is GeminiStreamChunk.Finished -> {
            println("\n(完成原因: ${chunk.reason})")
        }
        is GeminiStreamChunk.Done -> {
            println("\n-------------------")
        }
    }
}
```

## 4. 工具调用支持

Gemini 集成支持工具调用，允许模型使用预定义的工具执行特定任务。

### 4.1 创建工具

```kotlin
val calculatorTool = tool {
    id = "calculator"
    name = "计算器"
    description = "执行数学计算"

    // 定义输入模式
    inputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("expression") {
                put("type", "string")
                put("description", "要计算的数学表达式")
            }
        }
        putJsonArray("required") {
            add("expression")
        }
    }

    // 定义输出模式
    outputSchema = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("result") {
                put("type", "number")
                put("description", "计算结果")
            }
        }
    }

    // 实现执行逻辑
    execute = { input ->
        val expression = input.jsonObject["expression"]?.jsonPrimitive?.content ?: "0"
        val result = evaluateExpression(expression)

        buildJsonObject {
            put("result", result)
        }
    }
}
```

### 4.2 在代理中使用工具

```kotlin
val myAgent = agent {
    name = "Gemini 工具助手"
    instructions = "你是一个有帮助的助手，可以回答问题和执行计算。"
    
    // 使用 Gemini 模型
    model = gemini {
        model(GeminiModel.GEMINI_1_5_PRO)
        apiKey("your-api-key")
    }
    
    // 添加工具
    tools {
        tool(calculatorTool)
    }
}

// 使用代理生成回答
val response = myAgent.generate("计算 25 + 17 的结果")
println(response.content)
```

## 5. 嵌入生成

Gemini 集成支持生成文本嵌入，可用于语义搜索、文本相似度计算等任务。

```kotlin
val provider = gemini {
    model(GeminiModel.GEMINI_1_5_PRO)
    apiKey("your-api-key")
    embeddingModel("models/embedding-001") // 设置嵌入模型
}

// 生成嵌入
val embedding = provider.embedText("这是一段需要嵌入的文本")
println("嵌入维度: ${embedding.size}")
```

## 6. 高级配置

### 6.1 超时设置

可以为 Gemini 客户端设置超时时间，以处理长时间运行的请求：

```kotlin
val client = GeminiClient(
    apiKey = "your-api-key",
    timeout = 120000 // 120秒超时
)
```

### 6.2 错误处理

在使用 Gemini 集成时，应该始终包含适当的错误处理：

```kotlin
try {
    val response = myAgent.stream("请解释量子力学的基本原理")
    response.textStream?.collect { chunk ->
        print(chunk)
        System.out.flush()
    }
} catch (e: Exception) {
    println("\n流式响应出错: ${e.message}")
    e.printStackTrace()
}
```

## 7. 最佳实践

1. **API 密钥安全**：不要在代码中硬编码 API 密钥，应该使用环境变量或安全的配置管理系统。

2. **流式响应优化**：在处理流式响应时，确保立即刷新输出缓冲区，以获得最佳的实时体验。

3. **UTF-8 编码**：在处理中文等多字节字符时，确保使用 UTF-8 编码：
   ```kotlin
   System.setProperty("file.encoding", "UTF-8")
   System.setProperty("sun.jnu.encoding", "UTF-8")
   ```

4. **工具定义**：为工具提供清晰的描述和模式定义，以便模型能够正确理解和使用它们。

5. **超时设置**：对于复杂的请求，考虑增加超时时间，以避免请求被过早终止。

## 8. 示例

完整的示例可以在 `examples` 目录中找到：

- `GeminiStreamingExample.kt`：展示如何通过代理层使用流式响应
- `GeminiDirectStreamingExample.kt`：展示如何直接使用 GeminiStreamingClient

## 9. 故障排除

### 9.1 常见问题

1. **API 密钥格式错误**：确保 API 密钥格式正确，不包含多余的空格或特殊字符。
   ```kotlin
   apiKey("your-api-key".trim())
   ```

2. **请求超时**：对于复杂的请求，可能需要增加超时时间：
   ```kotlin
   val client = GeminiClient(apiKey = "your-api-key", timeout = 120000)
   ```

3. **中文显示问题**：确保使用 UTF-8 编码处理所有文本：
   ```kotlin
   val utf8Text = String(text.toByteArray(Charsets.UTF_8), Charsets.UTF_8)
   ```

### 9.2 调试技巧

1. 启用详细日志记录，以便更好地理解请求和响应：
   ```kotlin
   // 配置日志级别
   System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
   ```

2. 使用 `GeminiStreamChunk` 的不同类型来跟踪流式响应的状态：
   ```kotlin
   when (chunk) {
       is GeminiStreamChunk.Content -> { /* 处理内容 */ }
       is GeminiStreamChunk.Finished -> { /* 处理完成 */ }
       is GeminiStreamChunk.Done -> { /* 处理结束 */ }
   }
   ```

## 10. 参考

- [Google Gemini API 文档](https://ai.google.dev/docs/gemini_api)
- [KastraX 框架文档](https://kastrax.ai/docs)
