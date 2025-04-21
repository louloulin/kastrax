package ai.kastrax.integrations.gemini

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Google Gemini LLM 提供商，实现 KastraX 的 LlmProvider 接口。
 *
 * @property model Gemini 模型
 * @property apiKey Google API 密钥
 * @property client Gemini API 客户端
 * @property streamingClient Gemini 流式客户端
 * @property useEnhancedStreaming 是否使用增强的流式处理
 * @property embeddingModel 嵌入模型名称
 */
class GeminiProvider(
    override val model: String,
    private val apiKey: String,
    private val client: GeminiClient = GeminiClient(apiKey),
    private val streamingClient: GeminiStreamingClient = GeminiStreamingClient(
        baseUrl = "https://generativelanguage.googleapis.com/v1",
        apiKey = apiKey
    ),
    private val useEnhancedStreaming: Boolean = false,
    private val embeddingModel: String = "models/embedding-001"
) : LlmProvider, KastraXBase(component = "LLM", name = "Gemini") {

    /**
     * 生成文本完成。
     *
     * @param messages 输入消息列表
     * @param options 生成选项
     * @return LLM 响应
     */
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): LlmResponse {
        logger.debug { "Generating completion with model: $model" }

        val request = createChatCompletionRequest(messages, options)
        val response = client.createChatCompletion(model, request)

        return response.toLlmResponse()
    }

    /**
     * 流式生成文本完成。
     *
     * @param messages 输入消息列表
     * @param options 生成选项
     * @return 文本流
     */
    override suspend fun streamGenerate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): Flow<String> {
        logger.debug { "Streaming completion with model: $model" }

        val request = createChatCompletionRequest(messages, options)

        val streamFlow = if (useEnhancedStreaming) {
            streamingClient.createChatCompletionStreamEnhanced(model, request)
        } else {
            streamingClient.createChatCompletionStream(model, request)
        }

        return streamFlow
            .map { chunk ->
                when (chunk) {
                    is GeminiStreamChunk.Content -> chunk.text
                    else -> ""
                }
            }
            .filter { it.isNotEmpty() }
            .catch { e ->
                logger.error(e) { "Error in stream generation: ${e.message}" }
                throw e
            }
    }

    /**
     * 生成文本嵌入。
     *
     * @param text 输入文本
     * @return 嵌入向量
     */
    override suspend fun embedText(text: String): List<Float> {
        logger.debug { "Generating embedding with model: $embeddingModel" }

        val request = GeminiEmbeddingRequest(
            model = embeddingModel,
            content = GeminiContent(
                role = "user",
                parts = listOf(
                    GeminiPart(
                        text = text
                    )
                )
            ),
            taskType = "RETRIEVAL_QUERY"
        )

        val response = client.createEmbedding(embeddingModel, request)
        return response.embedding.values
    }

    /**
     * 创建聊天完成请求。
     *
     * @param messages 输入消息列表
     * @param options 生成选项
     * @return Gemini 聊天完成请求
     */
    private fun createChatCompletionRequest(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): GeminiChatRequest {
        // 提取系统消息
        val systemMessage = messages.find { it.role == LlmMessageRole.SYSTEM }?.content
        
        // 过滤掉系统消息，只保留用户和助手消息
        val filteredMessages = messages.filter { it.role != LlmMessageRole.SYSTEM }
        
        val geminiContents = filteredMessages.map { it.toGeminiContent() }

        // 处理工具
        val tools = if (options.tools.isNotEmpty()) {
            val functionDeclarations = options.tools.mapNotNull { toolJson ->
                try {
                    val jsonObject = toolJson.jsonObject
                    val functionJson = jsonObject["function"]?.jsonObject

                    if (functionJson != null) {
                        GeminiFunctionDeclaration(
                            name = functionJson["name"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            description = functionJson["description"]?.jsonPrimitive?.contentOrNull,
                            parameters = functionJson["parameters"] ?: JsonObject(emptyMap())
                        )
                    } else null
                } catch (e: Exception) {
                    logger.warn { "Failed to parse tool: $toolJson" }
                    null
                }
            }
            
            if (functionDeclarations.isNotEmpty()) {
                listOf(GeminiTool(functionDeclarations = functionDeclarations))
            } else null
        } else null

        // 处理工具配置
        val toolConfig = if (tools != null) {
            GeminiToolConfig(
                functionCallingConfig = GeminiFunctionCallingConfig(
                    mode = when (options.toolChoice) {
                        "auto" -> "AUTO"
                        "none" -> "NONE"
                        else -> "AUTO"
                    }
                )
            )
        } else null

        return GeminiChatRequest(
            contents = geminiContents,
            tools = tools,
            toolConfig = toolConfig,
            generationConfig = GeminiGenerationConfig(
                temperature = options.temperature,
                topP = options.topP,
                maxOutputTokens = options.maxTokens,
                stopSequences = options.stop.takeIf { it.isNotEmpty() }
            ),
            system = systemMessage
        )
    }

    /**
     * 将 LlmMessage 转换为 GeminiContent。
     */
    private fun LlmMessage.toGeminiContent(): GeminiContent {
        val roleStr = when (role) {
            LlmMessageRole.USER -> "user"
            LlmMessageRole.ASSISTANT -> "model"
            LlmMessageRole.TOOL -> "function"
            else -> throw IllegalArgumentException("Unsupported role: $role")
        }

        // 处理工具调用
        val parts = if (toolCalls.isNotEmpty()) {
            // Gemini 不支持在单个消息中同时包含文本和工具调用
            // 因此，我们只包含工具调用信息
            listOf(
                GeminiPart(
                    text = "I need to use a tool to help with this request."
                )
            )
        } else {
            listOf(
                GeminiPart(
                    text = content
                )
            )
        }

        return GeminiContent(
            role = roleStr,
            parts = parts
        )
    }

    /**
     * 将 GeminiChatResponse 转换为 LlmResponse。
     */
    private fun GeminiChatResponse.toLlmResponse(): LlmResponse {
        if (candidates.isEmpty()) {
            return LlmResponse(
                content = "",
                finishReason = "empty_candidates"
            )
        }

        val candidate = candidates.first()
        
        // 提取文本内容
        val textContent = candidate.content.parts
            .mapNotNull { it.text }
            .joinToString("")

        return LlmResponse(
            content = textContent,
            finishReason = candidate.finishReason,
            usage = usage?.let {
                LlmUsage(
                    promptTokens = it.promptTokenCount,
                    completionTokens = it.candidatesTokenCount,
                    totalTokens = it.totalTokenCount
                )
            }
        )
    }
}
