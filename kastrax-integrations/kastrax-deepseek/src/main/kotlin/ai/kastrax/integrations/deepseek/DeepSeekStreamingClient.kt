package ai.kastrax.integrations.deepseek

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.yield
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

/**
 * DeepSeek 流式客户端，专门处理 SSE (Server-Sent Events) 流式响应。
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
     * @param retryCount 重试次数，默认为 3
     */
    constructor(baseUrl: String, apiKey: String, timeout: Long = 60000, retryCount: Int = 3) : this(
        httpClient = DeepSeekClient.createDefaultHttpClient(apiKey, timeout),
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
    ): Flow<DeepSeekStreamChunk> {
        logger.debug { "Creating chat completion stream with model: ${request.model}" }

        // 设置系统属性，确保 UTF-8 编码
        System.setProperty("file.encoding", "UTF-8")
        System.setProperty("sun.jnu.encoding", "UTF-8")

        // 确保请求是流式的
        val streamingRequest = request.copy(stream = true)

        // 使用 channelFlow 而不是普通 flow，以便更好地控制背压
        return channelFlow {
            try {
                // 添加更多的 SSE 相关头部，确保实时响应
                val response = httpClient.preparePost("$baseUrl/chat/completions") {
                    contentType(ContentType.Application.Json)
                    // 添加 SSE 相关头部
                    header("Accept", "text/event-stream")
                    header("Cache-Control", "no-cache")
                    header("Connection", "keep-alive")
                    // 使用自动序列化
                    setBody(streamingRequest)
                }.execute()

                if (!response.status.isSuccess()) {
                    val errorBody = response.bodyAsText()
                    logger.error { "DeepSeek API error: $errorBody" }
                    throw DeepSeekException("DeepSeek API error: ${response.status.description}")
                }

                // 处理 SSE 响应
                val channel = response.bodyAsChannel()

                // 逐行处理 SSE 数据，不使用缓冲区
                while (!channel.isClosedForRead) {
                    val line = channel.readUTF8Line() ?: continue
                    if (line.isBlank()) continue

                    if (line.startsWith("data: ")) {
                        val data = line.substring(6).trim()

                        // 检查是否是结束标记
                        if (data == "[DONE]") {
                            send(DeepSeekStreamChunk.Done)
                            break
                        }

                        try {
                            // 解析 JSON 数据
                            val chatResponse = json.decodeFromString<DeepSeekChatCompletionResponse>(data)
                            val choice = chatResponse.choices.firstOrNull()

                            // 提取增量内容
                            val content = choice?.delta?.content
                            val toolCalls = choice?.delta?.toolCalls

                            if (content != null && content.isNotEmpty()) {
                                // 关键改进：将内容拆分为单个字符，确保真正的字符级实时返回
                                // 先确保内容使用 UTF-8 编码处理
                                val utf8Content = String(content.toByteArray(Charsets.UTF_8), Charsets.UTF_8)

                                // 将内容拆分为单个字符，逐个发送
                                utf8Content.forEachIndexed { index, char ->
                                    // 对于中文等多字节字符，需要特殊处理
                                    val charStr = char.toString()
                                    send(DeepSeekStreamChunk.Content(charStr))

                                    // 每发送一个字符就让出协程，确保实时处理
                                    yield()

                                    // 添加小延迟，模拟打字效果，但不要太长
                                    delay(5) // 5毫秒延迟，可以根据需要调整
                                }
                            }

                            // 处理工具调用
                            if (toolCalls != null && toolCalls.isNotEmpty()) {
                                for (toolCall in toolCalls) {
                                    val function = toolCall.function
                                    if (function != null) {
                                        send(DeepSeekStreamChunk.ToolCall(
                                            id = toolCall.id,
                                            name = function.name,
                                            arguments = function.arguments
                                        ))
                                        yield()
                                    }
                                }
                            }

                            // 检查是否完成
                            val finishReason = choice?.finishReason
                            if (finishReason != null) {
                                send(DeepSeekStreamChunk.Finished(finishReason))
                            }
                        } catch (e: Exception) {
                            logger.warn { "Failed to parse SSE data: $data" }
                        }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Error in chat completion stream: ${e.message}" }
                throw DeepSeekException("Failed to stream chat completion", e)
            }
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
