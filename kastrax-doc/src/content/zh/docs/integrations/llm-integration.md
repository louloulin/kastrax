# LLM 集成

## 概述

Kastrax 提供了与多种大型语言模型（LLM）的集成，使开发者能够轻松地在应用中使用不同的 LLM 提供商。本文档介绍 Kastrax 的 LLM 集成架构、支持的提供商以及如何配置和使用这些集成。

## LLM 集成架构

Kastrax 的 LLM 集成架构基于以下组件：

1. **LlmProvider 接口**：定义了 LLM 提供者的基本操作
2. **LlmRequest**：表示发送给 LLM 的请求
3. **LlmResponse**：表示从 LLM 接收的响应
4. **LlmMessage**：表示对话中的消息
5. **LlmToolCall**：表示工具调用
6. **LlmUsage**：表示 LLM 使用情况（如令牌数）

### LlmProvider 接口

`LlmProvider` 接口是 Kastrax LLM 集成的核心，定义了与 LLM 交互的基本操作：

```kotlin
interface LlmProvider {
    /**
     * 生成文本响应
     */
    suspend fun generate(request: LlmRequest): LlmResponse
    
    /**
     * 流式生成文本响应
     */
    suspend fun streamGenerate(request: LlmRequest): Flow<LlmStreamResponse>
    
    /**
     * 生成嵌入向量
     */
    suspend fun generateEmbeddings(texts: List<String>): List<List<Float>>
}
```

### LlmRequest

`LlmRequest` 表示发送给 LLM 的请求，包含以下信息：

```kotlin
data class LlmRequest(
    val messages: List<LlmMessage>,
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val topP: Double = 1.0,
    val frequencyPenalty: Double = 0.0,
    val presencePenalty: Double = 0.0,
    val stopSequences: List<String> = emptyList(),
    val tools: List<LlmTool> = emptyList(),
    val toolChoice: ToolChoice = ToolChoice.AUTO,
    val responseFormat: ResponseFormat? = null,
    val seed: Long? = null,
    val metadata: Map<String, String> = emptyMap()
)
```

### LlmMessage

`LlmMessage` 表示对话中的消息：

```kotlin
data class LlmMessage(
    val role: MessageRole,
    val content: String,
    val name: String? = null,
    val toolCalls: List<LlmToolCall> = emptyList(),
    val toolCallId: String? = null
)

enum class MessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}
```

### LlmResponse

`LlmResponse` 表示从 LLM 接收的响应：

```kotlin
data class LlmResponse(
    val text: String,
    val toolCalls: List<LlmToolCall> = emptyList(),
    val usage: LlmUsage? = null,
    val finishReason: String? = null
)
```

## 支持的 LLM 提供商

Kastrax 目前支持以下 LLM 提供商：

1. **DeepSeek**：DeepSeek 的语言模型
2. **OpenAI**：OpenAI 的 GPT 系列模型
3. **Anthropic**：Anthropic 的 Claude 系列模型
4. **Google Gemini**：Google 的 Gemini 系列模型

### DeepSeek 集成

DeepSeek 集成提供了与 DeepSeek 语言模型的连接：

```kotlin
val deepSeekProvider = deepSeek {
    apiKey("your-deepseek-api-key")
    model(DeepSeekModel.DEEPSEEK_CHAT)
    temperature(0.7)
    maxTokens(2000)
    baseUrl("https://api.deepseek.com") // 可选，默认为官方 API 地址
}

val agent = agent {
    name("DeepSeekAgent")
    model = deepSeekProvider
    // 其他配置...
}
```

DeepSeek 支持的模型：

```kotlin
enum class DeepSeekModel(val id: String) {
    DEEPSEEK_CHAT("deepseek-chat"),
    DEEPSEEK_CODER("deepseek-coder"),
    // 其他 DeepSeek 模型...
}
```

### OpenAI 集成

OpenAI 集成提供了与 OpenAI GPT 系列模型的连接：

```kotlin
val openAIProvider = openAI {
    apiKey("your-openai-api-key")
    model(OpenAIModel.GPT_4)
    temperature(0.7)
    maxTokens(2000)
    organization("your-organization-id") // 可选
    baseUrl("https://api.openai.com/v1") // 可选，默认为官方 API 地址
}

val agent = agent {
    name("OpenAIAgent")
    model = openAIProvider
    // 其他配置...
}
```

OpenAI 支持的模型：

```kotlin
enum class OpenAIModel(val id: String) {
    GPT_3_5_TURBO("gpt-3.5-turbo"),
    GPT_4("gpt-4"),
    GPT_4_TURBO("gpt-4-turbo"),
    GPT_4_VISION("gpt-4-vision-preview"),
    // 其他 OpenAI 模型...
}
```

### Anthropic 集成

Anthropic 集成提供了与 Anthropic Claude 系列模型的连接：

