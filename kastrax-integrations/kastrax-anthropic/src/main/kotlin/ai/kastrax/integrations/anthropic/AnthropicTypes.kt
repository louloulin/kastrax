package ai.kastrax.integrations.anthropic

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Anthropic 消息类型
 */
@Serializable
data class AnthropicMessage(
    val role: String,
    val content: List<AnthropicContent>? = null,
    val text: String? = null
)

/**
 * Anthropic 内容类型
 */
@Serializable
data class AnthropicContent(
    val type: String,
    val text: String? = null,
    val source: AnthropicSource? = null,
    @SerialName("tool_use") val toolUse: AnthropicToolUse? = null
)

/**
 * Anthropic 源类型
 */
@Serializable
data class AnthropicSource(
    val type: String,
    @SerialName("media_type") val mediaType: String? = null,
    val data: String? = null
)

/**
 * Anthropic 工具使用类型
 */
@Serializable
data class AnthropicToolUse(
    val id: String,
    val name: String,
    val input: JsonElement
)

/**
 * Anthropic 工具类型
 */
@Serializable
data class AnthropicTool(
    val name: String,
    val description: String? = null,
    @SerialName("input_schema") val inputSchema: JsonElement
)

/**
 * Anthropic 聊天请求
 */
@Serializable
data class AnthropicChatRequest(
    val model: String,
    val messages: List<AnthropicMessage>,
    val system: String? = null,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("top_k") val topK: Int? = null,
    val tools: List<AnthropicTool>? = null,
    @SerialName("tool_choice") val toolChoice: String? = null,
    val stream: Boolean = false,
    @SerialName("stop_sequences") val stopSequences: List<String>? = null,
    val metadata: Map<String, String>? = null
)

/**
 * Anthropic 聊天响应
 */
@Serializable
data class AnthropicChatResponse(
    val id: String,
    val model: String,
    val type: String,
    @SerialName("role") val role: String,
    val content: List<AnthropicContent>,
    val usage: AnthropicUsage,
    @SerialName("stop_reason") val stopReason: String? = null,
    @SerialName("stop_sequence") val stopSequence: String? = null
)

/**
 * Anthropic 使用统计
 */
@Serializable
data class AnthropicUsage(
    @SerialName("input_tokens") val inputTokens: Int,
    @SerialName("output_tokens") val outputTokens: Int
)

/**
 * Anthropic 流式响应块
 */
@Serializable
data class AnthropicStreamResponse(
    val type: String,
    val message: AnthropicStreamMessage? = null,
    val delta: AnthropicDelta? = null,
    val usage: AnthropicUsage? = null,
    @SerialName("stop_reason") val stopReason: String? = null,
    @SerialName("stop_sequence") val stopSequence: String? = null
)

/**
 * Anthropic 流式消息
 */
@Serializable
data class AnthropicStreamMessage(
    val id: String,
    val type: String,
    val role: String,
    val content: List<AnthropicContent>,
    val model: String,
    @SerialName("stop_reason") val stopReason: String? = null,
    @SerialName("stop_sequence") val stopSequence: String? = null,
    val usage: AnthropicUsage? = null
)

/**
 * Anthropic 增量更新
 */
@Serializable
data class AnthropicDelta(
    @SerialName("stop_reason") val stopReason: String? = null,
    @SerialName("stop_sequence") val stopSequence: String? = null,
    val text: String? = null,
    val type: String? = null,
    @SerialName("tool_use") val toolUse: AnthropicToolUse? = null
)

/**
 * Anthropic 异常
 */
class AnthropicException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Anthropic 流式块类型
 */
sealed class AnthropicStreamChunk {
    /**
     * 内容块
     */
    data class Content(val text: String) : AnthropicStreamChunk()

    /**
     * 工具使用块
     */
    data class ToolUse(val toolUse: AnthropicToolUse) : AnthropicStreamChunk()

    /**
     * 完成块
     */
    data class Finished(val reason: String?) : AnthropicStreamChunk()

    /**
     * 结束块
     */
    object Done : AnthropicStreamChunk()
}
