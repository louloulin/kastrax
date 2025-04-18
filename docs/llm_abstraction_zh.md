# KastraX LLM 抽象层详解

LLM 抽象层是 KastraX 框架的核心组件之一，它提供了统一的接口来与不同的大型语言模型（LLM）提供商交互。本文档详细介绍了如何使用和扩展 LLM 抽象层。

## 1. LLM 抽象层概述

KastraX 的 LLM 抽象层具有以下特点：

- **统一接口**：提供统一的接口来与不同的 LLM 提供商交互
- **多提供商支持**：支持 OpenAI、Anthropic、Google Gemini 等多个提供商
- **流式响应**：支持流式生成，实时返回生成内容
- **嵌入支持**：提供文本嵌入功能，用于语义搜索和相似度计算
- **错误处理**：统一的错误处理机制，简化异常情况的处理

## 2. 核心接口

LLM 抽象层的核心是 `LlmProvider` 接口：

```kotlin
interface LlmProvider {
    /**
     * 模型标识符
     */
    val model: String
    
    /**
     * 从 LLM 生成响应
     *
     * @param messages 发送给 LLM 的消息列表
     * @param options 生成选项
     * @return LLM 响应
     */
    suspend fun generate(
        messages: List<LlmMessage>,
        options: LlmOptions = LlmOptions()
    ): LlmResponse
    
    /**
     * 从 LLM 生成流式响应
     *
     * @param messages 发送给 LLM 的消息列表
     * @param options 生成选项
     * @return 文本块流
     */
    suspend fun streamGenerate(
        messages: List<LlmMessage>,
        options: LlmOptions = LlmOptions()
    ): Flow<String>
    
    /**
     * 生成文本的嵌入向量
     *
     * @param text 要嵌入的文本
     * @return 嵌入值列表
     */
    suspend fun embedText(text: String): List<Float>
}
```

## 3. 消息和响应

### 3.1 LLM 消息

LLM 消息表示发送给模型的输入：

```kotlin
/**
 * LLM 消息角色枚举
 */
enum class LlmMessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

/**
 * LLM 消息数据类
 *
 * @property role 消息发送者的角色
 * @property content 消息内容
 * @property name 消息发送者的可选名称
 * @property toolCalls 消息中的可选工具调用
 * @property toolCallId 如果这是工具响应，则为可选工具调用 ID
 */
data class LlmMessage(
    val role: LlmMessageRole,
    val content: String,
    val name: String? = null,
    val toolCalls: List<LlmToolCall> = emptyList(),
    val toolCallId: String? = null
)
```

### 3.2 LLM 响应

LLM 响应表示模型生成的输出：

```kotlin
/**
 * LLM 响应数据类
 *
 * @property content 生成的文本内容
 * @property toolCalls 响应中的工具调用列表
 * @property usage 令牌使用信息
 * @property finishReason LLM 停止生成的原因
 */
data class LlmResponse(
    val content: String,
    val toolCalls: List<LlmToolCall> = emptyList(),
    val usage: LlmUsage? = null,
    val finishReason: String? = null
)
```

### 3.3 生成选项

生成选项允许您自定义 LLM 的行为：

```kotlin
/**
 * LLM 生成选项数据类
 *
 * @property temperature 控制随机性（0.0 到 1.0）
 * @property maxTokens 生成的最大令牌数
 * @property topP 通过核采样控制多样性
 * @property frequencyPenalty 惩罚频繁出现的令牌
 * @property presencePenalty 惩罚重复出现的令牌
 * @property stop LLM 应停止生成的序列列表
 * @property tools LLM 可用的工具列表
 * @property toolChoice 模型应如何使用工具
 */
data class LlmOptions(
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val topP: Double = 1.0,
    val frequencyPenalty: Double = 0.0,
    val presencePenalty: Double = 0.0,
    val stop: List<String> = emptyList(),
    val tools: List<JsonElement> = emptyList(),
    val toolChoice: String = "auto"
)
```

## 4. 使用 OpenAI 提供商

KastraX 提供了 OpenAI 提供商的实现：

```kotlin
import ai.kastrax.integrations.openai.openAi

// 使用环境变量中的 API 密钥
val provider = openAi(
    model = "gpt-4o"
)

// 或者直接提供 API 密钥
val provider = openAi(
    apiKey = "your-api-key",
    model = "gpt-3.5-turbo"
)

// 自定义基础 URL（例如，对于 Azure OpenAI）
val provider = openAi(
    apiKey = "your-api-key",
    model = "gpt-4",
    baseUrl = "https://your-resource-name.openai.azure.com/openai/deployments/your-deployment-name"
)
```

