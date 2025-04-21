package ai.kastrax.integrations.gemini

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Gemini 内容部分类型
 */
@Serializable
data class GeminiPart(
    val text: String? = null,
    @SerialName("inline_data") val inlineData: GeminiInlineData? = null,
    @SerialName("file_data") val fileData: GeminiFileData? = null
)

/**
 * Gemini 内联数据类型
 */
@Serializable
data class GeminiInlineData(
    @SerialName("mime_type") val mimeType: String,
    val data: String
)

/**
 * Gemini 文件数据类型
 */
@Serializable
data class GeminiFileData(
    @SerialName("mime_type") val mimeType: String,
    @SerialName("file_uri") val fileUri: String
)

/**
 * Gemini 内容类型
 */
@Serializable
data class GeminiContent(
    val role: String,
    val parts: List<GeminiPart>
)

/**
 * Gemini 工具类型
 */
@Serializable
data class GeminiTool(
    @SerialName("function_declarations") val functionDeclarations: List<GeminiFunctionDeclaration>
)

/**
 * Gemini 函数声明类型
 */
@Serializable
data class GeminiFunctionDeclaration(
    val name: String,
    val description: String? = null,
    val parameters: JsonElement
)

/**
 * Gemini 生成配置类型
 */
@Serializable
data class GeminiGenerationConfig(
    val temperature: Double? = null,
    @SerialName("top_p") val topP: Double? = null,
    @SerialName("top_k") val topK: Int? = null,
    @SerialName("max_output_tokens") val maxOutputTokens: Int? = null,
    @SerialName("stop_sequences") val stopSequences: List<String>? = null,
    @SerialName("response_mime_type") val responseMimeType: String? = null,
    @SerialName("candidate_count") val candidateCount: Int? = null
)

/**
 * Gemini 安全设置类型
 */
@Serializable
data class GeminiSafetySetting(
    val category: String,
    val threshold: String
)

/**
 * Gemini 聊天请求类型
 */
@Serializable
data class GeminiChatRequest(
    val contents: List<GeminiContent>,
    val tools: List<GeminiTool>? = null,
    @SerialName("tool_config") val toolConfig: GeminiToolConfig? = null,
    @SerialName("generation_config") val generationConfig: GeminiGenerationConfig? = null,
    @SerialName("safety_settings") val safetySettings: List<GeminiSafetySetting>? = null,
    val system: String? = null
)

/**
 * Gemini 工具配置类型
 */
@Serializable
data class GeminiToolConfig(
    @SerialName("function_calling_config") val functionCallingConfig: GeminiFunctionCallingConfig? = null
)

/**
 * Gemini 函数调用配置类型
 */
@Serializable
data class GeminiFunctionCallingConfig(
    val mode: String? = null,
    @SerialName("allowed_function_names") val allowedFunctionNames: List<String>? = null
)

/**
 * Gemini 聊天响应类型
 */
@Serializable
data class GeminiChatResponse(
    val candidates: List<GeminiCandidate>,
    val promptFeedback: GeminiPromptFeedback? = null,
    val usage: GeminiUsage? = null
)

/**
 * Gemini 候选项类型
 */
@Serializable
data class GeminiCandidate(
    val content: GeminiContent,
    @SerialName("finish_reason") val finishReason: String? = null,
    val index: Int,
    @SerialName("safety_ratings") val safetyRatings: List<GeminiSafetyRating>? = null,
    @SerialName("citation_metadata") val citationMetadata: GeminiCitationMetadata? = null
)

/**
 * Gemini 安全评级类型
 */
@Serializable
data class GeminiSafetyRating(
    val category: String,
    val probability: String,
    val blocked: Boolean? = null
)

/**
 * Gemini 引用元数据类型
 */
@Serializable
data class GeminiCitationMetadata(
    val citations: List<GeminiCitation>? = null
)

/**
 * Gemini 引用类型
 */
@Serializable
data class GeminiCitation(
    @SerialName("start_index") val startIndex: Int? = null,
    @SerialName("end_index") val endIndex: Int? = null,
    val uri: String? = null,
    val title: String? = null,
    val license: String? = null,
    @SerialName("publication_date") val publicationDate: String? = null
)

/**
 * Gemini 提示反馈类型
 */
@Serializable
data class GeminiPromptFeedback(
    @SerialName("safety_ratings") val safetyRatings: List<GeminiSafetyRating>? = null,
    @SerialName("block_reason") val blockReason: String? = null
)

/**
 * Gemini 使用统计类型
 */
@Serializable
data class GeminiUsage(
    @SerialName("prompt_token_count") val promptTokenCount: Int,
    @SerialName("candidates_token_count") val candidatesTokenCount: Int,
    @SerialName("total_token_count") val totalTokenCount: Int
)

/**
 * Gemini 流式响应类型
 */
@Serializable
data class GeminiStreamResponse(
    val candidates: List<GeminiCandidate>? = null,
    val promptFeedback: GeminiPromptFeedback? = null,
    val usage: GeminiUsage? = null
)

/**
 * Gemini 嵌入请求类型
 */
@Serializable
data class GeminiEmbeddingRequest(
    val model: String,
    val content: GeminiContent,
    @SerialName("task_type") val taskType: String? = null,
    val title: String? = null
)

/**
 * Gemini 嵌入响应类型
 */
@Serializable
data class GeminiEmbeddingResponse(
    val embedding: GeminiEmbedding
)

/**
 * Gemini 嵌入类型
 */
@Serializable
data class GeminiEmbedding(
    val values: List<Float>
)

/**
 * Gemini 异常类型
 */
class GeminiException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Gemini 流式块类型
 */
sealed class GeminiStreamChunk {
    /**
     * 内容块
     */
    data class Content(val text: String) : GeminiStreamChunk()
    
    /**
     * 完成块
     */
    data class Finished(val reason: String?) : GeminiStreamChunk()
    
    /**
     * 结束块
     */
    object Done : GeminiStreamChunk()
}
