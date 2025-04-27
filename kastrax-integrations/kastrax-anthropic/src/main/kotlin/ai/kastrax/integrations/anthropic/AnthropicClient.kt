package ai.kastrax.integrations.anthropic

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Anthropic API 客户端，用于与 Anthropic API 进行交互。
 *
 * @property apiKey Anthropic API 密钥
 * @property baseUrl Anthropic API 基础 URL
 * @property httpClient HTTP 客户端
 * @property timeout 超时时间（毫秒）
 */
class AnthropicClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.anthropic.com/v1",
    private val httpClient: HttpClient = createDefaultHttpClient(apiKey),
    private val timeout: Long = 60000
) {
    /**
     * 创建聊天完成。
     *
     * @param request 聊天完成请求
     * @return 聊天完成响应
     */
    suspend fun createChatCompletion(request: AnthropicChatRequest): AnthropicChatResponse {
        logger.debug { "Creating chat completion with model: ${request.model}" }

        val nonStreamingRequest = request.copy(stream = false)

        return try {
            val response = httpClient.post("$baseUrl/messages") {
                contentType(ContentType.Application.Json)
                setBody(nonStreamingRequest)
            }

            response.body<AnthropicChatResponse>()
        } catch (e: Exception) {
            logger.error(e) { "Error creating chat completion: ${e.message}" }
            throw handleApiError(e)
        }
    }

    /**
     * 创建流式聊天完成。
     *
     * @param request 聊天完成请求
     * @return 流式响应块流
     */
    suspend fun createChatCompletionStream(request: AnthropicChatRequest): Flow<AnthropicStreamChunk> {
        logger.debug { "Creating streaming chat completion with model: ${request.model}" }

        val streamingRequest = request.copy(stream = true)

        return try {
            val response = httpClient.post("$baseUrl/messages") {
                contentType(ContentType.Application.Json)
                setBody(streamingRequest)
            }

            if (!response.status.isSuccess()) {
                throw AnthropicException("Anthropic API error: ${response.status.description}")
            }

            // 解析 SSE 流
            parseStreamResponse(response)
        } catch (e: Exception) {
            logger.error(e) { "Error creating streaming chat completion: ${e.message}" }
            flow { throw handleApiError(e) }
        }
    }

    /**
     * 解析流式响应。
     *
     * @param response HTTP 响应
     * @return 流式响应块流
     */
    private fun parseStreamResponse(response: HttpResponse): Flow<AnthropicStreamChunk> = flow {
        val json = Json { ignoreUnknownKeys = true }

        response.bodyAsChannel().apply {
            while (!isClosedForRead) {
                val line = readUTF8Line(Int.MAX_VALUE) ?: continue

                if (line.isEmpty() || !line.startsWith("data:")) continue

                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") {
                    emit(AnthropicStreamChunk.Done)
                    break
                }

                try {
                    val streamResponse = json.decodeFromString<AnthropicStreamResponse>(data)

                    when (streamResponse.type) {
                        "content_block_delta" -> {
                            streamResponse.delta?.text?.let { text ->
                                emit(AnthropicStreamChunk.Content(text))
                            }

                            streamResponse.delta?.toolUse?.let { toolUse ->
                                emit(AnthropicStreamChunk.ToolUse(toolUse))
                            }
                        }
                        "content_block_start" -> {
                            // 测试中使用了content_block_start类型
                            if (streamResponse.message?.content?.isNotEmpty() == true) {
                                val content = streamResponse.message.content.first()
                                if (content.type == "tool_use" && content.toolUse != null) {
                                    emit(AnthropicStreamChunk.ToolUse(content.toolUse))
                                } else if (content.type == "text" && content.text != null) {
                                    emit(AnthropicStreamChunk.Content(content.text))
                                }
                            }
                        }
                        "message_stop" -> {
                            emit(AnthropicStreamChunk.Finished(streamResponse.stopReason))
                        }
                    }
                } catch (e: Exception) {
                    logger.warn { "Failed to parse stream response: $data" }
                    logger.warn(e) { "Parse error: ${e.message}" }
                }
            }
        }
    }

    /**
     * 处理 API 错误。
     *
     * @param e 异常
     * @return Anthropic 异常
     */
    private fun handleApiError(e: Exception): AnthropicException {
        return when (e) {
            is ClientRequestException -> {
                val status = e.response.status
                val message = when (status) {
                    HttpStatusCode.Unauthorized -> "Invalid API key or unauthorized access"
                    HttpStatusCode.BadRequest -> "Bad request: ${e.message}"
                    HttpStatusCode.TooManyRequests -> "Rate limit exceeded"
                    else -> "Anthropic API error: ${status.description}"
                }
                AnthropicException(message, e)
            }
            is ServerResponseException -> {
                AnthropicException("Anthropic server error: ${e.message}", e)
            }
            else -> AnthropicException("Unexpected error: ${e.message}", e)
        }
    }

    companion object {
        /**
         * 创建默认的 HTTP 客户端。
         *
         * @param apiKey Anthropic API 密钥
         * @param timeout 超时时间（毫秒）
         * @return HTTP 客户端
         */
        fun createDefaultHttpClient(apiKey: String, timeout: Long = 60000): HttpClient {
            return HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = false
                    })
                }

                install(HttpTimeout) {
                    requestTimeoutMillis = timeout
                    connectTimeoutMillis = 30000
                    socketTimeoutMillis = timeout
                }

                defaultRequest {
                    header("x-api-key", apiKey)
                    header("anthropic-version", "2023-06-01")
                }

                expectSuccess = true
            }
        }
    }
}
