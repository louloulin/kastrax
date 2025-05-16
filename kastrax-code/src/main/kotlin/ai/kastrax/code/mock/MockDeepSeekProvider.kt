package ai.kastrax.code.mock

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext

/**
 * 模拟DeepSeek提供者
 *
 * 用于解决编译错误
 */
class DeepSeekProvider(
    val model: String,
    val apiKey: String,
    val temperature: Double? = null,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val timeout: Long = 60000
) {
    /**
     * 生成文本
     *
     * @param messages 消息列表
     * @param options 生成选项
     * @return 响应
     */
    suspend fun generate(
        messages: List<LlmMessage>,
        options: LlmOptions = LlmOptions()
    ): LlmResponse = withContext(Dispatchers.IO) {
        val prompt = messages.joinToString("\n") { "${it.role}: ${it.content}" }
        return@withContext LlmResponse(
            content = "模拟DeepSeek响应: $prompt",
            usage = LlmUsage(
                promptTokens = prompt.length,
                completionTokens = 100,
                totalTokens = prompt.length + 100
            )
        )
    }
    
    /**
     * 流式生成文本
     *
     * @param messages 消息列表
     * @param options 生成选项
     * @return 响应流
     */
    suspend fun streamGenerate(
        messages: List<LlmMessage>,
        options: LlmOptions = LlmOptions()
    ): Flow<String> = flow {
        val prompt = messages.joinToString("\n") { "${it.role}: ${it.content}" }
        val response = "模拟DeepSeek响应: $prompt"
        response.chunked(10).forEach { chunk ->
            emit(chunk)
        }
    }
}

/**
 * 模拟DeepSeek模型
 */
enum class DeepSeekModel(val id: String) {
    DEEPSEEK_CHAT("deepseek-chat"),
    DEEPSEEK_CODER("deepseek-coder")
}

/**
 * 模拟DeepSeek配置
 */
class DeepSeekConfig {
    var model: String = DeepSeekModel.DEEPSEEK_CHAT.id
    var apiKey: String = ""
    var temperature: Double? = null
    var maxTokens: Int? = null
    var topP: Double? = null
    var timeout: Long = 60000
    
    fun model(model: DeepSeekModel) {
        this.model = model.id
    }
    
    fun model(modelId: String) {
        this.model = modelId
    }
    
    fun apiKey(apiKey: String) {
        this.apiKey = apiKey
    }
    
    fun temperature(temperature: Double) {
        this.temperature = temperature
    }
    
    fun maxTokens(maxTokens: Int) {
        this.maxTokens = maxTokens
    }
    
    fun topP(topP: Double) {
        this.topP = topP
    }
}

/**
 * 创建DeepSeek提供者
 */
fun deepSeek(init: DeepSeekConfig.() -> Unit): DeepSeekProvider {
    val config = DeepSeekConfig().apply(init)
    
    val apiKey = if (config.apiKey.isBlank()) {
        System.getenv("DEEPSEEK_API_KEY") ?: "mock-api-key"
    } else {
        config.apiKey
    }
    
    return DeepSeekProvider(
        model = config.model,
        apiKey = apiKey,
        temperature = config.temperature,
        maxTokens = config.maxTokens,
        topP = config.topP,
        timeout = config.timeout
    )
}

/**
 * 模拟LLM消息角色
 */
enum class LlmMessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

/**
 * 模拟LLM消息
 */
data class LlmMessage(
    val role: LlmMessageRole,
    val content: String,
    val name: String? = null,
    val toolCalls: List<LlmToolCall> = emptyList(),
    val toolCallId: String? = null
)

/**
 * 模拟LLM工具调用
 */
data class LlmToolCall(
    val id: String,
    val name: String,
    val arguments: String
)

/**
 * 模拟LLM选项
 */
data class LlmOptions(
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val stop: List<String> = emptyList(),
    val tools: List<Any> = emptyList(),
    val toolChoice: Any = "auto"
)

/**
 * 模拟LLM响应
 */
data class LlmResponse(
    val content: String,
    val toolCalls: List<LlmToolCall> = emptyList(),
    val usage: LlmUsage? = null,
    val finishReason: String? = null
)

/**
 * 模拟LLM使用情况
 */
data class LlmUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)
