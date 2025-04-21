package ai.kastrax.integrations.deepseek

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * DeepSeekStreamingClient 的单元测试。
 * 测试流式响应处理的各种场景。
 */
class DeepSeekStreamingClientTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

    /**
     * 创建模拟的 HTTP 客户端，用于测试。
     */
    private fun createMockClient(responseContent: String): HttpClient {
        return HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(json)
            }

            engine {
                addHandler { request ->
                    when (request.url.toString()) {
                        "https://api.deepseek.com/v1/chat/completions" -> {
                            respond(
                                content = responseContent,
                                status = HttpStatusCode.OK,
                                headers = headersOf(
                                    HttpHeaders.ContentType to listOf("text/event-stream"),
                                    "Cache-Control" to listOf("no-cache"),
                                    "Connection" to listOf("keep-alive")
                                )
                            )
                        }
                        else -> error("Unhandled ${request.url}")
                    }
                }
            }
        }
    }

    @Test
    fun `test streaming with single character chunks`() = runTest {
        // 准备模拟响应 - 每个事件包含单个字符
        val mockResponses = listOf(
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "H"
                        ),
                        finishReason = null
                    )
                ),
                usage = null
            ),
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "e"
                        ),
                        finishReason = null
                    )
                ),
                usage = null
            ),
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "l"
                        ),
                        finishReason = null
                    )
                ),
                usage = null
            ),
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "l"
                        ),
                        finishReason = null
                    )
                ),
                usage = null
            ),
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "o"
                        ),
                        finishReason = "stop"
                    )
                ),
                usage = null
            )
        )

        // 构建模拟的服务器发送事件格式响应
        val mockResponseText = mockResponses.joinToString("\n") {
            "data: ${json.encodeToString(it)}"
        } + "\n\ndata: [DONE]"

        val mockClient = createMockClient(mockResponseText)
        val streamingClient = DeepSeekStreamingClient(
            httpClient = mockClient,
            baseUrl = "https://api.deepseek.com/v1",
            apiKey = "test-api-key"
        )

        // 创建测试请求
        val request = DeepSeekChatCompletionRequest(
            model = "deepseek-chat",
            messages = listOf(
                DeepSeekMessage(
                    role = "user",
                    content = "Say hello"
                )
            ),
            stream = true
        )

        // 执行测试
        val chunks = streamingClient.createChatCompletionStream(request).toList()

        // 验证结果
        // 注意：由于我们的实现会将每个字符拆分为单独的块，所以我们期望有更多的块
        // 但是，在这个测试中，我们使用模拟响应，所以我们只会得到模拟的块
        assertEquals(7, chunks.size) // 5个内容块 + 1个完成块 + 1个结束块

        // 验证内容块
        val contentChunks = chunks.filterIsInstance<DeepSeekStreamChunk.Content>()
        assertEquals(5, contentChunks.size)
        assertEquals("H", contentChunks[0].text)
        assertEquals("e", contentChunks[1].text)
        assertEquals("l", contentChunks[2].text)
        assertEquals("l", contentChunks[3].text)
        assertEquals("o", contentChunks[4].text)

        // 验证完成块
        val finishedChunk = chunks.filterIsInstance<DeepSeekStreamChunk.Finished>().firstOrNull()
        assertTrue(finishedChunk != null)
        assertEquals("stop", finishedChunk.reason)

        // 验证结束块
        val doneChunk = chunks.filterIsInstance<DeepSeekStreamChunk.Done>().firstOrNull()
        assertTrue(doneChunk != null)
    }

    @Test
    fun `test streaming with multi-character chunks`() = runTest {
        // 准备模拟响应 - 每个事件包含多个字符
        val mockResponses = listOf(
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "Hello"
                        ),
                        finishReason = null
                    )
                ),
                usage = null
            ),
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = ", "
                        ),
                        finishReason = null
                    )
                ),
                usage = null
            ),
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "world"
                        ),
                        finishReason = null
                    )
                ),
                usage = null
            ),
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "!"
                        ),
                        finishReason = "stop"
                    )
                ),
                usage = null
            )
        )

        // 构建模拟的服务器发送事件格式响应
        val mockResponseText = mockResponses.joinToString("\n") {
            "data: ${json.encodeToString(it)}"
        } + "\n\ndata: [DONE]"

        val mockClient = createMockClient(mockResponseText)
        val streamingClient = DeepSeekStreamingClient(
            httpClient = mockClient,
            baseUrl = "https://api.deepseek.com/v1",
            apiKey = "test-api-key"
        )

        // 创建测试请求
        val request = DeepSeekChatCompletionRequest(
            model = "deepseek-chat",
            messages = listOf(
                DeepSeekMessage(
                    role = "user",
                    content = "Say hello world"
                )
            ),
            stream = true
        )

        // 执行测试
        val chunks = streamingClient.createChatCompletionStream(request).toList()

        // 验证结果
        // 由于我们的实现会将每个字符拆分为单独的块，所以我们期望有更多的块
        // "Hello" (5个字符) + ", " (2个字符) + "world" (5个字符) + "!" (1个字符) = 13个字符
        // 加上1个完成块和1个结束块，总共15个块
        assertEquals(15, chunks.size)

        // 验证内容块
        val contentChunks = chunks.filterIsInstance<DeepSeekStreamChunk.Content>()
        assertEquals(13, contentChunks.size)

        // 验证完成块
        val finishedChunk = chunks.filterIsInstance<DeepSeekStreamChunk.Finished>().firstOrNull()
        assertTrue(finishedChunk != null)
        assertEquals("stop", finishedChunk.reason)

        // 验证结束块
        val doneChunk = chunks.filterIsInstance<DeepSeekStreamChunk.Done>().firstOrNull()
        assertTrue(doneChunk != null)
    }

    @Test
    fun `test streaming with Chinese characters`() = runTest {
        // 准备模拟响应 - 包含中文字符
        val mockResponses = listOf(
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "你"
                        ),
                        finishReason = null
                    )
                ),
                usage = null
            ),
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "好"
                        ),
                        finishReason = null
                    )
                ),
                usage = null
            ),
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "，"
                        ),
                        finishReason = null
                    )
                ),
                usage = null
            ),
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "世"
                        ),
                        finishReason = null
                    )
                ),
                usage = null
            ),
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "界"
                        ),
                        finishReason = null
                    )
                ),
                usage = null
            ),
            DeepSeekChatCompletionResponse(
                id = "test-id",
                objectType = "chat.completion.chunk",
                created = 1677858242,
                model = "deepseek-chat",
                systemFingerprint = "fp123",
                choices = listOf(
                    DeepSeekChoice(
                        index = 0,
                        delta = DeepSeekMessage(
                            role = "assistant",
                            content = "！"
                        ),
                        finishReason = "stop"
                    )
                ),
                usage = null
            )
        )

        // 构建模拟的服务器发送事件格式响应
        val mockResponseText = mockResponses.joinToString("\n") {
            "data: ${json.encodeToString(it)}"
        } + "\n\ndata: [DONE]"

        val mockClient = createMockClient(mockResponseText)
        val streamingClient = DeepSeekStreamingClient(
            httpClient = mockClient,
            baseUrl = "https://api.deepseek.com/v1",
            apiKey = "test-api-key"
        )

        // 创建测试请求
        val request = DeepSeekChatCompletionRequest(
            model = "deepseek-chat",
            messages = listOf(
                DeepSeekMessage(
                    role = "user",
                    content = "Say hello in Chinese"
                )
            ),
            stream = true
        )

        // 执行测试
        val chunks = streamingClient.createChatCompletionStream(request).toList()

        // 验证结果
        // 6个中文字符 + 1个完成块 + 1个结束块 = 8个块
        assertEquals(8, chunks.size)

        // 验证内容块
        val contentChunks = chunks.filterIsInstance<DeepSeekStreamChunk.Content>()
        assertEquals(6, contentChunks.size)
        assertEquals("你", contentChunks[0].text)
        assertEquals("好", contentChunks[1].text)
        assertEquals("，", contentChunks[2].text)
        assertEquals("世", contentChunks[3].text)
        assertEquals("界", contentChunks[4].text)
        assertEquals("！", contentChunks[5].text)

        // 验证完成块
        val finishedChunk = chunks.filterIsInstance<DeepSeekStreamChunk.Finished>().firstOrNull()
        assertTrue(finishedChunk != null)
        assertEquals("stop", finishedChunk.reason)

        // 验证结束块
        val doneChunk = chunks.filterIsInstance<DeepSeekStreamChunk.Done>().firstOrNull()
        assertTrue(doneChunk != null)
    }

    @Test
    fun `test streaming with error handling`() = runTest {
        // 准备模拟错误响应
        val errorResponseText = """
            {"error": {"message": "Invalid API key", "type": "invalid_request_error", "code": "invalid_api_key"}}
        """.trimIndent()

        val mockClient = HttpClient(MockEngine) {
            install(ContentNegotiation) {
                json(json)
            }

            engine {
                addHandler { request ->
                    respond(
                        content = errorResponseText,
                        status = HttpStatusCode.Unauthorized,
                        headers = headersOf(HttpHeaders.ContentType to listOf("application/json"))
                    )
                }
            }
        }

        val streamingClient = DeepSeekStreamingClient(
            httpClient = mockClient,
            baseUrl = "https://api.deepseek.com/v1",
            apiKey = "invalid-api-key"
        )

        // 创建测试请求
        val request = DeepSeekChatCompletionRequest(
            model = "deepseek-chat",
            messages = listOf(
                DeepSeekMessage(
                    role = "user",
                    content = "Say hello"
                )
            ),
            stream = true
        )

        // 执行测试，期望抛出异常
        var exceptionThrown = false
        try {
            streamingClient.createChatCompletionStream(request).toList()
        } catch (e: DeepSeekException) {
            exceptionThrown = true
            // 只要抛出异常就算通过，不检查具体的错误消息
            println("捕获到预期的异常: ${e.message}")
        }

        assertTrue(exceptionThrown, "Expected DeepSeekException to be thrown")
    }
}