```kotlin
val anthropicProvider = anthropic {
    apiKey("your-anthropic-api-key")
    model(AnthropicModel.CLAUDE_3_OPUS)
    temperature(0.7)
    maxTokens(2000)
    baseUrl("https://api.anthropic.com") // 可选，默认为官方 API 地址
}

val agent = agent {
    name("AnthropicAgent")
    model = anthropicProvider
    // 其他配置...
}
```

Anthropic 支持的模型：

```kotlin
enum class AnthropicModel(val id: String) {
    CLAUDE_3_OPUS("claude-3-opus-20240229"),
    CLAUDE_3_SONNET("claude-3-sonnet-20240229"),
    CLAUDE_3_HAIKU("claude-3-haiku-20240307"),
    // 其他 Anthropic 模型...
}
```

### Google Gemini 集成

Google Gemini 集成提供了与 Google Gemini 系列模型的连接：

```kotlin
val geminiProvider = gemini {
    apiKey("your-gemini-api-key")
    model(GeminiModel.GEMINI_PRO)
    temperature(0.7)
    maxTokens(2000)
    baseUrl("https://generativelanguage.googleapis.com") // 可选，默认为官方 API 地址
}

val agent = agent {
    name("GeminiAgent")
    model = geminiProvider
    // 其他配置...
}
```

Gemini 支持的模型：

```kotlin
enum class GeminiModel(val id: String) {
    GEMINI_PRO("gemini-pro"),
    GEMINI_PRO_VISION("gemini-pro-vision"),
    // 其他 Gemini 模型...
}
```

## 使用 LLM 提供商

### 基本使用

使用 LLM 提供商生成文本：

```kotlin
val provider = deepSeek {
    apiKey("your-deepseek-api-key")
    model(DeepSeekModel.DEEPSEEK_CHAT)
}

val request = LlmRequest(
    messages = listOf(
        LlmMessage(
            role = MessageRole.USER,
            content = "你好，请介绍一下自己。"
        )
    ),
    temperature = 0.7,
    maxTokens = 1000
)

val response = runBlocking {
    provider.generate(request)
}

println("响应: ${response.text}")
```

### 流式生成

使用流式生成获取实时响应：

```kotlin
val provider = deepSeek {
    apiKey("your-deepseek-api-key")
    model(DeepSeekModel.DEEPSEEK_CHAT)
}

val request = LlmRequest(
    messages = listOf(
        LlmMessage(
            role = MessageRole.USER,
            content = "请写一个关于人工智能的短文。"
        )
    ),
    temperature = 0.7
)

runBlocking {
    provider.streamGenerate(request).collect { chunk ->
        print(chunk.text)
    }
}
```

### 工具调用

使用工具调用功能：

```kotlin
val provider = deepSeek {
    apiKey("your-deepseek-api-key")
    model(DeepSeekModel.DEEPSEEK_CHAT)
}

val calculatorTool = LlmTool(
    name = "calculator",
    description = "执行数学计算",
    parameters = jsonObject {
        "type" to JsonPrimitive("object")
        "properties" to jsonObject {
            "expression" to jsonObject {
                "type" to JsonPrimitive("string")
                "description" to JsonPrimitive("要计算的数学表达式")
            }
        }
        "required" to jsonArray {
            add(JsonPrimitive("expression"))
        }
    }
)

val request = LlmRequest(
    messages = listOf(
        LlmMessage(
            role = MessageRole.USER,
            content = "计算 (15 * 3) + 27 的结果"
        )
    ),
    tools = listOf(calculatorTool),
    toolChoice = ToolChoice.AUTO
)

val response = runBlocking {
    provider.generate(request)
}

// 处理工具调用
if (response.toolCalls.isNotEmpty()) {
    val toolCall = response.toolCalls.first()
    println("工具: ${toolCall.name}")
    println("参数: ${toolCall.arguments}")
    
    // 执行工具调用
    val result = executeToolCall(toolCall)
    
    // 发送工具结果
    val followUpRequest = LlmRequest(
        messages = listOf(
            LlmMessage(
                role = MessageRole.USER,
                content = "计算 (15 * 3) + 27 的结果"
            ),
            LlmMessage(
                role = MessageRole.ASSISTANT,
                content = "",
                toolCalls = listOf(toolCall)
            ),
            LlmMessage(
                role = MessageRole.TOOL,
                content = result,
                toolCallId = toolCall.id
            )
        ),
        tools = listOf(calculatorTool)
    )
    
    val finalResponse = runBlocking {
        provider.generate(followUpRequest)
    }
    
    println("最终响应: ${finalResponse.text}")
}
```

### 生成嵌入向量

使用 LLM 提供商生成嵌入向量：

