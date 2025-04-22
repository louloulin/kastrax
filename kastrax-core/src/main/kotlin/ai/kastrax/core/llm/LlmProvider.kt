package ai.kastrax.core.llm

import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.json.JsonElement

/**
 * Interface for LLM providers.
 * Defines methods for generating text and embeddings.
 */
interface LlmProvider {
    /**
     * The model identifier.
     */
    val model: String

    /**
     * Generate a response from the LLM.
     *
     * @param messages List of messages to send to the LLM
     * @param options Options for the generation
     * @return LLM response
     */
    suspend fun generate(
        messages: List<LlmMessage>,
        options: LlmOptions = LlmOptions()
    ): LlmResponse

    /**
     * Generate a streaming response from the LLM.
     *
     * @param messages List of messages to send to the LLM
     * @param options Options for the generation
     * @return Flow of text chunks
     */
    suspend fun streamGenerate(
        messages: List<LlmMessage>,
        options: LlmOptions = LlmOptions()
    ): Flow<String>

    /**
     * Generate embeddings for a text.
     *
     * @param text Text to embed
     * @return List of embedding values
     */
    suspend fun embedText(text: String): List<Float>
}

/**
 * Enum for LLM message roles.
 */
enum class LlmMessageRole {
    SYSTEM,
    USER,
    ASSISTANT,
    TOOL
}

/**
 * Data class for LLM messages.
 *
 * @property role The role of the message sender
 * @property content The message content
 * @property name Optional name for the message sender
 * @property toolCalls Optional tool calls in the message
 * @property toolCallId Optional tool call ID if this is a tool response
 */
data class LlmMessage(
    val role: LlmMessageRole,
    val content: String,
    val name: String? = null,
    val toolCalls: List<LlmToolCall> = emptyList(),
    val toolCallId: String? = null
)

/**
 * Data class for LLM tool calls.
 *
 * @property id The tool call ID
 * @property name The tool name
 * @property arguments The tool arguments as a JSON string
 */
data class LlmToolCall(
    val id: String,
    val name: String,
    val arguments: String
)

/**
 * Data class for LLM generation options.
 *
 * @property temperature Controls randomness (0.0 to 1.0)
 * @property maxTokens Maximum number of tokens to generate
 * @property topP Controls diversity via nucleus sampling
 * @property frequencyPenalty Penalizes frequent tokens
 * @property presencePenalty Penalizes repeated tokens
 * @property stop List of sequences where the LLM should stop generating
 * @property tools List of tools available to the LLM
 * @property toolChoice How the model should use tools (can be a string or a JSON object)
 * @property responseFormat Controls the format of the response
 * @property seed Random seed for deterministic results
 */
data class LlmOptions(
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val topP: Double? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val stop: List<String> = emptyList(),
    val tools: List<JsonElement> = emptyList(),
    val toolChoice: Any = "auto",
    val responseFormat: JsonElement? = null,
    val seed: Long? = null
)

/**
 * Data class for LLM responses.
 *
 * @property content The generated text content
 * @property toolCalls List of tool calls in the response
 * @property usage Token usage information
 * @property finishReason Reason why the LLM stopped generating
 */
data class LlmResponse(
    val content: String,
    val toolCalls: List<LlmToolCall> = emptyList(),
    val usage: LlmUsage? = null,
    val finishReason: String? = null
)

/**
 * Data class for LLM token usage.
 *
 * @property promptTokens Number of tokens in the prompt
 * @property completionTokens Number of tokens in the completion
 * @property totalTokens Total number of tokens used
 */
data class LlmUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int
)

/**
 * Enum for LLM provider types.
 */
enum class LlmProviderType {
    OPENAI,
    ANTHROPIC,
    GEMINI,
    MISTRAL,
    CUSTOM
}