### 4.1 生成文本

```kotlin
// 创建消息
val messages = listOf(
    LlmMessage(role = LlmMessageRole.SYSTEM, content = "你是一个有帮助的助手。"),
    LlmMessage(role = LlmMessageRole.USER, content = "你好，请告诉我关于人工智能的信息。")
)

// 生成响应
val response = provider.generate(messages)
println(response.content)

// 使用自定义选项
val options = LlmOptions(
    temperature = 0.5,
    maxTokens = 500,
    stop = listOf("。", "！")
)
val response = provider.generate(messages, options)
```

### 4.2 流式生成

```kotlin
// 流式生成
val messageFlow = provider.streamGenerate(messages)

// 收集流
messageFlow.collect { chunk ->
    print(chunk) // 实时打印每个文本块
}
```

### 4.3 生成嵌入

```kotlin
// 生成文本嵌入
val text = "这是一个示例文本，用于生成嵌入向量。"
val embedding = provider.embedText(text)

// 使用嵌入进行相似度计算
fun cosineSimilarity(a: List<Float>, b: List<Float>): Float {
    val dotProduct = a.zip(b).sumOf { (x, y) -> x * y.toDouble() }
    val normA = sqrt(a.sumOf { it * it.toDouble() })
    val normB = sqrt(b.sumOf { it * it.toDouble() })
    return (dotProduct / (normA * normB)).toFloat()
}

val similarity = cosineSimilarity(embedding1, embedding2)
```

## 5. 其他提供商（即将推出）

### 5.1 Anthropic 提供商

```kotlin
import ai.kastrax.integrations.anthropic.anthropic

val provider = anthropic(
    apiKey = "your-api-key",
    model = "claude-3-opus-20240229"
)
```

### 5.2 Google Gemini 提供商

```kotlin
import ai.kastrax.integrations.gemini.gemini

val provider = gemini(
    apiKey = "your-api-key",
    model = "gemini-pro"
)
```

### 5.3 Mistral 提供商

```kotlin
import ai.kastrax.integrations.mistral.mistral

val provider = mistral(
    apiKey = "your-api-key",
    model = "mistral-large-latest"
)
```

## 6. 创建自定义提供商

您可以通过实现 `LlmProvider` 接口来创建自定义提供商：

```kotlin
class CustomLlmProvider(
    override val model: String,
    private val apiKey: String,
    private val baseUrl: String
) : LlmProvider {
    
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json()
        }
    }
    
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): LlmResponse {
        // 实现生成逻辑
        // 1. 将 KastraX 消息转换为提供商特定格式
        // 2. 调用提供商 API
        // 3. 将响应转换为 KastraX LlmResponse
    }
    
    override suspend fun streamGenerate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): Flow<String> = flow {
        // 实现流式生成逻辑
        // 1. 将 KastraX 消息转换为提供商特定格式
        // 2. 调用提供商流式 API
        // 3. 将响应流转换为文本块流
    }
    
    override suspend fun embedText(text: String): List<Float> {
        // 实现嵌入逻辑
        // 1. 调用提供商嵌入 API
        // 2. 将响应转换为浮点数列表
    }
}
```

## 7. 错误处理

处理 LLM API 调用中的错误是很重要的：

```kotlin
try {
    val response = provider.generate(messages)
    println(response.content)
} catch (e: Exception) {
    when (e) {
        is HttpRequestTimeoutException -> {
            println("请求超时，请稍后重试")
        }
        is ClientRequestException -> {
            println("客户端请求错误: ${e.message}")
        }
        is ServerResponseException -> {
            println("服务器响应错误: ${e.message}")
        }
        else -> {
            println("未知错误: ${e.message}")
        }
    }
}
```

## 8. 高级用例

### 8.1 模型回退

实现模型回退机制，在首选模型失败时尝试备用模型：

