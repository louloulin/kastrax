package ai.kastrax.integrations.gemini

import io.ktor.client.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Google Gemini 流式客户端，提供增强的流式处理功能。
 *
 * @property httpClient HTTP 客户端
 * @property baseUrl Gemini API 基础 URL
 * @property apiKey Google API 密钥
 */
class GeminiStreamingClient(
    private val httpClient: HttpClient,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1",
    private val apiKey: String
) {
    /**
     * 创建一个新的 GeminiStreamingClient 实例，使用与 GeminiClient 相同的 HTTP 客户端配置。
     */
    constructor(baseUrl: String, apiKey: String) : this(
        httpClient = GeminiClient.createDefaultHttpClient(),
        baseUrl = baseUrl,
        apiKey = apiKey
    )

    /**
     * 创建聊天完成流。
     *
     * @param model 模型名称
     * @param request 聊天完成请求
     * @return 流式响应块流
     */
    suspend fun createChatCompletionStream(model: String, request: GeminiChatRequest): Flow<GeminiStreamChunk> {
        logger.debug { "Creating streaming chat completion with model: $model" }

        val client = GeminiClient(apiKey, baseUrl, httpClient)
        return client.createChatCompletionStream(model, request)
    }

    /**
     * 增强的流式处理方法，将每个文本块拆分为单个字符，以实现更平滑的流式效果。
     *
     * @param model 模型名称
     * @param request 聊天完成请求
     * @return 流式响应块流
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun createChatCompletionStreamEnhanced(model: String, request: GeminiChatRequest): Flow<GeminiStreamChunk> {
        return createChatCompletionStream(model, request)
            .flatMapConcat { chunk ->
                when (chunk) {
                    is GeminiStreamChunk.Content -> {
                        // 将文本块拆分为单个字符
                        val characters = chunk.text.map { GeminiStreamChunk.Content(it.toString()) }
                        flow {
                            characters.forEach { emit(it) }
                        }
                    }
                    else -> flowOf(chunk)
                }
            }
    }
}
