package ai.kastrax.core.agent

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.*
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.ToolCallResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.*
import java.util.UUID

/**
 * Interface for AI agents.
 */
interface Agent {
    /**
     * The agent's name.
     */
    val name: String

    /**
     * Generate a response from the agent.
     *
     * @param messages List of messages for the conversation
     * @param options Options for generation
     * @return Agent response
     */
    suspend fun generate(
        messages: List<LlmMessage>,
        options: AgentGenerateOptions = AgentGenerateOptions()
    ): AgentResponse

    /**
     * Generate a response from the agent using a single prompt.
     *
     * @param prompt Text prompt for the agent
     * @param options Options for generation
     * @return Agent response
     */
    suspend fun generate(
        prompt: String,
        options: AgentGenerateOptions = AgentGenerateOptions()
    ): AgentResponse

    /**
     * Stream a response from the agent.
     *
     * @param prompt Text prompt for the agent
     * @param options Options for streaming
     * @return Agent response with streaming
     */
    suspend fun stream(
        prompt: String,
        options: AgentStreamOptions = AgentStreamOptions()
    ): AgentResponse

    /**
     * Reset the agent's state.
     */
    suspend fun reset()
}

/**
 * Options for agent generation.
 *
 * @property maxSteps Maximum number of steps (for tool use)
 * @property temperature Controls randomness (0.0 to 1.0)
 * @property maxTokens Maximum number of tokens to generate
 * @property executeTools Whether to execute tools
 * @property output Optional schema for structured output
 * @property onStepFinish Callback for step completion
 */
data class AgentGenerateOptions(
    val maxSteps: Int = 1,
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val executeTools: Boolean = true,
    val output: JsonElement? = null,
    val onStepFinish: ((StepResult) -> Unit)? = null
) {
    /**
     * Convert to LLM options.
     */
    val llmOptions: LlmOptions
        get() = LlmOptions(
            temperature = temperature,
            maxTokens = maxTokens
        )
}

/**
 * Options for agent streaming.
 *
 * @property threadId Optional thread ID for memory
 * @property resourceId Optional resource ID
 * @property threadTitle Optional title for new threads
 * @property temperature Controls randomness (0.0 to 1.0)
 * @property maxTokens Maximum number of tokens to generate
 */
data class AgentStreamOptions(
    val threadId: String? = null,
    val resourceId: String? = null,
    val threadTitle: String? = null,
    val temperature: Double = 0.7,
    val maxTokens: Int? = null
) {
    /**
     * Convert to LLM options.
     */
    val llmOptions: LlmOptions
        get() = LlmOptions(
            temperature = temperature,
            maxTokens = maxTokens
        )
}

/**
 * Result of a step in agent execution.
 *
 * @property text Generated text
 * @property toolCalls Tool calls made
 * @property toolResults Results of tool executions
 */
data class StepResult(
    val text: String,
    val toolCalls: List<LlmToolCall> = emptyList(),
    val toolResults: Map<String, ToolCallResult> = emptyMap()
)

/**
 * Agent response.
 *
 * @property text Generated text
 * @property toolCalls Tool calls made
 * @property toolResults Results of tool executions
 * @property usage Token usage information
 * @property result Structured result (if requested)
 * @property textStream Stream of text chunks (for streaming)
 * @property threadId Thread ID (if using memory)
 */
data class AgentResponse(
    val text: String = "",
    val toolCalls: List<LlmToolCall> = emptyList(),
    val toolResults: Map<String, ToolCallResult> = emptyMap(),
    val usage: LlmUsage? = null,
    val result: Any? = null,
    val textStream: Flow<String>? = null,
    val threadId: String? = null
)

/**
 * Builder for creating agents.
 */
class AgentBuilder {
    var name: String = ""
    var instructions: String = ""
    lateinit var model: LlmProvider
    var tools: MutableMap<String, Tool> = mutableMapOf()

    /**
     * Add tools to the agent.
     */
    fun tools(init: ToolsBuilder.() -> Unit) {
        val builder = ToolsBuilder()
        builder.init()
        tools.putAll(builder.tools)
    }

    /**
     * Builder for adding tools.
     */
    class ToolsBuilder {
        val tools: MutableMap<String, Tool> = mutableMapOf()

        /**
         * Add a tool.
         */
        fun tool(tool: Tool) {
            tools[tool.id] = tool
        }

        /**
         * Add a tool with an ID.
         */
        fun tool(id: String, tool: Tool) {
            tools[id] = tool
        }
    }

    /**
     * Build the agent.
     */
    fun build(): LLMAgent {
        require(name.isNotEmpty()) { "Agent name must not be empty" }
        require(::model.isInitialized) { "Agent model must be defined" }

        return LLMAgent(
            name = name,
            instructions = instructions,
            model = model,
            tools = tools
        )
    }
}

