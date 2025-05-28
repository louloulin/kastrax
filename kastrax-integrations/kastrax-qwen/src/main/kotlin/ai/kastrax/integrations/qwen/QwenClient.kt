package ai.kastrax.integrations.qwen

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
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
 * Qwen API 客户端，用于与 Qwen API 进行交互。
 *
 * @property apiKey Qwen API 密钥
 * @property baseUrl Qwen API 基础 URL
 * @property httpClient HTTP 客户端
 */
class QwenClient(
    private val apiKey: String,
    private val baseUrl: String = "https://dashscope.aliyuncs.com/api/v1",
    private val timeout: Long = 60000,
    private val httpClient: HttpClient = createDefaultHttpClient(apiKey, timeout)
) {
    /**
     * 创建聊天完成。
     *
     * @param request 聊天完成请求
     * @return 聊天完成响应
     */
    suspend fun createChatCompletion(request: QwenChatCompletionRequest): QwenChatCompletionResponse {
        logger.debug { "Creating chat completion with model: ${request.model}" }

        val nonStreamingRequest = request.copy(stream = false)
        logger.debug { "Request body: $nonStreamingRequest" }

        return try {
            val response = httpClient.post("$baseUrl/services/aigc/text-generation/generation") {
                contentType(ContentType.Application.Json)
                setBody(nonStreamingRequest)
            }

            logger.debug { "Response status: ${response.status}" }
            if (response.status.isSuccess()) {
                val responseBody = response.body<QwenChatCompletionResponse>()
                logger.debug { "Response body: $responseBody" }
                responseBody
            } else {
                val errorText = response.bodyAsText()
                logger.error { "Error response: $errorText" }
                throw QwenException("Failed to create chat completion: ${response.status} - $errorText")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error creating chat completion: ${e.message}" }
            throw QwenException("Failed to create chat completion", e)
        }
    }

    /**
     * 流式创建聊天完成。
     *
     * @param request 聊天完成请求
     * @return 聊天完成响应流
     */
    suspend fun streamChatCompletion(request: QwenChatCompletionRequest): Flow<QwenStreamChunk> {
        logger.debug { "Streaming chat completion with model: ${request.model}" }

        val streamingRequest = request.copy(stream = true)

        return try {
            val response = httpClient.preparePost("$baseUrl/services/aigc/text-generation/generation") {
                contentType(ContentType.Application.Json)
                setBody(streamingRequest)
            }.execute()

            if (!response.status.isSuccess()) {
                val errorBody = response.bodyAsText()
                logger.error { "Qwen API error: $errorBody" }
                throw QwenException("Qwen API error: ${response.status.description}")
            }

            // 使用 channelFlow 处理流式响应
            channelFlow {
                val jsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

                response.bodyAsChannel().apply {
                    val buffer = StringBuilder()
                    
                    while (!isClosedForRead) {
                        val chunk = readUTF8Line()
                        if (chunk != null) {
                            buffer.append(chunk)
                            
                            // 处理完整的行
                            val lines = buffer.toString().split("\n")
                            buffer.clear()
                            
                            // 保留最后一行（可能不完整）
                            if (lines.isNotEmpty()) {
                                buffer.append(lines.last())
                            }
                            
                            // 处理完整的行
                            for (i in 0 until lines.size - 1) {
                                val line = lines[i].trim()
                                if (line.startsWith("data: ")) {
                                    val jsonData = line.substring(6).trim()
                                    if (jsonData == "[DONE]") {
                                        logger.debug { "Stream completed" }
                                        break
                                    }
                                    
                                    try {
                                        val streamChunk = jsonParser.decodeFromString<QwenStreamChunk>(jsonData)
                                        send(streamChunk)
                                    } catch (e: Exception) {
                                        logger.warn { "Failed to parse stream chunk: $jsonData, error: ${e.message}" }
                                    }
                                }
                            }
                        }
                    }
                    
                    // 处理剩余的缓冲区内容
                    val remainingLine = buffer.toString().trim()
                    if (remainingLine.startsWith("data: ")) {
                        val jsonData = remainingLine.substring(6).trim()
                        if (jsonData != "[DONE]") {
                            try {
                                val streamChunk = jsonParser.decodeFromString<QwenStreamChunk>(jsonData)
                                send(streamChunk)
                            } catch (e: Exception) {
                                logger.warn { "Failed to parse final stream chunk: $jsonData, error: ${e.message}" }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error streaming chat completion: ${e.message}" }
            flow { throw QwenException("Failed to stream chat completion", e) }
        }
    }

    companion object {
        /**
         * 创建默认的 HTTP 客户端。
         *
         * @param apiKey Qwen API 密钥
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
                    connectTimeoutMillis = timeout
                    socketTimeoutMillis = timeout
                }

                install(Logging) {
                    logger = Logger.DEFAULT
                    level = LogLevel.INFO
                }

                defaultRequest {
                    header("Authorization", "Bearer $apiKey")
                    header("Content-Type", "application/json")
                }
            }
        }
    }
}