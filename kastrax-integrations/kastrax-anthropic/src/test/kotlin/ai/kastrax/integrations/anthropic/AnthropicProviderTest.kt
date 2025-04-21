package ai.kastrax.integrations.anthropic

import ai.kastrax.core.llm.*
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
 * AnthropicProvider 的单元测试。
 */
class AnthropicProviderTest {

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
                                    HttpHeaders.ContentType to listOf("application/json")
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
    fun `test generate`() = runTest {
        // 准备模拟响应
        val mockResponse = AnthropicChatResponse(
            id = "msg_123",
            model = "claude-3-sonnet-20240229",
            type = "message",
            role = "assistant",
            content = listOf(
                AnthropicContent(
                    type = "text",
                    text = "This is a test response"
                )
            ),
            usage = AnthropicUsage(
                inputTokens = 10,
                outputTokens = 5
            ),
            stopReason = "end_turn",
            stopSequence = null
        )

        val mockClient = createMockClient(json.encodeToString(mockResponse))
        val provider = AnthropicProvider(
            model = "claude-3-sonnet-20240229",
            apiKey = "test-api-key",
            client = AnthropicClient(
                apiKey = "test-api-key",
                httpClient = mockClient
            )
        )

        // 创建测试消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "Hello, Claude!"
            )
        )

        // 执行测试
        val response = provider.generate(messages, LlmOptions())

        // 验证结果
        assertEquals("This is a test response", response.content)
        assertEquals("end_turn", response.finishReason)
        assertEquals(10, response.usage?.promptTokens)
        assertEquals(5, response.usage?.completionTokens)
        assertEquals(15, response.usage?.totalTokens)
    }

    @Test
    fun `test generate with system message`() = runTest {
        // 准备模拟响应
        val mockResponse = AnthropicChatResponse(
            id = "msg_123",
            model = "claude-3-sonnet-20240229",
            type = "message",
            role = "assistant",
            content = listOf(
                AnthropicContent(
                    type = "text",
                    text = "This is a test response with system message"
                )
            ),
            usage = AnthropicUsage(
                inputTokens = 15,
                outputTokens = 8
            ),
            stopReason = "end_turn",
            stopSequence = null
        )

        val mockClient = createMockClient(json.encodeToString(mockResponse))
        val provider = AnthropicProvider(
            model = "claude-3-sonnet-20240229",
            apiKey = "test-api-key",
            client = AnthropicClient(
                apiKey = "test-api-key",
                httpClient = mockClient
            )
        )

        // 创建测试消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "You are a helpful assistant."
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "Hello, Claude!"
            )
        )

        // 执行测试
        val response = provider.generate(messages, LlmOptions())

        // 验证结果
        assertEquals("This is a test response with system message", response.content)
        assertEquals("end_turn", response.finishReason)
    }

    @Test
    fun `test generate with tool calls`() = runTest {
        // 准备模拟响应
        val mockResponse = AnthropicChatResponse(
            id = "msg_123",
            model = "claude-3-sonnet-20240229",
            type = "message",
            role = "assistant",
            content = listOf(
                AnthropicContent(
                    type = "text",
                    text = ""
                ),
                AnthropicContent(
                    type = "tool_use",
                    toolUse = AnthropicToolUse(
                        id = "call_123",
                        name = "get_weather",
                        input = kotlinx.serialization.json.JsonObject(
                            mapOf(
                                "location" to kotlinx.serialization.json.JsonPrimitive("Beijing"),
                                "unit" to kotlinx.serialization.json.JsonPrimitive("celsius")
                            )
                        )
                    )
                )
            ),
            usage = AnthropicUsage(
                inputTokens = 20,
                outputTokens = 10
            ),
            stopReason = "tool_use",
            stopSequence = null
        )

        val mockClient = createMockClient(json.encodeToString(mockResponse))
        val provider = AnthropicProvider(
            model = "claude-3-sonnet-20240229",
            apiKey = "test-api-key",
            client = AnthropicClient(
                apiKey = "test-api-key",
                httpClient = mockClient
            )
        )

        // 创建测试消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "What's the weather in Beijing?"
            )
        )

        // 执行测试
        val response = provider.generate(messages, LlmOptions())

        // 验证结果
        assertEquals("", response.content) // 工具调用时内容可能为空
        assertEquals("tool_use", response.finishReason)
        assertEquals(1, response.toolCalls.size)
        assertEquals("call_123", response.toolCalls[0].id)
        assertEquals("get_weather", response.toolCalls[0].name)
        assertTrue(response.toolCalls[0].arguments.contains("Beijing"))
        assertTrue(response.toolCalls[0].arguments.contains("celsius"))
    }

    @Test
    fun `test stream generate`() = runTest {
        // 准备模拟流式响应
        val mockResponses = listOf(
            """
            data: {"type":"content_block_start","index":0,"content_block":{"type":"text","text":""}}
            """.trimIndent(),
            """
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text","text":"This"}}
            """.trimIndent(),
            """
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text","text":" is"}}
            """.trimIndent(),
            """
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text","text":" a"}}
            """.trimIndent(),
            """
            data: {"type":"content_block_delta","index":0,"delta":{"type":"text","text":" test"}}
            """.trimIndent(),
            """
            data: {"type":"message_stop","message":{"id":"msg_123","type":"message","role":"assistant","content":[{"type":"text","text":"This is a test"}],"model":"claude-3-sonnet-20240229","stop_reason":"end_turn","usage":{"input_tokens":10,"output_tokens":4}}}
            """.trimIndent(),
            """
            data: [DONE]
            """.trimIndent()
        ).joinToString("\n")

        val mockClient = createMockClient(mockResponses)
        val provider = AnthropicProvider(
            model = "claude-3-sonnet-20240229",
            apiKey = "test-api-key",
            client = AnthropicClient(
                apiKey = "test-api-key",
                httpClient = mockClient
            ),
            streamingClient = AnthropicStreamingClient(
                baseUrl = "https://api.anthropic.com/v1",
                apiKey = "test-api-key",
                httpClient = mockClient
            )
        )

        // 创建测试消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "Hello, Claude!"
            )
        )

        // 执行测试
        val streamResult = provider.streamGenerate(messages, LlmOptions()).toList()

        // 验证结果
        // 注意：由于流式响应的实现可能会将字符分割成单独的块，所以我们只验证最终结果
        val fullText = streamResult.joinToString("")
        assertEquals("This is a test", fullText)
    }
}
