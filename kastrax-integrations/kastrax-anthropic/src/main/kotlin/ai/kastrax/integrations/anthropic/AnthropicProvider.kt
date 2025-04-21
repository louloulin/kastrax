package ai.kastrax.integrations.anthropic

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Anthropic LLM 提供商，实现 KastraX 的 LlmProvider 接口。
 *
 * @property model Anthropic 模型
 * @property apiKey Anthropic API 密钥
 * @property client Anthropic API 客户端
 * @property streamingClient Anthropic 流式客户端
 * @property useEnhancedStreaming 是否使用增强的流式处理
 */
class AnthropicProvider(
    override val model: String,
    private val apiKey: String,
    private val client: AnthropicClient = AnthropicClient(apiKey),
    private val streamingClient: AnthropicStreamingClient = AnthropicStreamingClient(
        baseUrl = "https://api.anthropic.com/v1",
        apiKey = apiKey
    ),
    private val useEnhancedStreaming: Boolean = false
) : LlmProvider, KastraXBase(component = "LLM", name = "Anthropic") {

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

        val streamFlow = if (useEnhancedStreaming) {
            streamingClient.createChatCompletionStreamEnhanced(request)
        } else {
            streamingClient.createChatCompletionStream(request)
        }

        return streamFlow
            .map { chunk ->
                when (chunk) {
                    is AnthropicStreamChunk.Content -> chunk.text
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
     * 注意：Anthropic 目前不提供嵌入 API，此方法抛出异常。
     *
     * @param text 输入文本
     * @return 嵌入向量
     */
    override suspend fun embedText(text: String): List<Float> {
        throw UnsupportedOperationException("Anthropic does not provide an embedding API")
    }

    /**
     * 创建聊天完成请求。
     *
     * @param messages 输入消息列表
     * @param options 生成选项
     * @param stream 是否流式输出
     * @return Anthropic 聊天完成请求
     */
    private fun createChatCompletionRequest(
        messages: List<LlmMessage>,
        options: LlmOptions,
        stream: Boolean = false
    ): AnthropicChatRequest {
        // 提取系统消息
        val systemMessage = messages.find { it.role == LlmMessageRole.SYSTEM }?.content

        // 过滤掉系统消息，只保留用户和助手消息
        val filteredMessages = messages.filter { it.role != LlmMessageRole.SYSTEM }

        val anthropicMessages = filteredMessages.map { it.toAnthropicMessage() }

        // 处理工具
        val tools = if (options.tools.isNotEmpty()) {
            options.tools.mapNotNull { toolJson ->
                try {
                    val jsonObject = toolJson.jsonObject
                    val functionJson = jsonObject["function"]?.jsonObject

                    if (functionJson != null) {
                        AnthropicTool(
                            name = functionJson["name"]?.jsonPrimitive?.content ?: return@mapNotNull null,
                            description = functionJson["description"]?.jsonPrimitive?.contentOrNull,
                            inputSchema = functionJson["parameters"] ?: JsonObject(emptyMap())
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
            options.toolChoice != null -> options.toolChoice
            else -> null
        }

        return AnthropicChatRequest(
            model = model,
            messages = anthropicMessages,
            system = systemMessage,
            temperature = options.temperature,
            topP = options.topP,
            maxTokens = options.maxTokens,
            stream = stream,
            stopSequences = options.stop.takeIf { it.isNotEmpty() },
            tools = tools,
            toolChoice = toolChoice
        )
    }

    /**
     * 将 LlmMessage 转换为 AnthropicMessage。
     */
    private fun LlmMessage.toAnthropicMessage(): AnthropicMessage {
        val roleStr = when (role) {
            LlmMessageRole.USER -> "user"
            LlmMessageRole.ASSISTANT -> "assistant"
            LlmMessageRole.TOOL -> "tool"
            else -> throw IllegalArgumentException("Unsupported role: $role")
        }

        // 处理工具调用
        val contentList = if (toolCalls.isNotEmpty()) {
            val textContent = if (content.isNotEmpty()) {
                listOf(AnthropicContent(type = "text", text = content))
            } else {
                emptyList()
            }

            val toolUseContents = toolCalls.map { toolCall ->
                AnthropicContent(
                    type = "tool_use",
                    toolUse = AnthropicToolUse(
                        id = toolCall.id,
                        name = toolCall.name,
                        input = Json.parseToJsonElement(toolCall.arguments)
                    )
                )
            }

            textContent + toolUseContents
        } else {
            listOf(AnthropicContent(type = "text", text = content))
        }

        return AnthropicMessage(
            role = roleStr,
            content = contentList
        )
    }

    /**
     * 将 AnthropicChatResponse 转换为 LlmResponse。
     */
    private fun AnthropicChatResponse.toLlmResponse(): LlmResponse {
        // 提取文本内容
        val textContent = content.filter { it.type == "text" }
            .mapNotNull { it.text }
            .joinToString("")

        // 提取工具调用
        val toolCalls = content.filter { it.type == "tool_use" }
            .mapNotNull { content ->
                content.toolUse?.let { toolUse ->
                    LlmToolCall(
                        id = toolUse.id,
                        name = toolUse.name,
                        arguments = toolUse.input.toString()
                    )
                }
            }

        return LlmResponse(
            content = textContent,
            toolCalls = toolCalls,
            finishReason = stopReason,
            usage = usage.let {
                LlmUsage(
                    promptTokens = it.inputTokens,
                    completionTokens = it.outputTokens,
                    totalTokens = it.inputTokens + it.outputTokens
                )
            }
        )
    }
}
