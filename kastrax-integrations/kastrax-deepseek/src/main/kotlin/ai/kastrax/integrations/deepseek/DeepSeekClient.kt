package ai.kastrax.integrations.deepseek

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
 * DeepSeek API 客户端，用于与 DeepSeek API 进行交互。
 *
 * @property apiKey DeepSeek API 密钥
 * @property baseUrl DeepSeek API 基础 URL
 * @property httpClient HTTP 客户端
 */
class DeepSeekClient(
    private val apiKey: String,
    private val baseUrl: String = "https://api.deepseek.com/v1",
    private val timeout: Long = 60000,
    private val httpClient: HttpClient = createDefaultHttpClient(apiKey, timeout)
) {
    /**
     * 创建聊天完成。
     *
     * @param request 聊天完成请求
     * @return 聊天完成响应
     */
    suspend fun createChatCompletion(request: DeepSeekChatCompletionRequest): DeepSeekChatCompletionResponse {
        logger.debug { "Creating chat completion with model: ${request.model}" }

        val nonStreamingRequest = request.copy(stream = false)

        return try {
            val response = httpClient.post("$baseUrl/chat/completions") {
                contentType(ContentType.Application.Json)
                // 使用自动序列化
                setBody(nonStreamingRequest)
            }

            response.body<DeepSeekChatCompletionResponse>()
        } catch (e: Exception) {
            logger.error(e) { "Error creating chat completion: ${e.message}" }
            throw DeepSeekException("Failed to create chat completion", e)
        }
    }

    /**
     * 流式创建聊天完成。
     *
     * @param request 聊天完成请求
     * @return 聊天完成响应流
     */
    suspend fun streamChatCompletion(request: DeepSeekChatCompletionRequest): Flow<DeepSeekChatCompletionResponse> {
        logger.debug { "Streaming chat completion with model: ${request.model}" }

        val streamingRequest = request.copy(stream = true)

        return try {
            val response = httpClient.preparePost("$baseUrl/chat/completions") {
                contentType(ContentType.Application.Json)
                setBody(streamingRequest)
            }.execute()

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                logger.error { "DeepSeek API error: $errorBody" }
                throw DeepSeekException("DeepSeek API error: ${response.status.description}")
            }

            // 使用 channelFlow 处理流式响应
            channelFlow {
                val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

                response.bodyAsChannel().apply {
                    // 不需要buffer，直接处理每一行

                    while (!isClosedForRead) {
                        val line = readUTF8Line() ?: continue

                        if (line.isBlank()) continue

                        if (line.startsWith("data: ")) {
                            val data = line.substring(6).trim()

                            // 检查是否是结束标记
                            if (data == "[DONE]") {
                                break
                            }

                            try {
                                val chunk = jsonParser.decodeFromString(DeepSeekChatCompletionResponse.serializer(), data)
                                send(chunk)
                            } catch (e: Exception) {
                                logger.warn { "Failed to parse streaming response chunk: $data" }
                                logger.warn(e) { "Parse exception" }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error streaming chat completion: ${e.message}" }
            throw DeepSeekException("Failed to stream chat completion", e)
        }
    }

    /**
     * 创建嵌入。
     *
     * @param request 嵌入请求
     * @return 嵌入响应
     */
    suspend fun createEmbedding(request: DeepSeekEmbeddingRequest): DeepSeekEmbeddingResponse {
        logger.debug { "Creating embedding with model: ${request.model}" }

        return try {
            val response = httpClient.post("$baseUrl/embeddings") {
                contentType(ContentType.Application.Json)
                // 使用自动序列化
                setBody(request)
            }

            response.body<DeepSeekEmbeddingResponse>()
        } catch (e: Exception) {
            logger.error(e) { "Error creating embedding: ${e.message}" }
            throw DeepSeekException("Failed to create embedding", e)
        }
    }

    /**
     * 改进的流式聊天完成方法。
     * 返回细粒度更高的 DeepSeekStreamChunk 对象，更适合实时显示。
     *
     * @param request 聊天完成请求
     * @return 包含流式响应块的流
     */
    suspend fun streamChatCompletionEnhanced(
        request: DeepSeekChatCompletionRequest
    ): Flow<DeepSeekStreamChunk> {
        // 创建流式客户端，使用与当前客户端相同的 HTTP 客户端
        val streamingClient = DeepSeekStreamingClient(
            httpClient = httpClient,  // 使用相同的 HTTP 客户端
            baseUrl = baseUrl,
            apiKey = apiKey
        )

        logger.debug { "Using enhanced streaming for model: ${request.model}" }

        // 使用流式客户端创建流
        return streamingClient.createChatCompletionStream(request)
    }

    /**
     * 关闭客户端。
     */
    fun close() {
        httpClient.close()
    }

    companion object {
        /**
         * 创建默认的 HTTP 客户端。
         *
         * @param apiKey DeepSeek API 密钥
         * @param timeout 请求超时时间（毫秒），默认为 60000 毫秒（60秒）
         * @return HTTP 客户端
         */
        fun createDefaultHttpClient(apiKey: String, timeout: Long = 60000): HttpClient {
            return HttpClient(CIO) {
                install(ContentNegotiation) {
                    // 使用我们的自定义序列化器，它预先注册了所有需要的序列化器
                    json(DeepSeekJson.json)
                }

                install(HttpTimeout) {
                    requestTimeoutMillis = timeout
                    connectTimeoutMillis = 30000  // 增加连接超时时间
                    socketTimeoutMillis = timeout
                }

                defaultRequest {
                    header("Authorization", "Bearer $apiKey")
                }

                expectSuccess = true
            }
        }
    }
}

/**
 * DeepSeek 异常。
 */
class DeepSeekException(message: String, cause: Throwable? = null) : Exception(message, cause)