/**
 * Implementation of an agent using an LLM.
 *
 * @property name Agent name
 * @property instructions Instructions/system prompt for the agent
 * @property model LLM provider
 * @property tools Available tools
 */
class LLMAgent(
    name: String,
    val instructions: String,
    val model: LlmProvider,
    val tools: Map<String, Tool> = emptyMap()
) : KastraXBase(component = "AGENT", name = name), Agent {

    /**
     * Generate a response from multiple messages.
     */
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: AgentGenerateOptions
    ): AgentResponse {
        // Prepare messages with system instructions
        val allMessages = if (messages.isNotEmpty() && messages[0].role == LlmMessageRole.SYSTEM) {
            messages
        } else {
            listOf(LlmMessage(role = LlmMessageRole.SYSTEM, content = instructions)) + messages
        }

        // Generate response from LLM
        val llmOptions = options.llmOptions.copy(
            tools = tools.values.map { tool ->
                buildJsonObject {
                    put("type", JsonPrimitive("function"))
                    put("function", buildJsonObject {
                        put("name", JsonPrimitive(tool.id))
                        put("description", JsonPrimitive(tool.description))
                        put("parameters", tool.inputSchema)
                    })
                }
            }
        )

        val response = model.generate(allMessages, llmOptions)

        // Handle tool calls if present and execution is enabled
        val toolResults = if (options.executeTools && response.toolCalls.isNotEmpty()) {
            executeToolCalls(response.toolCalls)
        } else {
            emptyMap()
        }

        // Create step result for callback
        val stepResult = StepResult(
            text = response.content,
            toolCalls = response.toolCalls,
            toolResults = toolResults
        )

        // Call step finish callback if provided
        options.onStepFinish?.invoke(stepResult)

        return AgentResponse(
            text = response.content,
            toolCalls = response.toolCalls,
            toolResults = toolResults,
            usage = response.usage
        )
    }

    /**
     * Generate a response from a single prompt.
     */
    override suspend fun generate(
        prompt: String,
        options: AgentGenerateOptions
    ): AgentResponse {
        val messages = listOf(
            LlmMessage(role = LlmMessageRole.USER, content = prompt)
        )
        return generate(messages, options)
    }

    /**
     * Stream a response.
     */
    override suspend fun stream(
        prompt: String,
        options: AgentStreamOptions
    ): AgentResponse {
        val threadId = options.threadId ?: UUID.randomUUID().toString()

        // Prepare messages
        val messages = listOf(
            LlmMessage(role = LlmMessageRole.SYSTEM, content = instructions),
            LlmMessage(role = LlmMessageRole.USER, content = prompt)
        )

        // Stream response from LLM
        val responseStream = model.streamGenerate(messages, options.llmOptions)

        // Create response flow that collects the full text
        val processedStream = flow {
            val responseBuilder = StringBuilder()

            responseStream.collect { chunk ->
                responseBuilder.append(chunk)
                emit(chunk)
            }
        }

        return AgentResponse(
            textStream = processedStream,
            threadId = threadId
        )
    }

    /**
     * Reset the agent's state.
     */
    override suspend fun reset() {
        logger.debug { "Resetting agent state" }
    }

    /**
     * Execute tool calls.
     */
    private suspend fun executeToolCalls(
        toolCalls: List<LlmToolCall>
    ): Map<String, ToolCallResult> {
        if (toolCalls.isEmpty()) {
            return emptyMap()
        }

        val results = mutableMapOf<String, ToolCallResult>()

        for (toolCall in toolCalls) {
            try {
                val toolId = toolCall.name
                val tool = tools[toolId]

                if (tool != null) {
                    // Parse arguments as JSON
                    val arguments = try {
                        Json.parseToJsonElement(toolCall.arguments)
                    } catch (e: Exception) {
                        logger.warn { "Failed to parse tool arguments as JSON: ${toolCall.arguments}" }
                        JsonObject(emptyMap())
                    }

                    // Execute the tool
                    val result = tool.execute(arguments)

                    results[toolCall.id] = ToolCallResult(
                        success = true,
                        result = result
                    )
                } else {
                    results[toolCall.id] = ToolCallResult(
                        success = false,
                        error = "Tool not found: $toolId"
                    )
                }
            } catch (e: Exception) {
                logger.error(e) { "Error executing tool call: ${toolCall.name}" }
                results[toolCall.id] = ToolCallResult(
                    success = false,
                    error = "Error executing tool: ${e.message}"
                )
            }
        }

        return results
    }
}

/**
 * DSL function for creating an agent.
 */
fun agent(init: AgentBuilder.() -> Unit): Agent {
    val builder = AgentBuilder()
    builder.init()
    return builder.build()
}
