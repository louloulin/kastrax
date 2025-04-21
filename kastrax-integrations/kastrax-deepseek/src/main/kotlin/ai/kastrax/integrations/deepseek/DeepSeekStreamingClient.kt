package ai.kastrax.integrations.deepseek

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.yield
import kotlinx.serialization.json.*
import mu.KotlinLogging
import java.net.URLEncoder
import java.nio.charset.StandardCharsets

private val logger = KotlinLogging.logger {}

/**
 * DeepSeek 流式客户端，专门处理 SSE (Server-Sent Events) 流式响应。
 * 参考了 OpenAI 和其他 SSE 实现的最佳实践。
 */
class DeepSeekStreamingClient(
    val httpClient: HttpClient,  // 更改为公开属性，以便外部访问
    private val baseUrl: String,
    private val apiKey: String,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }
) {
    /**
     * 创建一个新的 DeepSeekStreamingClient 实例，使用与 DeepSeekClient 相同的 HTTP 客户端配置。
     */
    constructor(baseUrl: String, apiKey: String) : this(
        httpClient = DeepSeekClient.createDefaultHttpClient(apiKey),
        baseUrl = baseUrl,
        apiKey = apiKey
    )

    /**
     * 创建聊天完成流。
     * 使用 SSE 协议处理流式响应，每个事件作为单独的数据块处理。
     *
     * @param request 聊天完成请求
     * @return 包含增量响应的流
     */
    suspend fun createChatCompletionStream(
        request: DeepSeekChatCompletionRequest
    ): Flow<DeepSeekStreamChunk> {
        logger.debug { "Creating chat completion stream with model: ${request.model}" }

        // 确保请求是流式的
        val streamingRequest = request.copy(stream = true)

        // 使用现有的 httpClient，而不是创建新的客户端
        // 这样可以避免配置兼容性问题

        // 使用 channelFlow 而不是普通 flow，以便更好地控制背压
        return channelFlow {
            try {
                // 使用现有的 httpClient，但采用字节级别的处理
                httpClient.preparePost("$baseUrl/chat/completions") {
                    contentType(ContentType.Application.Json)
                    // 添加 SSE 相关头部
                    header("Accept", "text/event-stream")
                    header("Cache-Control", "no-cache")
                    header("Connection", "keep-alive")
                    setBody(json.encodeToString(DeepSeekChatCompletionRequest.serializer(), streamingRequest))
                }.execute { response ->
                    // 检查响应状态
                    if (!response.status.isSuccess()) {
                        val errorBody = response.bodyAsText()
                        logger.error { "DeepSeek API error: $errorBody" }
                        throw DeepSeekException("DeepSeek API error: ${response.status.description}")
                    }

                    // 使用低级别的 bodyAsChannel 直接处理字节流
                    val channel = response.bodyAsChannel()

                    // 缓冲区用于收集 SSE 行
                    val lineBuffer = StringBuilder()

                    // 逐字节处理，确保实时性
                    val buffer = ByteArray(1) // 一次只读取一个字节

                    while (!channel.isClosedForRead) {
                        try {
                            // 读取一个字节
                            val bytesRead = channel.readAvailable(buffer, 0, 1)
                            if (bytesRead <= 0) {
                                // 没有数据可读，等待一下
                                delay(1)
                                continue
                            }

                            // 将字节转换为字符
                            val char = buffer[0].toInt().toChar()

                            if (char == '\n') {
                                // 行结束，处理完整行
                                val line = lineBuffer.toString()
                                lineBuffer.clear()

                                // 如果是空行，表示 SSE 事件结束
                                if (line.isBlank()) continue

                                // 处理 SSE 数据行
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

                                        if (content != null && content.isNotEmpty()) {
                                            // 关键改进：立即发送每个字符，不累积
                                            for (contentChar in content) {
                                                send(DeepSeekStreamChunk.Content(contentChar.toString()))
                                                // 关键：每发送一个字符就让出协程
                                                yield()
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
                            } else if (char != '\r') { // 忽略回车符
                                // 将字符添加到行缓冲区
                                lineBuffer.append(char)
                            }

                            // 立即让出协程，确保其他协程可以处理数据
                            yield()
                        } catch (e: CancellationException) {
                            throw e
                        } catch (e: Exception) {
                            // 忽略读取错误，继续尝试
                            delay(1)
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
sealed class DeepSeekStreamChunk {
    /**
     * 内容块，包含实际的文本内容。
     */
    data class Content(val text: String) : DeepSeekStreamChunk()

    /**
     * 完成标记，表示流已结束，并带有结束原因。
     */
    data class Finished(val reason: String) : DeepSeekStreamChunk()

    /**
     * 结束标记，表示流已完全结束。
     */
    object Done : DeepSeekStreamChunk()
}
