package ai.kastrax.integrations.gemini

import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Google Gemini API 客户端，用于与 Gemini API 进行交互。
 *
 * @property apiKey Google API 密钥
 * @property baseUrl Gemini API 基础 URL
 * @property httpClient HTTP 客户端

 */
class GeminiClient(
    private val apiKey: String,
    private val baseUrl: String = "https://generativelanguage.googleapis.com/v1",
    private val httpClient: HttpClient = createDefaultHttpClient()
) {
    /**
     * 创建聊天完成。
     *
     * @param model 模型名称
     * @param request 聊天完成请求
     * @return 聊天完成响应
     */
    suspend fun createChatCompletion(model: String, request: GeminiChatRequest): GeminiChatResponse {
        logger.debug { "Creating chat completion with model: $model" }

        return try {
            val response = httpClient.post("$baseUrl/models/$model:generateContent?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            response.body<GeminiChatResponse>()
        } catch (e: Exception) {
            logger.error(e) { "Error creating chat completion: ${e.message}" }
            throw handleApiError(e)
        }
    }

    /**
     * 创建流式聊天完成。
     *
     * @param model 模型名称
     * @param request 聊天完成请求
     * @return 流式响应块流
     */
    suspend fun createChatCompletionStream(model: String, request: GeminiChatRequest): Flow<GeminiStreamChunk> {
        logger.debug { "Creating streaming chat completion with model: $model" }

        return try {
            val response = httpClient.post("$baseUrl/models/$model:streamGenerateContent?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            if (!response.status.isSuccess()) {
                throw GeminiException("Gemini API error: ${response.status.description}")
            }

            // 解析 SSE 流
            parseStreamResponse(response)
        } catch (e: Exception) {
            logger.error(e) { "Error creating streaming chat completion: ${e.message}" }
            flow { throw handleApiError(e) }
        }
    }

    /**
     * 创建嵌入。
     *
     * @param model 模型名称
     * @param request 嵌入请求
     * @return 嵌入响应
     */
    suspend fun createEmbedding(model: String, request: GeminiEmbeddingRequest): GeminiEmbeddingResponse {
        logger.debug { "Creating embedding with model: $model" }

        return try {
            val response = httpClient.post("$baseUrl/models/$model:embedContent?key=$apiKey") {
                contentType(ContentType.Application.Json)
                setBody(request)
            }

            response.body<GeminiEmbeddingResponse>()
        } catch (e: Exception) {
            logger.error(e) { "Error creating embedding: ${e.message}" }
            throw handleApiError(e)
        }
    }

    /**
     * 解析流式响应。
     *
     * @param response HTTP 响应
     * @return 流式响应块流
     */
    private fun parseStreamResponse(response: HttpResponse): Flow<GeminiStreamChunk> = flow {
        val json = Json { ignoreUnknownKeys = true }

        response.bodyAsChannel().apply {
            while (!isClosedForRead) {
                val line = readUTF8Line(Int.MAX_VALUE) ?: continue

                if (line.isEmpty() || !line.startsWith("data:")) continue

                val data = line.removePrefix("data:").trim()
                if (data == "[DONE]") {
                    emit(GeminiStreamChunk.Done)
                    break
                }

                try {
                    val streamResponse = json.decodeFromString<GeminiStreamResponse>(data)

                    streamResponse.candidates?.forEach { candidate ->
                        candidate.content.parts.forEach { part ->
                            part.text?.let { text ->
                                if (text.isNotEmpty()) {
                                    emit(GeminiStreamChunk.Content(text))
                                }
                            }
                        }

                        if (candidate.finishReason != null) {
                            emit(GeminiStreamChunk.Finished(candidate.finishReason))
                        }
                    }
                } catch (e: Exception) {
                    logger.warn { "Failed to parse stream response: $data" }
                    logger.warn(e) { "Parse error: ${e.message}" }
                }
            }
        }
    }

    /**
     * 处理 API 错误。
     *
     * @param e 异常
     * @return Gemini 异常
     */
    private fun handleApiError(e: Exception): GeminiException {
        return when (e) {
            is ClientRequestException -> {
                val status = e.response.status
                val message = when (status) {
                    HttpStatusCode.Unauthorized -> "Invalid API key or unauthorized access"
                    HttpStatusCode.BadRequest -> "Bad request: ${e.message}"
                    HttpStatusCode.TooManyRequests -> "Rate limit exceeded"
                    else -> "Gemini API error: ${status.description}"
                }
                GeminiException(message, e)
            }
            is ServerResponseException -> {
                GeminiException("Gemini server error: ${e.message}", e)
            }
            else -> GeminiException("Unexpected error: ${e.message}", e)
        }
    }

    companion object {
        /**
         * 创建默认的 HTTP 客户端。
         *
         * @return HTTP 客户端
         */
        fun createDefaultHttpClient(): HttpClient {
            return HttpClient(CIO) {
                install(ContentNegotiation) {
                    json(Json {
                        ignoreUnknownKeys = true
                        isLenient = true
                        encodeDefaults = false
                    })
                }

                install(HttpTimeout) {
                    requestTimeoutMillis = 60000
                    connectTimeoutMillis = 30000
                    socketTimeoutMillis = 60000
                }

                expectSuccess = true
            }
        }
    }
}
