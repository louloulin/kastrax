package ai.kastrax.integrations.deepseek

import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DeepSeekProviderTest {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = false
    }

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
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        "https://api.deepseek.com/v1/embeddings" -> {
                            respond(
                                content = """
                                {
                                    "object": "list",
                                    "data": [
                                        {
                                            "object": "embedding",
                                            "embedding": [0.1, 0.2, 0.3],
                                            "index": 0
                                        }
                                    ],
                                    "model": "deepseek-embedding",
                                    "usage": {
                                        "prompt_tokens": 5,
                                        "total_tokens": 5
                                    }
                                }
                                """.trimIndent(),
                                status = HttpStatusCode.OK,
                                headers = headersOf(HttpHeaders.ContentType, "application/json")
                            )
                        }
                        else -> error("Unhandled ${request.url}")
                    }
                }
            }
        }
    }

    @Test
    fun `test generate completion`() = runTest {
        // 准备模拟响应
        val mockResponse = DeepSeekChatCompletionResponse(
            id = "test-id",
            objectType = "chat.completion",
            created = 1677858242,
            model = "deepseek-chat",
            systemFingerprint = "fp123",
            choices = listOf(
                DeepSeekChoice(
                    index = 0,
                    message = DeepSeekMessage(
                        role = "assistant",
                        content = "This is a test response"
                    ),
                    finishReason = "stop"
                )
            ),
            usage = DeepSeekUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15
            )
        )

        val mockClient = createMockClient(json.encodeToString(mockResponse))
        val deepSeekClient = DeepSeekClient("test-api-key", httpClient = mockClient)
        val provider = DeepSeekProvider("deepseek-chat", "test-api-key", deepSeekClient)

        // 创建测试消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "Hello, DeepSeek!"
            )
        )

        // 执行测试
        val response = provider.generate(messages, LlmOptions())

        // 验证结果
        assertEquals("This is a test response", response.content)
        assertEquals(10, response.usage?.promptTokens)
        assertEquals(5, response.usage?.completionTokens)
        assertEquals(15, response.usage?.totalTokens)
    }

    @Test
    fun `test generate completion with tool calls`() = runTest {
        // 准备带工具调用的模拟响应
        val mockResponse = DeepSeekChatCompletionResponse(
            id = "test-id-tool",
            objectType = "chat.completion",
            created = 1677858242,
            model = "deepseek-chat",
            systemFingerprint = "fp123",
            choices = listOf(
                DeepSeekChoice(
                    index = 0,
                    message = DeepSeekMessage(
                        role = "assistant",
                        content = null,
                        toolCalls = listOf(
                            DeepSeekToolCall(
                                id = "call_123",
                                type = "function",
                                function = DeepSeekFunctionCall(
                                    name = "get_weather",
                                    arguments = "{\"location\":\"Beijing\",\"unit\":\"celsius\"}"
                                )
                            )
                        )
                    ),
                    finishReason = "tool_calls"
                )
            ),
            usage = DeepSeekUsage(
                promptTokens = 12,
                completionTokens = 8,
                totalTokens = 20
            )
        )

        val mockClient = createMockClient(json.encodeToString(mockResponse))
        val deepSeekClient = DeepSeekClient("test-api-key", httpClient = mockClient)
        val provider = DeepSeekProvider("deepseek-chat", "test-api-key", deepSeekClient)

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
        assertEquals("tool_calls", response.finishReason)
        assertEquals(1, response.toolCalls.size)
        assertEquals("call_123", response.toolCalls[0].id)
        assertEquals("get_weather", response.toolCalls[0].name)
        assertEquals("{\"location\":\"Beijing\",\"unit\":\"celsius\"}", response.toolCalls[0].arguments)
    }

    @Test
    fun `test stream generate`() = runTest {
        // 准备模拟流式响应
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
                            content = "This"
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
                            content = " is"
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
                            content = " a"
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
                            content = " test"
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
        }

        val mockClient = createMockClient(mockResponseText)
        val deepSeekClient = DeepSeekClient("test-api-key", httpClient = mockClient)
        val provider = DeepSeekProvider("deepseek-chat", "test-api-key", deepSeekClient)

        // 创建测试消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "Hello, DeepSeek!"
            )
        )

        // 执行测试
        val streamResult = provider.streamGenerate(messages, LlmOptions()).toList()

        // 验证结果
        assertEquals(4, streamResult.size)
        assertEquals("This", streamResult[0])
        assertEquals(" is", streamResult[1])
        assertEquals(" a", streamResult[2])
        assertEquals(" test", streamResult[3])
    }

    @Test
    fun `test embed text`() = runTest {
        val mockClient = createMockClient("")  // 使用默认的嵌入响应
        val deepSeekClient = DeepSeekClient("test-api-key", httpClient = mockClient)
        val provider = DeepSeekProvider("deepseek-embedding", "test-api-key", deepSeekClient)

        // 执行测试
        val embedding = provider.embedText("Test text")

        // 验证结果
        assertNotNull(embedding)
        assertEquals(3, embedding.size)
        assertTrue(embedding.contains(0.1f))
        assertTrue(embedding.contains(0.2f))
        assertTrue(embedding.contains(0.3f))
    }

    @Test
    fun `test DSL creation`() {
        // 测试简单创建
        val provider1 = deepSeek(
            model = "deepseek-chat",
            apiKey = "test-api-key"
        )
        assertEquals("deepseek-chat", provider1.model)

        // 测试 DSL 创建
        val provider2 = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CODER)
            apiKey("test-api-key")
        }
        assertEquals("deepseek-coder", provider2.model)

        // 测试自定义模型 ID
        val provider3 = deepSeek {
            model("custom-model-id")
            apiKey("test-api-key")
        }
        assertEquals("custom-model-id", provider3.model)
    }
}
