# DeepSeek 集成文档

本文档详细介绍了 KastraX 框架中的 DeepSeek 集成功能，包括基本用法、流式响应处理和工具调用支持。

## 1. 概述

DeepSeek 集成是 KastraX 框架的一部分，允许开发者使用 DeepSeek 的大语言模型进行文本生成、流式响应和工具调用。该集成提供了以下核心功能：

- DeepSeek 模型支持
- 流式响应处理
- 工具调用支持

## 2. 基本用法

### 2.1 创建 DeepSeek 提供商

可以通过两种方式创建 DeepSeek 提供商：

#### 简单创建方式

```kotlin
val provider = deepSeek(
    model = "deepseek-chat",
    apiKey = "your-api-key"
)
```

#### DSL 创建方式

```kotlin
val provider = deepSeek {
    model("deepseek-chat")
    apiKey("your-api-key")
    useEnhancedStreaming(true) // 启用增强的流式处理
}
```

### 2.2 在代理中使用 DeepSeek

```kotlin
val myAgent = agent {
    name = "DeepSeek 助手"
    instructions = "你是一个有帮助的助手，可以回答问题。"
    
    // 使用 DeepSeek 模型
    model = deepSeek {
        model("deepseek-chat")
        apiKey("your-api-key")
    }
}

// 使用代理生成回答
val response = myAgent.generate("你好，请介绍一下自己。")
println(response.content)
```

## 3. 流式响应处理

DeepSeek 集成提供了两种流式响应处理方式：通过代理层和直接使用 DeepSeekStreamingClient。

### 3.1 通过代理层使用流式响应

```kotlin
val myAgent = agent {
    name = "DeepSeek 流式助手"
    instructions = "你是一个有帮助的助手，可以回答问题。"
    
    // 使用 DeepSeek 模型
    model = deepSeek {
        model("deepseek-chat")
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

### 3.2 直接使用 DeepSeekStreamingClient

```kotlin
// 创建流式客户端
val client = DeepSeekStreamingClient("https://api.deepseek.com/v1", "your-api-key")

// 创建请求
val request = DeepSeekChatCompletionRequest(
    model = "deepseek-chat",
    messages = listOf(
        DeepSeekMessage(
            role = "system",
            content = "你是一个有帮助的助手。"
        ),
        DeepSeekMessage(
            role = "user",
            content = "请解释量子力学的基本原理"
        )
    ),
    stream = true
)

// 使用流式响应
client.createChatCompletionStream(request).collect { chunk ->
    when (chunk) {
        is DeepSeekStreamChunk.Content -> {
            print(chunk.text)
            System.out.flush() // 立即刷新输出缓冲区
        }
        is DeepSeekStreamChunk.Finished -> {
            println("\n(完成原因: ${chunk.reason})")
        }
        is DeepSeekStreamChunk.Done -> {
            println("\n-------------------")
        }
    }
}
```

## 4. 工具调用支持

DeepSeek 集成支持工具调用，允许模型使用预定义的工具执行特定任务。

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
    name = "DeepSeek 工具助手"
    instructions = "你是一个有帮助的助手，可以回答问题和执行计算。"
    
    // 使用 DeepSeek 模型
    model = deepSeek {
        model("deepseek-chat")
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

## 5. 高级配置

### 5.1 超时设置

可以为 DeepSeek 客户端设置超时时间，以处理长时间运行的请求：

```kotlin
val client = DeepSeekStreamingClient(
    baseUrl = "https://api.deepseek.com/v1",
    apiKey = "your-api-key",
    timeout = 120000 // 120秒超时
)
```

### 5.2 错误处理

在使用 DeepSeek 集成时，应该始终包含适当的错误处理：

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

## 6. 最佳实践

1. **API 密钥安全**：不要在代码中硬编码 API 密钥，应该使用环境变量或安全的配置管理系统。

2. **流式响应优化**：在处理流式响应时，确保立即刷新输出缓冲区，以获得最佳的实时体验。

3. **UTF-8 编码**：在处理中文等多字节字符时，确保使用 UTF-8 编码：
   ```kotlin
   System.setProperty("file.encoding", "UTF-8")
   System.setProperty("sun.jnu.encoding", "UTF-8")
   ```

4. **工具定义**：为工具提供清晰的描述和模式定义，以便模型能够正确理解和使用它们。

5. **超时设置**：对于复杂的请求，考虑增加超时时间，以避免请求被过早终止。

## 7. 示例

完整的示例可以在 `examples` 目录中找到：

- `DeepSeekStreamingExample.kt`：展示如何通过代理层使用流式响应
- `DeepSeekDirectStreamingExample.kt`：展示如何直接使用 DeepSeekStreamingClient
- `DeepSeekExample.kt`：展示基本的 DeepSeek 使用方式

## 8. 故障排除

### 8.1 常见问题

1. **API 密钥格式错误**：确保 API 密钥格式正确，不包含多余的空格或特殊字符。
   ```kotlin
   apiKey("your-api-key".trim())
   ```

2. **请求超时**：对于复杂的请求，可能需要增加超时时间：
   ```kotlin
   val client = DeepSeekStreamingClient(baseUrl, apiKey, timeout = 120000)
   ```

3. **中文显示问题**：确保使用 UTF-8 编码处理所有文本：
   ```kotlin
   val utf8Text = String(text.toByteArray(Charsets.UTF_8), Charsets.UTF_8)
   ```

### 8.2 调试技巧

1. 启用详细日志记录，以便更好地理解请求和响应：
   ```kotlin
   // 配置日志级别
   System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "DEBUG")
   ```

2. 使用 `DeepSeekStreamChunk` 的不同类型来跟踪流式响应的状态：
   ```kotlin
   when (chunk) {
       is DeepSeekStreamChunk.Content -> { /* 处理内容 */ }
       is DeepSeekStreamChunk.Finished -> { /* 处理完成 */ }
       is DeepSeekStreamChunk.Done -> { /* 处理结束 */ }
   }
   ```

## 9. 参考

- [DeepSeek API 文档](https://platform.deepseek.com/docs)
- [KastraX 框架文档](https://kastrax.ai/docs)
