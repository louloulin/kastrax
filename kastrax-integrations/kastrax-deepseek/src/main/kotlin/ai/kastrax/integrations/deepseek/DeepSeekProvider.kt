package ai.kastrax.integrations.deepseek

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * DeepSeek LLM 提供商，实现 KastraX 的 LlmProvider 接口。
 *
 * @property model DeepSeek 模型
 * @property apiKey DeepSeek API 密钥
 * @property client DeepSeek API 客户端
 */
class DeepSeekProvider(
    override val model: String,
    private val apiKey: String,
    private val temperature: Double? = null,
    private val maxTokens: Int? = null,
    private val topP: Double? = null,
    private val timeout: Long = 60000,
    private val client: DeepSeekClient = DeepSeekClient(apiKey = apiKey, timeout = timeout)
) : LlmProvider, KastraXBase(component = "LLM", name = "DeepSeek") {

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

        // 使用增强的流式处理方法
        return client.streamChatCompletionEnhanced(request)
            .map { chunk ->
                when (chunk) {
                    is DeepSeekStreamChunk.Content -> chunk.text
                    is DeepSeekStreamChunk.ToolCall -> "" // 工具调用在文本流中不显示
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
     * 流式生成文本完成，包含工具调用。
     *
     * @param messages 输入消息列表
     * @param options 生成选项
     * @return 完整的流式响应块流
     */
    suspend fun streamGenerateWithToolCalls(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): Flow<DeepSeekStreamChunk> {
        logger.debug { "Streaming completion with tool calls for model: $model" }

        val request = createChatCompletionRequest(messages, options, stream = true)

        // 直接返回原始流，包含工具调用
        return client.streamChatCompletionEnhanced(request)
            .catch { e ->
                logger.error(e) { "Error in stream generation with tool calls: ${e.message}" }
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
        logger.debug { "Embedding text with model: $model" }

        val request = DeepSeekEmbeddingRequest(
            model = model,
            input = listOf(text)
        )

        val response = client.createEmbedding(request)
        return response.data.firstOrNull()?.embedding ?: emptyList()
    }





    /**
     * 创建聊天完成请求。
     *
     * @param messages 输入消息列表
     * @param options 生成选项
     * @param stream 是否流式输出
     * @return DeepSeek 聊天完成请求
     */
    private fun createChatCompletionRequest(
        messages: List<LlmMessage>,
        options: LlmOptions,
        stream: Boolean = false
    ): DeepSeekChatCompletionRequest {
        val deepSeekMessages = messages.map { it.toDeepSeekMessage() }

        // 处理工具
        val tools = if (options.tools.isNotEmpty()) {
            options.tools.mapNotNull { toolJson ->
                try {
                    val jsonObject = toolJson.jsonObject
                    val functionJson = jsonObject["function"]?.jsonObject

                    if (functionJson != null) {
                        DeepSeekTool(
                            type = "function",
                            function = DeepSeekFunction(
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

        return DeepSeekChatCompletionRequest(
            model = model,
            messages = deepSeekMessages,
            temperature = options.temperature,
            topP = options.topP,
            maxTokens = options.maxTokens,
            stream = stream,
            stop = options.stop,
            frequencyPenalty = options.frequencyPenalty,
            presencePenalty = options.presencePenalty,
            tools = tools,
            toolChoice = toolChoice as? String,
            user = null
        )
    }

    /**
     * 将 LlmMessage 转换为 DeepSeekMessage。
     */
    private fun LlmMessage.toDeepSeekMessage(): DeepSeekMessage {
        val roleStr = when (role) {
            LlmMessageRole.SYSTEM -> "system"
            LlmMessageRole.USER -> "user"
            LlmMessageRole.ASSISTANT -> "assistant"
            LlmMessageRole.TOOL -> "tool"
        }

        return DeepSeekMessage(
            role = roleStr,  // 现在 role 是可选的，但在这里我们总是提供它
            content = content,
            name = name,
            toolCalls = toolCalls.map { it.toDeepSeekToolCall() }.takeIf { it.isNotEmpty() },
            toolCallId = toolCallId
        )
    }

    /**
     * 将 LlmToolCall 转换为 DeepSeekToolCall。
     */
    private fun LlmToolCall.toDeepSeekToolCall(): DeepSeekToolCall {
        return DeepSeekToolCall(
            id = id,
            type = "function",
            function = DeepSeekFunctionCall(
                name = name,
                arguments = arguments
            )
        )
    }



    /**
     * 将 DeepSeekToolCall 转换为 LlmToolCall。
     */
    private fun DeepSeekToolCall.toLlmToolCall(): LlmToolCall {
        return LlmToolCall(
            id = id,
            name = function.name,
            arguments = function.arguments
        )
    }

    /**
     * 将 DeepSeekChatCompletionResponse 转换为 LlmResponse。
     */
    private fun DeepSeekChatCompletionResponse.toLlmResponse(): LlmResponse {
        val choice = choices.firstOrNull()
        val message = choice?.message

        // 提取工具调用
        val toolCalls = message?.toolCalls?.map { it.toLlmToolCall() } ?: emptyList()

        return LlmResponse(
            content = message?.content ?: "",
            toolCalls = toolCalls,
            finishReason = choice?.finishReason,
            usage = usage?.let {
                LlmUsage(
                    promptTokens = it.promptTokens,
                    completionTokens = it.completionTokens,
                    totalTokens = it.totalTokens
                )
            }
        )
    }
}
