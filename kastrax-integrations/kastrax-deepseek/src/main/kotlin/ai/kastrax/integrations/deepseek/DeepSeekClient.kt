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
    private val httpClient: HttpClient = createDefaultHttpClient(apiKey)
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
                    val buffer = StringBuilder()

                    while (!isClosedForRead) {
                        val line = readUTF8Line(limit = 8192) ?: continue

                        if (line.isBlank()) continue

                        if (line.startsWith("data: ")) {
                            val data = line.substring(6).trim()

                            // 检查是否是结束标记
                            if (data == "[DONE]") {
                                break
                            }

                            try {
                                val chunk = jsonParser.decodeFromString<DeepSeekChatCompletionResponse>(data)
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
                setBody(request)
            }

            response.body<DeepSeekEmbeddingResponse>()
        } catch (e: Exception) {
            logger.error(e) { "Error creating embedding: ${e.message}" }
            throw DeepSeekException("Failed to create embedding", e)
        }
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
         */
        fun createDefaultHttpClient(apiKey: String): HttpClient {
            return HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = false
                    })
                }

                install(HttpTimeout) {
                    requestTimeoutMillis = 60000
                    connectTimeoutMillis = 10000
                    socketTimeoutMillis = 60000
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
