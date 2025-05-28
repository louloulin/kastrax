package ai.kastrax.integrations.qwen

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * Qwen 聊天完成请求。
 */
@Serializable
data class QwenChatCompletionRequest(
    /** 要使用的模型 ID */
    val model: String,
    /** 对话消息列表 */
    val messages: List<QwenMessage>,
    /** 采样温度，控制输出的随机性 */
    val temperature: Double? = null,
    /** 核采样参数，用于控制输出的多样性 */
    @SerialName("top_p") val topP: Double? = null,
    /** 生成的最大令牌数 */
    @SerialName("max_tokens") val maxTokens: Int? = null,
    /** 是否流式输出 */
    val stream: Boolean = false,
    /** 停止生成的标记 */
    val stop: List<String>? = null,
    /** 重复惩罚参数 */
    @SerialName("repetition_penalty") val repetitionPenalty: Double? = null,
    /** 工具定义列表 */
    val tools: List<QwenTool>? = null,
    /** 工具选择策略 */
    @SerialName("tool_choice") val toolChoice: String? = null,
    /** 用户标识符 */
    val user: String? = null,
    /** 是否启用搜索 */
    @SerialName("enable_search") val enableSearch: Boolean? = null
)

/**
 * Qwen 消息。
 */
@Serializable
data class QwenMessage(
    /** 消息角色（在流式响应的 delta 中可能不存在） */
    val role: String? = null,
    /** 消息内容 */
    val content: String? = null,
    /** 消息名称 */
    val name: String? = null,
    /** 工具调用列表 */
    @SerialName("tool_calls") val toolCalls: List<QwenToolCall>? = null,
    /** 工具调用 ID */
    @SerialName("tool_call_id") val toolCallId: String? = null
)

/**
 * Qwen 工具调用。
 */
@Serializable
data class QwenToolCall(
    /** 工具调用 ID */
    val id: String,
    /** 工具类型 */
    val type: String,
    /** 函数调用信息 */
    val function: QwenFunctionCall
)

/**
 * Qwen 函数调用。
 */
@Serializable
data class QwenFunctionCall(
    /** 函数名称 */
    val name: String,
    /** 函数参数（JSON 字符串） */
    val arguments: String
)

/**
 * Qwen 工具定义。
 */
@Serializable
data class QwenTool(
    /** 工具类型 */
    val type: String,
    /** 函数定义 */
    val function: QwenFunction
)

/**
 * Qwen 函数定义。
 */
@Serializable
data class QwenFunction(
    /** 函数名称 */
    val name: String,
    /** 函数描述 */
    val description: String? = null,
    /** 函数参数模式 */
    val parameters: JsonObject? = null
)

/**
 * Qwen 聊天完成响应。
 */
@Serializable
data class QwenChatCompletionResponse(
    /** 响应 ID */
    val id: String,
    /** 对象类型 */
    val `object`: String,
    /** 创建时间戳 */
    val created: Long,
    /** 使用的模型 */
    val model: String,
    /** 选择列表 */
    val choices: List<QwenChoice>,
    /** 使用情况统计 */
    val usage: QwenUsage? = null,
    /** 系统指纹 */
    @SerialName("system_fingerprint") val systemFingerprint: String? = null
)

/**
 * Qwen 选择。
 */
@Serializable
data class QwenChoice(
    /** 选择索引 */
    val index: Int,
    /** 消息内容 */
    val message: QwenMessage? = null,
    /** 增量消息（流式响应中使用） */
    val delta: QwenMessage? = null,
    /** 完成原因 */
    @SerialName("finish_reason") val finishReason: String? = null,
    /** 日志概率 */
    val logprobs: JsonObject? = null
)

/**
 * Qwen 使用情况统计。
 */
@Serializable
data class QwenUsage(
    /** 输入令牌数 */
    @SerialName("prompt_tokens") val promptTokens: Int,
    /** 输出令牌数 */
    @SerialName("completion_tokens") val completionTokens: Int,
    /** 总令牌数 */
    @SerialName("total_tokens") val totalTokens: Int
)

/**
 * Qwen 流式响应块。
 */
@Serializable
data class QwenStreamChunk(
    /** 响应 ID */
    val id: String,
    /** 对象类型 */
    val `object`: String,
    /** 创建时间戳 */
    val created: Long,
    /** 使用的模型 */
    val model: String,
    /** 选择列表 */
    val choices: List<QwenChoice>,
    /** 使用情况统计（仅在最后一个块中出现） */
    val usage: QwenUsage? = null
)

/**
 * Qwen 异常。
 */
class QwenException(message: String, cause: Throwable? = null) : Exception(message, cause)