```kotlin
class FallbackLlmProvider(
    private val primaryProvider: LlmProvider,
    private val fallbackProvider: LlmProvider
) : LlmProvider {
    
    override val model: String = primaryProvider.model
    
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): LlmResponse {
        return try {
            primaryProvider.generate(messages, options)
        } catch (e: Exception) {
            println("主要提供商失败，回退到备用提供商: ${e.message}")
            fallbackProvider.generate(messages, options)
        }
    }
    
    override suspend fun streamGenerate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): Flow<String> = flow {
        try {
            primaryProvider.streamGenerate(messages, options).collect { emit(it) }
        } catch (e: Exception) {
            println("主要提供商失败，回退到备用提供商: ${e.message}")
            fallbackProvider.streamGenerate(messages, options).collect { emit(it) }
        }
    }
    
    override suspend fun embedText(text: String): List<Float> {
        return try {
            primaryProvider.embedText(text)
        } catch (e: Exception) {
            println("主要提供商失败，回退到备用提供商: ${e.message}")
            fallbackProvider.embedText(text)
        }
    }
}

// 使用回退提供商
val primary = openAi("gpt-4o")
val fallback = openAi("gpt-3.5-turbo")
val provider = FallbackLlmProvider(primary, fallback)
```

### 8.2 缓存响应

实现响应缓存，减少 API 调用：

```kotlin
class CachingLlmProvider(
    private val delegate: LlmProvider,
    private val cache: MutableMap<String, LlmResponse> = mutableMapOf()
) : LlmProvider {
    
    override val model: String = delegate.model
    
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): LlmResponse {
        val cacheKey = generateCacheKey(messages, options)
        return cache[cacheKey] ?: delegate.generate(messages, options).also {
            cache[cacheKey] = it
        }
    }
    
    override suspend fun streamGenerate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): Flow<String> {
        // 流式生成通常不缓存
        return delegate.streamGenerate(messages, options)
    }
    
    override suspend fun embedText(text: String): List<Float> {
        val cacheKey = "embed:$text"
        return cache[cacheKey]?.let {
            it.content.split(",").map { it.toFloat() }
        } ?: delegate.embedText(text).also {
            cache[cacheKey] = LlmResponse(it.joinToString(","))
        }
    }
    
    private fun generateCacheKey(messages: List<LlmMessage>, options: LlmOptions): String {
        val messagesStr = messages.joinToString("|") { "${it.role}:${it.content}" }
        val optionsStr = "${options.temperature}:${options.maxTokens}:${options.topP}"
        return "gen:$messagesStr:$optionsStr"
    }
}

// 使用缓存提供商
val delegate = openAi("gpt-4o")
val provider = CachingLlmProvider(delegate)
```

### 8.3 负载均衡

在多个提供商之间实现负载均衡：

```kotlin
class LoadBalancingLlmProvider(
    private val providers: List<LlmProvider>
) : LlmProvider {
    
    private var currentIndex = 0
    private val mutex = Mutex()
    
    override val model: String = providers.first().model
    
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): LlmResponse {
        val provider = getNextProvider()
        return provider.generate(messages, options)
    }
    
    override suspend fun streamGenerate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): Flow<String> {
        val provider = getNextProvider()
        return provider.streamGenerate(messages, options)
    }
    
    override suspend fun embedText(text: String): List<Float> {
        val provider = getNextProvider()
        return provider.embedText(text)
    }
    
    private suspend fun getNextProvider(): LlmProvider {
        mutex.withLock {
            val provider = providers[currentIndex]
            currentIndex = (currentIndex + 1) % providers.size
            return provider
        }
    }
}

// 使用负载均衡提供商
val providers = listOf(
    openAi("gpt-4o", apiKey = "key1"),
    openAi("gpt-4o", apiKey = "key2"),
    openAi("gpt-4o", apiKey = "key3")
)
val provider = LoadBalancingLlmProvider(providers)
```

## 9. 最佳实践

1. **错误处理**：实施适当的错误处理机制，处理 API 超时、限速和其他常见错误
2. **重试机制**：对暂时性错误实施重试机制，使用指数退避策略
3. **超时设置**：设置适当的超时，防止请求挂起
4. **模型选择**：根据任务复杂性和预算选择合适的模型
5. **缓存策略**：对于重复查询，实施缓存策略以减少 API 调用
6. **监控使用情况**：监控 API 使用情况和成本
7. **安全考虑**：安全存储 API 密钥，避免在代码中硬编码

## 10. 性能优化

1. **批处理请求**：尽可能批处理请求，减少 API 调用次数
2. **流式处理**：对于长响应，使用流式处理减少等待时间
3. **并发请求**：使用协程并发处理多个请求
4. **连接池**：使用 HTTP 客户端连接池减少连接建立开销
5. **压缩**：启用请求和响应压缩，减少数据传输量
