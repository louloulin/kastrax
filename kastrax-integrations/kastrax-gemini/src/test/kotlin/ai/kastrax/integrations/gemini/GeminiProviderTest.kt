package ai.kastrax.integrations.gemini

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
 * GeminiProvider 的单元测试。
 */
class GeminiProviderTest {

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
                    when {
                        request.url.toString().contains("generateContent") -> {
                            respond(
                                content = responseContent,
                                status = HttpStatusCode.OK,
                                headers = headersOf(
                                    HttpHeaders.ContentType to listOf("application/json")
                                )
                            )
                        }
                        request.url.toString().contains("streamGenerateContent") -> {
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
                        request.url.toString().contains("embedContent") -> {
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
        val mockResponse = GeminiChatResponse(
            candidates = listOf(
                GeminiCandidate(
                    content = GeminiContent(
                        role = "model",
                        parts = listOf(
                            GeminiPart(
                                text = "This is a test response"
                            )
                        )
                    ),
                    finishReason = "STOP",
                    index = 0,
                    safetyRatings = emptyList()
                )
            ),
            usage = GeminiUsage(
                promptTokenCount = 10,
                candidatesTokenCount = 5,
                totalTokenCount = 15
            )
        )

        val mockClient = createMockClient(json.encodeToString(mockResponse))
        val provider = GeminiProvider(
            model = "gemini-1.5-pro",
            apiKey = "test-api-key",
            client = GeminiClient(
                apiKey = "test-api-key",
                httpClient = mockClient
            )
        )

        // 创建测试消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "Hello, Gemini!"
            )
        )

        // 执行测试
        val response = provider.generate(messages, LlmOptions())

        // 验证结果
        assertEquals("This is a test response", response.content)
        assertEquals("STOP", response.finishReason)
        assertEquals(10, response.usage?.promptTokens)
        assertEquals(5, response.usage?.completionTokens)
        assertEquals(15, response.usage?.totalTokens)
    }

    @Test
    fun `test generate with system message`() = runTest {
        // 准备模拟响应
        val mockResponse = GeminiChatResponse(
            candidates = listOf(
                GeminiCandidate(
                    content = GeminiContent(
                        role = "model",
                        parts = listOf(
                            GeminiPart(
                                text = "This is a test response with system message"
                            )
                        )
                    ),
                    finishReason = "STOP",
                    index = 0,
                    safetyRatings = emptyList()
                )
            ),
            usage = GeminiUsage(
                promptTokenCount = 15,
                candidatesTokenCount = 8,
                totalTokenCount = 23
            )
        )

        val mockClient = createMockClient(json.encodeToString(mockResponse))
        val provider = GeminiProvider(
            model = "gemini-1.5-pro",
            apiKey = "test-api-key",
            client = GeminiClient(
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
                content = "Hello, Gemini!"
            )
        )

        // 执行测试
        val response = provider.generate(messages, LlmOptions())

        // 验证结果
        assertEquals("This is a test response with system message", response.content)
        assertEquals("STOP", response.finishReason)
    }

    @Test
    fun `test stream generate`() = runTest {
        // 准备模拟流式响应
        val mockResponse = """
            data: {"candidates":[{"content":{"parts":[{"text":"This"}],"role":"model"},"index":0}]}
            
            data: {"candidates":[{"content":{"parts":[{"text":" is"}],"role":"model"},"index":0}]}
            
            data: {"candidates":[{"content":{"parts":[{"text":" a"}],"role":"model"},"index":0}]}
            
            data: {"candidates":[{"content":{"parts":[{"text":" test"}],"role":"model"},"index":0}]}
            
            data: {"candidates":[{"content":{"parts":[{"text":""}],"role":"model"},"finishReason":"STOP","index":0}],"usageMetadata":{"promptTokenCount":10,"candidatesTokenCount":4,"totalTokenCount":14}}
            
            data: [DONE]
        """.trimIndent()

        val mockClient = createMockClient(mockResponse)
        val provider = GeminiProvider(
            model = "gemini-1.5-pro",
            apiKey = "test-api-key",
            client = GeminiClient(
                apiKey = "test-api-key",
                httpClient = mockClient
            ),
            streamingClient = GeminiStreamingClient(
                baseUrl = "https://generativelanguage.googleapis.com/v1",
                apiKey = "test-api-key",
                httpClient = mockClient
            )
        )

        // 创建测试消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "Hello, Gemini!"
            )
        )

        // 执行测试
        val streamResult = provider.streamGenerate(messages, LlmOptions()).toList()

        // 验证结果
        // 注意：由于流式响应的实现可能会将字符分割成单独的块，所以我们只验证最终结果
        val fullText = streamResult.joinToString("")
        assertEquals("This is a test", fullText)
    }

    @Test
    fun `test embedText`() = runTest {
        // 准备模拟响应
        val mockResponse = GeminiEmbeddingResponse(
            embedding = GeminiEmbedding(
                values = listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f)
            )
        )

        val mockClient = createMockClient(json.encodeToString(mockResponse))
        val provider = GeminiProvider(
            model = "gemini-1.5-pro",
            apiKey = "test-api-key",
            client = GeminiClient(
                apiKey = "test-api-key",
                httpClient = mockClient
            )
        )

        // 执行测试
        val embedding = provider.embedText("Test text for embedding")

        // 验证结果
        assertEquals(5, embedding.size)
        assertEquals(listOf(0.1f, 0.2f, 0.3f, 0.4f, 0.5f), embedding)
    }
}
