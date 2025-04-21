package ai.kastrax.integrations.anthropic

import io.ktor.client.*
import kotlinx.coroutines.flow.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Anthropic 流式客户端，提供增强的流式处理功能。
 *
 * @property httpClient HTTP 客户端
 * @property baseUrl Anthropic API 基础 URL
 * @property apiKey Anthropic API 密钥
 */
class AnthropicStreamingClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://api.anthropic.com/v1",
    private val apiKey: String
) {
    /**
     * 创建一个新的 AnthropicStreamingClient 实例，使用与 AnthropicClient 相同的 HTTP 客户端配置。
     */
    constructor(baseUrl: String, apiKey: String, timeout: Long = 60000) : this(
        httpClient = AnthropicClient.createDefaultHttpClient(apiKey, timeout),
        baseUrl = baseUrl,
        apiKey = apiKey
    )

    /**
     * 创建聊天完成流。
     *
     * @param request 聊天完成请求
     * @return 流式响应块流
     */
    suspend fun createChatCompletionStream(request: AnthropicChatRequest): Flow<AnthropicStreamChunk> {
        logger.debug { "Creating streaming chat completion with model: ${request.model}" }
        
        val client = AnthropicClient(apiKey, baseUrl, httpClient)
        return client.createChatCompletionStream(request)
    }

    /**
     * 增强的流式处理方法，将每个文本块拆分为单个字符，以实现更平滑的流式效果。
     *
     * @param request 聊天完成请求
     * @return 流式响应块流
     */
    suspend fun createChatCompletionStreamEnhanced(request: AnthropicChatRequest): Flow<AnthropicStreamChunk> {
        return createChatCompletionStream(request)
            .flatMapConcat { chunk ->
                when (chunk) {
                    is AnthropicStreamChunk.Content -> {
                        // 将文本块拆分为单个字符
                        val characters = chunk.text.map { AnthropicStreamChunk.Content(it.toString()) }
                        flow {
                            characters.forEach { emit(it) }
                        }
                    }
                    else -> flowOf(chunk)
                }
            }
    }
}
