package ai.kastrax.integrations.deepseek

import io.ktor.client.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.cache.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.logging.*
import io.ktor.client.plugins.sse.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.sse.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * DeepSeek 流式客户端，专门处理 SSE (Server-Sent Events) 流式响应。
 * 使用 Ktor 的 SSE 插件实现真正的流式响应。
 */
class DeepSeekStreamingClient(
    val httpClient: HttpClient,
    private val baseUrl: String,
    private val apiKey: String,
    private val json: Json = DeepSeekJson.json
) {
    /**
     * 创建一个新的 DeepSeekStreamingClient 实例，使用与 DeepSeekClient 相同的 HTTP 客户端配置。
     *
     * @param baseUrl DeepSeek API 的基础 URL
     * @param apiKey DeepSeek API 密钥
     * @param timeout 请求超时时间（毫秒），默认为 60000 毫秒（60秒）
     */
    constructor(baseUrl: String, apiKey: String, timeout: Long = 60000) : this(
        httpClient = HttpClient(OkHttp) {
            install(ContentNegotiation) {
                json(DeepSeekJson.json)
            }

            install(HttpTimeout) {
                requestTimeoutMillis = timeout
                connectTimeoutMillis = 30000
                socketTimeoutMillis = timeout
            }

            install(HttpCache)

            // 安装 SSE 插件，处理服务器发送事件
            install(SSE) {
                // 显示所有类型的事件
                showCommentEvents()
                showRetryEvents()
            }

            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
            }

            defaultRequest {
                // 检查 API 密钥是否已经包含 Bearer 前缀
                val authHeader = if (apiKey.startsWith("Bearer ")) {
                    apiKey
                } else {
                    "Bearer $apiKey"
                }
                logger.debug { "Using Authorization header: $authHeader" }
                header("Authorization", authHeader)
            }

            expectSuccess = true
        },
        baseUrl = baseUrl,
        apiKey = apiKey
    )

    /**
     * 创建聊天完成流。
     * 使用 SSE 协议处理流式响应，每个事件作为单独的数据块处理。
     * 使用 UTF-8 编码处理所有文本，确保中文等多字节字符能够正确显示。
     *
     * @param request 聊天完成请求
     * @return 包含增量响应的流
     */
    suspend fun createChatCompletionStream(
        request: DeepSeekChatCompletionRequest
    ): Flow<DeepSeekStreamChunk> = flow {
        logger.debug { "Creating chat completion stream with model: ${request.model}" }

        // 输出请求详细信息
        logger.debug { "Request body: $request" }

        // 设置系统属性，确保 UTF-8 编码
        System.setProperty("file.encoding", "UTF-8")
        System.setProperty("sun.jnu.encoding", "UTF-8")

        // 确保请求是流式的
        val streamingRequest = request.copy(stream = true)

        try {
            // 使用 Ktor 的 SSE 插件发送请求
            httpClient.sse(
                urlString = "$baseUrl/chat/completions",
                request = {
                    method = HttpMethod.Post
                    contentType(ContentType.Application.Json)
                    header("Authorization", "Bearer $apiKey")
                    header("Cache-Control", "no-cache")
                    header("Connection", "keep-alive")
                    setBody(streamingRequest)
                }
            ) {
                // 处理 SSE 事件
                incoming.collect { sseEvent ->
                    val data = sseEvent.data?.trim() ?: ""

                    // 检查是否是结束标记
                    if (data == "[DONE]") {
                        emit(DeepSeekStreamChunk.Done)
                        return@collect
                    }

                    try {
                        // 解析 JSON 数据
                        logger.debug { "Processing SSE event: $data" }
                        val chatResponse = json.decodeFromString<DeepSeekChatCompletionResponse>(data)
                        val choice = chatResponse.choices.firstOrNull()

                        // 提取增量内容
                        val content = choice?.delta?.content
                        val toolCalls = choice?.delta?.toolCalls

                        if (toolCalls != null && toolCalls.isNotEmpty()) {
                            logger.debug { "Received tool calls: $toolCalls" }

                            // 处理工具调用
                            for (toolCall in toolCalls) {
                                val function = toolCall.function
                                if (function != null) {
                                    // 发送工具调用事件
                                    emit(DeepSeekStreamChunk.ToolCall(
                                        id = toolCall.id ?: "",
                                        name = function.name ?: "",
                                        arguments = function.arguments ?: "{}"
                                    ))
                                }
                            }
                        }

                        if (content != null && content.isNotEmpty()) {
                            // 确保内容使用 UTF-8 编码处理
                            val utf8Content = String(content.toByteArray(Charsets.UTF_8), Charsets.UTF_8)

                            // 直接发送内容，不拆分字符
                            emit(DeepSeekStreamChunk.Content(utf8Content))
                        }

                        // 处理完成标记
                        val finishReason = choice?.finishReason
                        if (finishReason != null) {
                            logger.info { "Finish reason detected: $finishReason" }
                            emit(DeepSeekStreamChunk.Finished(finishReason))
                        }
                    } catch (e: Exception) {
                        logger.error(e) { "Error processing SSE event: ${e.message}" }
                        throw e
                    }
                }

                logger.debug { "SSE session completed" }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in createChatCompletionStream: ${e.message}" }
            throw e
        }
    }
}

/**
 * DeepSeek 流式响应块，表示流中的不同类型的事件。
 */
@Serializable
sealed class DeepSeekStreamChunk {
    /**
     * 内容块，包含实际的文本内容。
     */
    @Serializable
    @SerialName("content")
    data class Content(val text: String) : DeepSeekStreamChunk()

    /**
     * 工具调用块，包含工具调用信息。
     */
    @Serializable
    @SerialName("tool_call")
    data class ToolCall(
        val id: String,
        val name: String,
        val arguments: String
    ) : DeepSeekStreamChunk()

    /**
     * 完成标记，表示流已结束，并带有结束原因。
     */
    @Serializable
    @SerialName("finished")
    data class Finished(val reason: String) : DeepSeekStreamChunk()

    /**
     * 结束标记，表示流已完全结束。
     */
    @Serializable
    @SerialName("done")
    object Done : DeepSeekStreamChunk()
}
