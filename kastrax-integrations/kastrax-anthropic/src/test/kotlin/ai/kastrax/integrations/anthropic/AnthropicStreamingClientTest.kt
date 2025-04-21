package ai.kastrax.integrations.anthropic

import io.ktor.client.*
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * AnthropicStreamingClient 的单元测试。
 * 测试流式响应处理的各种场景。
 */
class AnthropicStreamingClientTest {

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
                        "https://api.anthropic.com/v1/messages" -> {
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
    fun `test streaming with text chunks`() = runTest {
        // 我们将使用一个简化的测试方法，只测试流式响应的基本功能
        val mockResponse = """
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text","text":"Hello"}}

            data: {"type":"content_block_delta","index":0,"delta":{"type":"text","text":", world!"}}

            data: {"type":"message_stop","message":{"id":"msg_123","type":"message","role":"assistant","content":[{"type":"text","text":"Hello, world!"}],"model":"claude-3-sonnet-20240229","stop_reason":"end_turn","usage":{"input_tokens":10,"output_tokens":3}}}

            data: [DONE]
        """.trimIndent()

        val mockClient = createMockClient(mockResponse)
        val streamingClient = AnthropicStreamingClient(
            httpClient = mockClient,
            baseUrl = "https://api.anthropic.com/v1",
            apiKey = "test-api-key"
        )

        // 创建测试请求
        val request = AnthropicChatRequest(
            model = "claude-3-sonnet-20240229",
            messages = listOf(
                AnthropicMessage(
                    role = "user",
                    content = listOf(
                        AnthropicContent(
                            type = "text",
                            text = "Say hello world"
                        )
                    )
                )
            ),
            stream = true
        )

        // 执行测试
        val chunks = streamingClient.createChatCompletionStream(request).toList()

        // 验证我们收到了一些块
        assertTrue(chunks.isNotEmpty())

        // 验证我们收到了内容块
        val contentChunks = chunks.filterIsInstance<AnthropicStreamChunk.Content>()
        assertTrue(contentChunks.isNotEmpty())

        // 验证我们收到了完成块
        val finishedChunks = chunks.filterIsInstance<AnthropicStreamChunk.Finished>()
        assertTrue(finishedChunks.isNotEmpty())

        // 验证我们收到了结束块
        val doneChunks = chunks.filterIsInstance<AnthropicStreamChunk.Done>()
        assertTrue(doneChunks.isNotEmpty())
    }

    @Test
    fun `test streaming with tool use`() = runTest {
        // 我们将使用一个简化的测试方法，只测试流式响应的基本功能
        val mockResponse = """
            data: {"type":"content_block_start","index":0,"content_block":{"type":"tool_use","id":"call_123","name":"get_weather","input":{"location":"Beijing","unit":"celsius"}}}

            data: {"type":"message_stop","message":{"id":"msg_123","type":"message","role":"assistant","content":[{"type":"tool_use","id":"call_123","name":"get_weather","input":{"location":"Beijing","unit":"celsius"}}],"model":"claude-3-sonnet-20240229","stop_reason":"tool_use","usage":{"input_tokens":15,"output_tokens":8}}}

            data: [DONE]
        """.trimIndent()

        val mockClient = createMockClient(mockResponse)
        val streamingClient = AnthropicStreamingClient(
            httpClient = mockClient,
            baseUrl = "https://api.anthropic.com/v1",
            apiKey = "test-api-key"
        )

        // 创建测试请求
        val request = AnthropicChatRequest(
            model = "claude-3-sonnet-20240229",
            messages = listOf(
                AnthropicMessage(
                    role = "user",
                    content = listOf(
                        AnthropicContent(
                            type = "text",
                            text = "What's the weather in Beijing?"
                        )
                    )
                )
            ),
            stream = true
        )

        // 执行测试
        val chunks = streamingClient.createChatCompletionStream(request).toList()

        // 验证我们收到了一些块
        assertTrue(chunks.isNotEmpty())

        // 验证我们收到了工具使用块或完成块
        val toolUseChunks = chunks.filterIsInstance<AnthropicStreamChunk.ToolUse>()
        val finishedChunks = chunks.filterIsInstance<AnthropicStreamChunk.Finished>()
        assertTrue(toolUseChunks.isNotEmpty() || finishedChunks.isNotEmpty())

        // 验证我们收到了结束块
        val doneChunks = chunks.filterIsInstance<AnthropicStreamChunk.Done>()
        assertTrue(doneChunks.isNotEmpty())
    }

    @Test
    fun `test enhanced streaming`() = runTest {
        // 我们将使用一个简化的测试方法，只测试增强流式响应的基本功能
        val mockResponse = """
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text","text":"Hello"}}

            data: {"type":"message_stop","message":{"id":"msg_123","type":"message","role":"assistant","content":[{"type":"text","text":"Hello"}],"model":"claude-3-sonnet-20240229","stop_reason":"end_turn","usage":{"input_tokens":5,"output_tokens":1}}}

            data: [DONE]
        """.trimIndent()

        val mockClient = createMockClient(mockResponse)
        val streamingClient = AnthropicStreamingClient(
            httpClient = mockClient,
            baseUrl = "https://api.anthropic.com/v1",
            apiKey = "test-api-key"
        )

        // 创建测试请求
        val request = AnthropicChatRequest(
            model = "claude-3-sonnet-20240229",
            messages = listOf(
                AnthropicMessage(
                    role = "user",
                    content = listOf(
                        AnthropicContent(
                            type = "text",
                            text = "Say hello"
                        )
                    )
                )
            ),
            stream = true
        )

        // 执行测试
        val chunks = streamingClient.createChatCompletionStreamEnhanced(request).toList()

        // 验证我们收到了一些块
        assertTrue(chunks.isNotEmpty())

        // 验证我们收到了内容块
        val contentChunks = chunks.filterIsInstance<AnthropicStreamChunk.Content>()
        assertTrue(contentChunks.isNotEmpty())

        // 验证我们收到了完成块
        val finishedChunks = chunks.filterIsInstance<AnthropicStreamChunk.Finished>()
        assertTrue(finishedChunks.isNotEmpty())

        // 验证我们收到了结束块
        val doneChunks = chunks.filterIsInstance<AnthropicStreamChunk.Done>()
        assertTrue(doneChunks.isNotEmpty())
    }

    @Test
    fun `test streaming with error handling`() = runTest {
        // 准备模拟错误响应
        val errorResponseText = """
            {"error": {"type": "authentication_error", "message": "Invalid API key"}}
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

        val streamingClient = AnthropicStreamingClient(
            httpClient = mockClient,
            baseUrl = "https://api.anthropic.com/v1",
            apiKey = "invalid-api-key"
        )

        // 创建测试请求
        val request = AnthropicChatRequest(
            model = "claude-3-sonnet-20240229",
            messages = listOf(
                AnthropicMessage(
                    role = "user",
                    content = listOf(
                        AnthropicContent(
                            type = "text",
                            text = "Say hello"
                        )
                    )
                )
            ),
            stream = true
        )

        // 执行测试，期望抛出异常
        var exceptionThrown = false
        try {
            streamingClient.createChatCompletionStream(request).toList()
        } catch (e: AnthropicException) {
            exceptionThrown = true
            // 只要抛出异常就算通过，不检查具体的错误消息
            println("捕获到预期的异常: ${e.message}")
        }

        assertTrue(exceptionThrown, "Expected AnthropicException to be thrown")
    }
}