```kotlin
val provider = openAI {
    apiKey("your-openai-api-key")
    model(OpenAIModel.TEXT_EMBEDDING_ADA_002)
}

val texts = listOf(
    "人工智能是计算机科学的一个分支",
    "机器学习是人工智能的一个子领域",
    "深度学习是机器学习的一种方法"
)

val embeddings = runBlocking {
    provider.generateEmbeddings(texts)
}

println("嵌入向量数量: ${embeddings.size}")
println("第一个向量维度: ${embeddings.first().size}")
```

## 自定义 LLM 提供商

如果需要集成其他 LLM 提供商，可以实现 `LlmProvider` 接口：

```kotlin
class CustomLlmProvider(
    private val apiKey: String,
    private val modelId: String,
    private val baseUrl: String,
    private val client: HttpClient = HttpClient()
) : LlmProvider {
    
    override suspend fun generate(request: LlmRequest): LlmResponse {
        // 实现生成逻辑
        // 1. 将 LlmRequest 转换为提供商的请求格式
        // 2. 发送 HTTP 请求
        // 3. 解析响应
        // 4. 返回 LlmResponse
    }
    
    override suspend fun streamGenerate(request: LlmRequest): Flow<LlmStreamResponse> {
        // 实现流式生成逻辑
        return flow {
            // 1. 将 LlmRequest 转换为提供商的请求格式
            // 2. 发送 HTTP 请求
            // 3. 解析流式响应
            // 4. 发送 LlmStreamResponse
        }
    }
    
    override suspend fun generateEmbeddings(texts: List<String>): List<List<Float>> {
        // 实现嵌入生成逻辑
        // 1. 将文本转换为提供商的请求格式
        // 2. 发送 HTTP 请求
        // 3. 解析响应
        // 4. 返回嵌入向量
    }
}
```

## 最佳实践

### 1. API 密钥管理

将 API 密钥存储在环境变量或安全的配置管理系统中：

```kotlin
val apiKey = System.getenv("DEEPSEEK_API_KEY")
    ?: throw IllegalStateException("DEEPSEEK_API_KEY environment variable is required")

val provider = deepSeek {
    apiKey(apiKey)
    // 其他配置...
}
```

### 2. 错误处理

处理 LLM 请求中可能出现的错误：

```kotlin
try {
    val response = provider.generate(request)
    println("响应: ${response.text}")
} catch (e: LlmRequestException) {
    println("LLM 请求错误: ${e.message}")
    println("错误代码: ${e.errorCode}")
    println("错误类型: ${e.errorType}")
} catch (e: Exception) {
    println("发生错误: ${e.message}")
}
```

### 3. 模型选择

根据任务需求选择合适的模型：

- 对于简单任务，使用较小的模型（如 GPT-3.5-Turbo）
- 对于复杂任务，使用较大的模型（如 GPT-4、Claude 3 Opus）
- 对于代码相关任务，使用专门的代码模型（如 DeepSeek Coder）

### 4. 参数调优

调整生成参数以获得最佳结果：

- **temperature**：控制随机性，较低值（如 0.2）产生更确定性的输出，较高值（如 0.8）产生更多样化的输出
- **maxTokens**：控制生成的最大令牌数
- **topP**：控制核采样，与 temperature 类似但工作方式不同
- **frequencyPenalty** 和 **presencePenalty**：减少重复

### 5. 缓存

对频繁使用的请求实施缓存，减少 API 调用：

```kotlin
class CachedLlmProvider(
    private val delegate: LlmProvider,
    private val cache: MutableMap<String, LlmResponse> = mutableMapOf()
) : LlmProvider {
    
    override suspend fun generate(request: LlmRequest): LlmResponse {
        val cacheKey = generateCacheKey(request)
        return cache.getOrPut(cacheKey) {
            delegate.generate(request)
        }
    }
    
    // 其他方法实现...
    
    private fun generateCacheKey(request: LlmRequest): String {
        // 生成缓存键
    }
}
```

### 6. 降级策略

实施降级策略，在主要提供商不可用时使用备用提供商：

```kotlin
class FallbackLlmProvider(
    private val primaryProvider: LlmProvider,
    private val fallbackProvider: LlmProvider
) : LlmProvider {
    
    override suspend fun generate(request: LlmRequest): LlmResponse {
        return try {
            primaryProvider.generate(request)
        } catch (e: Exception) {
            logger.warn("Primary provider failed, falling back: ${e.message}")
            fallbackProvider.generate(request)
        }
    }
    
    // 其他方法实现...
}
```

## 总结

Kastrax 提供了与多种 LLM 提供商的集成，包括 DeepSeek、OpenAI、Anthropic 和 Google Gemini。通过统一的 `LlmProvider` 接口，开发者可以轻松地在应用中使用不同的 LLM 提供商，并利用文本生成、流式生成、工具调用和嵌入生成等功能。遵循最佳实践，可以更有效地使用 LLM 集成，提高应用的性能和可靠性。
