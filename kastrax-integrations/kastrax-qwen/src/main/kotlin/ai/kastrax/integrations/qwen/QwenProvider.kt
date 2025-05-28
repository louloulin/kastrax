package ai.kastrax.integrations.qwen

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Qwen LLM 提供商，实现 KastraX 的 LlmProvider 接口。
 *
 * @property model Qwen 模型
 * @property apiKey Qwen API 密钥
 * @property client Qwen API 客户端
 */
class QwenProvider(
    override val model: String,
    private val apiKey: String,
    private val temperature: Double? = null,
    private val maxTokens: Int? = null,
    private val topP: Double? = null,
    private val timeout: Long = 60000,
    private val client: QwenClient = QwenClient(apiKey = apiKey, timeout = timeout),
    private val streamingClient: QwenStreamingClient? = null
) : LlmProvider, KastraXBase(component = "LLM", name = "Qwen") {

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
        val response = client.createChatCompletion(request)

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

        val request = createChatCompletionRequest(messages, options, stream = true)

        // 使用流式客户端（如果提供）或者默认客户端
        val streamFlow = if (streamingClient != null) {
            // 使用专用的流式客户端
            streamingClient.createChatCompletionStream(request)
        } else {
            // 使用普通客户端的流式处理
            client.streamChatCompletion(request)
        }

        return streamFlow
            .mapNotNull { chunk ->
                chunk.choices.firstOrNull()?.delta?.content
            }
            .filter { it.isNotEmpty() }
            .catch { e ->
                logger.error(e) { "Error in stream generation: ${e.message}" }
                throw e
            }
    }

    /**
     * 流式生成文本完成，包含工具调用。
     *
     * @param messages 输入消息列表
     * @param options 生成选项
     * @return 完整的流式响应块流
     */
    suspend fun streamGenerateWithToolCalls(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): Flow<QwenStreamChunk> {
        logger.debug { "Streaming completion with tool calls for model: $model" }

        val request = createChatCompletionRequest(messages, options, stream = true)

        // 使用流式客户端（如果提供）或者默认客户端
        val streamFlow = if (streamingClient != null) {
            // 使用专用的流式客户端
            streamingClient.createChatCompletionStream(request)
        } else {
            // 使用普通客户端的流式处理
            client.streamChatCompletion(request)
        }

        // 直接返回原始流，包含工具调用
        return streamFlow
            .catch { e ->
                logger.error(e) { "Error in stream generation with tool calls: ${e.message}" }
                throw e
            }
    }

    /**
     * 流式生成文本完成，并处理工具调用。
     *
     * @param messages 输入消息列表
     * @param options 生成选项
     * @return 包含文本和工具调用流的响应
     */
    override suspend fun streamGenerateWithTools(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): LlmStreamResponse {
        logger.debug { "Streaming completion with tools for model: $model" }

        val request = createChatCompletionRequest(messages, options, stream = true)

        // 分离文本和工具调用流
        val streamChunks = client.streamChatCompletion(request)

        // 文本流
        val textStream = streamChunks
            .mapNotNull { chunk ->
                chunk.choices.firstOrNull()?.delta?.content
            }
            .filter { it.isNotEmpty() }

        // 工具调用流
        val toolCallStream = streamChunks
            .mapNotNull { chunk ->
                chunk.choices.firstOrNull()?.delta?.toolCalls?.firstOrNull()
            }
            .map { toolCall ->
                LlmToolCall(
                    id = toolCall.id,
                    name = toolCall.function.name,
                    arguments = toolCall.function.arguments
                )
            }

        return LlmStreamResponse(
            textStream = textStream,
            toolCallStream = toolCallStream
        )
    }

    /**
     * 生成文本嵌入。
     *
     * @param text 输入文本
     * @return 嵌入向量
     */
    override suspend fun embedText(text: String): List<Float> {
        logger.debug { "Embedding text with model: $model" }
        
        // Qwen 的嵌入功能可能需要不同的端点或模型
        // 这里先返回空列表，实际实现需要根据 Qwen API 文档调整
        logger.warn { "Text embedding not yet implemented for Qwen" }
        return emptyList()
    }

    /**
     * 创建聊天完成请求。
     *
     * @param messages 输入消息列表
     * @param options 生成选项
     * @param stream 是否流式输出
     * @return Qwen 聊天完成请求
     */
    private fun createChatCompletionRequest(
        messages: List<LlmMessage>,
        options: LlmOptions,
        stream: Boolean = false
    ): QwenChatCompletionRequest {
        val qwenMessages = messages.map { it.toQwenMessage() }

        // 处理工具
        val tools = if (options.tools.isNotEmpty()) {
            options.tools.mapNotNull { toolJson ->
                try {
                    val jsonObject = toolJson.jsonObject
                    val functionJson = jsonObject["function"]?.jsonObject

                    if (functionJson != null) {
                        QwenTool(
                            type = "function",
                            function = QwenFunction(
                                name = functionJson["name"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                                description = functionJson["description"]?.jsonPrimitive?.contentOrNull,
                                parameters = functionJson["parameters"]?.jsonObject
                            )
                        )
                    } else null
                } catch (e: Exception) {
                    logger.warn { "Failed to parse tool: $toolJson" }
                    null
                }
            }
        } else null

        // 处理工具选择
        val toolChoice = when {
            options.toolChoice == "auto" -> "auto"
            options.toolChoice == "none" -> "none"
            options.toolChoice != null -> {
                // 如果是特定工具，需要构建工具选择对象
                // 这里简化处理，实际可能需要更复杂的逻辑
                options.toolChoice
            }
            else -> null
        }

        return QwenChatCompletionRequest(
            model = model,
            messages = qwenMessages,
            temperature = options.temperature ?: temperature,
            topP = options.topP ?: topP,
            maxTokens = options.maxTokens ?: maxTokens,
            stream = stream,
            stop = options.stop,
            repetitionPenalty = null, // Qwen 使用 repetitionPenalty 而不是 frequencyPenalty
            tools = tools,
            toolChoice = toolChoice as? String,
            user = null,
            enableSearch = null // Qwen 特有的搜索功能
        )
    }
}

/**
 * 将 LlmMessage 转换为 QwenMessage。
 */
private fun LlmMessage.toQwenMessage(): QwenMessage {
    return QwenMessage(
        role = this.role.name.lowercase(),
        content = this.content,
        name = this.name,
        toolCalls = this.toolCalls.map { toolCall ->
            QwenToolCall(
                id = toolCall.id,
                type = "function",
                function = QwenFunctionCall(
                    name = toolCall.name,
                    arguments = toolCall.arguments
                )
            )
        },
        toolCallId = this.toolCallId
    )
}

/**
 * 将 QwenChatCompletionResponse 转换为 LlmResponse。
 */
private fun QwenChatCompletionResponse.toLlmResponse(): LlmResponse {
    val choice = this.choices.firstOrNull()
    val message = choice?.message

    return LlmResponse(
        content = message?.content ?: "",
        finishReason = choice?.finishReason,
        usage = this.usage?.let {
            LlmUsage(
                promptTokens = it.promptTokens,
                completionTokens = it.completionTokens,
                totalTokens = it.totalTokens
            )
        },
        toolCalls = message?.toolCalls?.map { toolCall ->
            LlmToolCall(
                id = toolCall.id,
                name = toolCall.function.name,
                arguments = toolCall.function.arguments
            )
        } ?: emptyList()
    )
}