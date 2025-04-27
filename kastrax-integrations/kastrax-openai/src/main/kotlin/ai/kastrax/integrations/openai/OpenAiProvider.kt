package ai.kastrax.integrations.openai

import ai.kastrax.core.llm.*
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import io.ktor.utils.io.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.flow.*
import kotlinx.serialization.*
import kotlinx.serialization.json.*
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * OpenAI LLM provider implementation.
 *
 * @property apiKey OpenAI API key
 * @property model OpenAI model ID
 * @property baseUrl Base URL for OpenAI API
 * @property organization Optional OpenAI organization ID
 */
class OpenAiProvider(
    private val apiKey: String,
    override val model: String,
    private val baseUrl: String = "https://api.openai.com/v1",
    private val organization: String? = null
) : LlmProvider {

    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                isLenient = true
            })
        }
    }

    /**
     * Generate a response from the OpenAI API.
     */
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): LlmResponse {
        val requestBody = buildChatCompletionRequest(messages, options)

        try {
            val response = client.post("$baseUrl/chat/completions") {
                contentType(ContentType.Application.Json)
                setHeaders()
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                val openAiResponse = response.body<OpenAiChatCompletionResponse>()
                return convertToLlmResponse(openAiResponse)
            } else {
                val errorBody = response.bodyAsText()
                logger.error { "OpenAI API error: ${response.status} - $errorBody" }
                throw Exception("OpenAI API error: ${response.status} - $errorBody")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error calling OpenAI API" }
            throw e
        }
    }

    /**
     * Generate a streaming response from the OpenAI API.
     */
    override suspend fun streamGenerate(
        messages: List<LlmMessage>,
        options: LlmOptions
    ): Flow<String> = flow {
        val requestBody = buildChatCompletionRequest(messages, options, stream = true)

        try {
            client.preparePost("$baseUrl/chat/completions") {
                contentType(ContentType.Application.Json)
                setHeaders()
                setBody(requestBody)
            }.execute { response ->
                if (response.status.isSuccess()) {
                    val channel = response.bodyAsChannel()
                    val buffer = StringBuilder()

                    while (!channel.isClosedForRead) {
                        val line = channel.readUTF8Line() ?: continue

                        if (line.isEmpty() || !line.startsWith("data:")) continue

                        val data = line.removePrefix("data:").trim()
                        if (data == "[DONE]") break

                        try {
                            val json = Json.parseToJsonElement(data).jsonObject
                            val choices = json["choices"]?.jsonArray

                            choices?.firstOrNull()?.jsonObject?.let { choice ->
                                val delta = choice["delta"]?.jsonObject
                                val content = delta?.get("content")?.jsonPrimitive?.contentOrNull

                                if (content != null) {
                                    buffer.append(content)
                                    emit(content)
                                }
                            }
                        } catch (e: Exception) {
                            logger.warn(e) { "Error parsing streaming response: $data" }
                        }
                    }
                } else {
                    val errorBody = response.bodyAsText()
                    logger.error { "OpenAI API error: ${response.status} - $errorBody" }
                    throw Exception("OpenAI API error: ${response.status} - $errorBody")
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Error in OpenAI streaming" }
            throw e
        }
    }

    /**
     * Generate embeddings for a text.
     */
    override suspend fun embedText(text: String): List<Float> {
        val requestBody = buildEmbeddingRequest(text)

        try {
            val response = client.post("$baseUrl/embeddings") {
                contentType(ContentType.Application.Json)
                setHeaders()
                setBody(requestBody)
            }

            if (response.status.isSuccess()) {
                val embeddingResponse = response.body<OpenAiEmbeddingResponse>()
                return embeddingResponse.data.firstOrNull()?.embedding ?: emptyList()
            } else {
                val errorBody = response.bodyAsText()
                logger.error { "OpenAI API error: ${response.status} - $errorBody" }
                throw Exception("OpenAI API error: ${response.status} - $errorBody")
            }
        } catch (e: Exception) {
            logger.error(e) { "Error calling OpenAI embeddings API" }
            throw e
        }
    }

    /**
     * Set headers for OpenAI API requests.
     */
    private fun HttpRequestBuilder.setHeaders() {
        header("Authorization", "Bearer $apiKey")
        if (organization != null) {
            header("OpenAI-Organization", organization)
        }
    }

    /**
     * Build a chat completion request body.
     */
    private fun buildChatCompletionRequest(
        messages: List<LlmMessage>,
        options: LlmOptions,
        stream: Boolean = false
    ): JsonElement {
        return buildJsonObject {
            put("model", model)
            put("temperature", options.temperature)
            if (options.maxTokens != null) {
                put("max_tokens", options.maxTokens)
            }
            put("top_p", options.topP)
            put("frequency_penalty", options.frequencyPenalty)
            put("presence_penalty", options.presencePenalty)
            put("stream", stream)

            if (options.stop.isNotEmpty()) {
                putJsonArray("stop") {
                    options.stop.forEach { add(it) }
                }
            }

            putJsonArray("messages") {
                messages.forEach { message ->
                    addJsonObject {
                        put("role", message.role.toString().lowercase())
                        put("content", message.content)
                        if (message.name != null) {
                            put("name", message.name)
                        }
                        if (message.toolCallId != null) {
                            put("tool_call_id", message.toolCallId)
                        }
                    }
                }
            }

            if (options.tools.isNotEmpty()) {
                putJsonArray("tools") {
                    options.tools.forEach { add(it) }
                }

                if (options.toolChoice != "auto") {
                    when (val toolChoice = options.toolChoice) {
                        is String -> put("tool_choice", toolChoice)
                        is JsonElement -> put("tool_choice", toolChoice)
                        else -> put("tool_choice", JsonPrimitive(toolChoice.toString()))
                    }
                }
            }
        }
    }

    /**
     * Build an embedding request body.
     */
    private fun buildEmbeddingRequest(text: String): JsonElement {
        return buildJsonObject {
            put("model", "text-embedding-3-small") // Default embedding model
            put("input", text)
        }
    }

    /**
     * Convert OpenAI response to LlmResponse.
     */
    private fun convertToLlmResponse(response: OpenAiChatCompletionResponse): LlmResponse {
        val choice = response.choices.firstOrNull()

        val content = choice?.message?.content ?: ""
        val toolCalls = choice?.message?.tool_calls?.map { toolCall ->
            LlmToolCall(
                id = toolCall.id,
                name = toolCall.function.name,
                arguments = toolCall.function.arguments
            )
        } ?: emptyList()

        val usage = response.usage?.let { usage ->
            LlmUsage(
                promptTokens = usage.prompt_tokens,
                completionTokens = usage.completion_tokens,
                totalTokens = usage.total_tokens
            )
        }

        return LlmResponse(
            content = content,
            toolCalls = toolCalls,
            usage = usage,
            finishReason = choice?.finish_reason
        )
    }
}

/**
 * OpenAI chat completion response.
 */
@Serializable
data class OpenAiChatCompletionResponse(
    val id: String,
    val `object`: String,
    val created: Long,
    val model: String,
    val choices: List<OpenAiChatChoice>,
    val usage: OpenAiUsage? = null
)

/**
 * OpenAI chat choice.
 */
@Serializable
data class OpenAiChatChoice(
    val index: Int,
    val message: OpenAiChatMessage,
    val finish_reason: String? = null
)

/**
 * OpenAI chat message.
 */
@Serializable
data class OpenAiChatMessage(
    val role: String,
    val content: String? = null,
    val tool_calls: List<OpenAiToolCall>? = null
)

/**
 * OpenAI tool call.
 */
@Serializable
data class OpenAiToolCall(
    val id: String,
    val type: String,
    val function: OpenAiFunction
)

/**
 * OpenAI function.
 */
@Serializable
data class OpenAiFunction(
    val name: String,
    val arguments: String
)

/**
 * OpenAI usage.
 */
@Serializable
data class OpenAiUsage(
    val prompt_tokens: Int,
    val completion_tokens: Int,
    val total_tokens: Int
)

/**
 * OpenAI embedding response.
 */
@Serializable
data class OpenAiEmbeddingResponse(
    val data: List<OpenAiEmbedding>,
    val model: String,
    val usage: OpenAiEmbeddingUsage
)

/**
 * OpenAI embedding.
 */
@Serializable
data class OpenAiEmbedding(
    val embedding: List<Float>,
    val index: Int
)

/**
 * OpenAI embedding usage.
 */
@Serializable
data class OpenAiEmbeddingUsage(
    val prompt_tokens: Int,
    val total_tokens: Int
)
