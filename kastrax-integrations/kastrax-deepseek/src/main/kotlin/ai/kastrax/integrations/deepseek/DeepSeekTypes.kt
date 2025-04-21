package ai.kastrax.integrations.deepseek

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

/**
 * DeepSeek 聊天完成请求。
 */
@Serializable
data class DeepSeekChatCompletionRequest(
    /** 要使用的模型 ID */
    val model: String,
    /** 对话消息列表 */
    val messages: List<DeepSeekMessage>,
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
    /** 频率惩罚参数 */
    @SerialName("frequency_penalty") val frequencyPenalty: Double? = null,
    /** 存在惩罚参数 */
    @SerialName("presence_penalty") val presencePenalty: Double? = null,
    /** 工具定义列表 */
    val tools: List<DeepSeekTool>? = null,
    /** 工具选择策略 */
    @SerialName("tool_choice") val toolChoice: String? = null,
    /** 用户标识符 */
    val user: String? = null
)

/**
 * DeepSeek 消息。
 */
@Serializable
data class DeepSeekMessage(
    /** 消息角色（在流式响应的 delta 中可能不存在） */
    val role: String? = null,
    /** 消息内容 */
    val content: String? = null,
    /** 消息名称 */
    val name: String? = null,
    /** 工具调用列表 */
    @SerialName("tool_calls") val toolCalls: List<DeepSeekToolCall>? = null,
    /** 工具调用 ID */
    @SerialName("tool_call_id") val toolCallId: String? = null
)

/**
 * DeepSeek 工具定义。
 */
@Serializable
data class DeepSeekTool(
    /** 工具类型 */
    val type: String,
    /** 工具函数 */
    val function: DeepSeekFunction
)

/**
 * DeepSeek 函数定义。
 */
@Serializable
data class DeepSeekFunction(
    /** 函数名称 */
    val name: String,
    /** 函数描述 */
    val description: String? = null,
    /** 函数参数 */
    val parameters: JsonObject? = null
)

/**
 * DeepSeek 工具调用。
 */
@Serializable
data class DeepSeekToolCall(
    /** 工具调用 ID */
    val id: String,
    /** 工具类型 */
    val type: String,
    /** 调用的函数 */
    val function: DeepSeekFunctionCall
)

/**
 * DeepSeek 函数调用。
 */
@Serializable
data class DeepSeekFunctionCall(
    /** 函数名称 */
    val name: String,
    /** 函数参数 */
    val arguments: String
)

/**
 * DeepSeek 聊天完成响应。
 */
@Serializable
data class DeepSeekChatCompletionResponse(
    /** 响应 ID */
    val id: String,
    /** 对象类型 */
    @SerialName("object") val objectType: String,
    /** 创建时间戳 */
    val created: Long,
    /** 模型 ID */
    val model: String,
    /** 系统指纹 */
    @SerialName("system_fingerprint") val systemFingerprint: String? = null,
    /** 响应选项列表 */
    val choices: List<DeepSeekChoice>,
    /** 使用情况统计 */
    val usage: DeepSeekUsage? = null
)

/**
 * DeepSeek 响应选项。
 */
@Serializable
data class DeepSeekChoice(
    /** 选项索引 */
    val index: Int,
    /** 消息内容 */
    val message: DeepSeekMessage? = null,
    /** 增量内容（用于流式响应） */
    val delta: DeepSeekMessage? = null,
    /** 结束原因 */
    @SerialName("finish_reason") val finishReason: String? = null
)

/**
 * DeepSeek 使用情况统计。
 */
@Serializable
data class DeepSeekUsage(
    /** 提示令牌数 */
    @SerialName("prompt_tokens") val promptTokens: Int,
    /** 完成令牌数 */
    @SerialName("completion_tokens") val completionTokens: Int,
    /** 总令牌数 */
    @SerialName("total_tokens") val totalTokens: Int
)

/**
 * DeepSeek 嵌入请求。
 */
@Serializable
data class DeepSeekEmbeddingRequest(
    /** 要使用的模型 ID */
    val model: String,
    /** 要嵌入的输入文本 */
    val input: List<String>,
    /** 用户标识符 */
    val user: String? = null
)

/**
 * DeepSeek 嵌入响应。
 */
@Serializable
data class DeepSeekEmbeddingResponse(
    /** 对象类型 */
    @SerialName("object") val objectType: String,
    /** 嵌入数据列表 */
    val data: List<DeepSeekEmbedding>,
    /** 模型 ID */
    val model: String,
    /** 使用情况统计 */
    val usage: DeepSeekEmbeddingUsage
)

/**
 * DeepSeek 嵌入数据。
 */
@Serializable
data class DeepSeekEmbedding(
    /** 对象类型 */
    @SerialName("object") val objectType: String,
    /** 嵌入向量 */
    val embedding: List<Float>,
    /** 嵌入索引 */
    val index: Int
)

/**
 * DeepSeek 嵌入使用情况统计。
 */
@Serializable
data class DeepSeekEmbeddingUsage(
    /** 提示令牌数 */
    @SerialName("prompt_tokens") val promptTokens: Int,
    /** 总令牌数 */
    @SerialName("total_tokens") val totalTokens: Int
)

/**
 * DeepSeek 错误响应。
 */
@Serializable
data class DeepSeekErrorResponse(
    /** 错误对象 */
    val error: DeepSeekError
)

/**
 * DeepSeek 错误详情。
 */
@Serializable
data class DeepSeekError(
    /** 错误消息 */
    val message: String,
    /** 错误类型 */
    val type: String,
    /** 错误代码 */
    val code: String? = null,
    /** 参数错误 */
    val param: String? = null
)
