package ai.kastrax.integrations.qwen

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Qwen 流式客户端，专门处理流式响应。
 */
class QwenStreamingClient(
    val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }
) {
    // 添加一个标志，用于测试模式
    private var testMode = false

    /**
     * 设置测试模式
     */
    fun setTestMode(enabled: Boolean) {
        testMode = enabled
    }

    /**
     * 创建一个新的 QwenStreamingClient 实例，使用与 QwenClient 相同的 HTTP 客户端配置。
     *
     * @param baseUrl Qwen API 的基础 URL
     * @param apiKey Qwen API 密钥
     * @param timeout 请求超时时间（毫秒），默认为 60000 毫秒（60秒）
     */
    constructor(baseUrl: String, apiKey: String, timeout: Long = 60000) : this(
        httpClient = HttpClient(CIO) {
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

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }

            defaultRequest {
                // 确保 API 密钥格式正确
                val cleanApiKey = if (apiKey.startsWith("Bearer ")) {
                    apiKey.substring("Bearer ".length)
                } else {
                    apiKey
                }
                header("Authorization", "Bearer $cleanApiKey")
                header("Content-Type", "application/json")
                header("Accept", "text/event-stream")
            }
        },
        baseUrl = baseUrl,
        apiKey = apiKey
    )

    /**
     * 创建聊天完成流。
     *
     * @param request 聊天完成请求
     * @return 流式响应块流
     */
    suspend fun createChatCompletionStream(request: QwenChatCompletionRequest): Flow<QwenStreamChunk> {
        logger.debug { "Creating streaming chat completion with model: ${request.model}" }
        
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

            // 解析 SSE 流
            parseStreamResponse(response)
        } catch (e: Exception) {
            logger.error(e) { "Error creating streaming chat completion: ${e.message}" }
            flow { throw QwenException("Failed to create streaming chat completion", e) }
        }
    }

    /**
     * 增强的流式处理方法，将每个文本块拆分为单个字符，以实现更平滑的流式效果。
     *
     * @param request 聊天完成请求
     * @return 增强的流式响应块流
     */
    suspend fun createChatCompletionStreamEnhanced(request: QwenChatCompletionRequest): Flow<QwenStreamChunk> {
        logger.debug { "Creating enhanced streaming chat completion with model: ${request.model}" }
        
        return createChatCompletionStream(request)
            .flatMapConcat { chunk ->
                // 如果是包含文本内容的块，将其拆分为单个字符
                if (chunk.choices.isNotEmpty() && 
                    chunk.choices[0].delta?.content != null && 
                    chunk.choices[0].delta?.content?.isNotEmpty() == true) {
                    
                    val content = chunk.choices[0].delta?.content ?: ""
                    val choice = chunk.choices[0]
                    
                    // 将文本拆分为单个字符，每个字符作为一个独立的块发送
                    content.asSequence().map { char ->
                        chunk.copy(
                            choices = listOf(
                                choice.copy(
                                    delta = choice.delta?.copy(content = char.toString())
                                )
                            )
                        )
                    }.asFlow()
                } else {
                    // 对于非文本内容的块，直接发送
                    flowOf(chunk)
                }
            }
    }

    /**
     * 解析流式响应。
     *
     * @param response HTTP 响应
     * @return 流式响应块流
     */
    private suspend fun parseStreamResponse(response: HttpResponse): Flow<QwenStreamChunk> {
        return channelFlow {
            val channel = response.bodyAsChannel()
            val buffer = StringBuilder()
            
            try {
                while (!channel.isClosedForRead) {
                    val chunk = channel.readUTF8Line()
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
                                    val streamChunk = json.decodeFromString<QwenStreamChunk>(jsonData)
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
                            val streamChunk = json.decodeFromString<QwenStreamChunk>(jsonData)
                            send(streamChunk)
                        } catch (e: Exception) {
                            logger.warn { "Failed to parse final stream chunk: $jsonData, error: ${e.message}" }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error parsing stream response: ${e.message}" }
                throw QwenException("Failed to parse stream response", e)
            }
        }
    }

    /**
     * 关闭客户端。
     */
    fun close() {
        httpClient.close()
    }
}