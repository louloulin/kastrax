package ai.kastrax.core.agent

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmToolCall
import ai.kastrax.core.llm.LlmUsage
import ai.kastrax.core.memory.toMessage
import ai.kastrax.core.memory.toLlmMessage
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.ToolCallResult
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryBuilder
import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.MessageRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
 * @property threadId Optional thread ID for memory
 * @property threadTitle Optional title for new threads
 */
data class AgentGenerateOptions(
    val maxSteps: Int = 1,
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val executeTools: Boolean = true,
    val output: JsonElement? = null,
    val onStepFinish: ((StepResult) -> Unit)? = null,
    val threadId: String? = null,
    val threadTitle: String? = null
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
    var memory: ai.kastrax.memory.api.Memory? = null

    /**
     * Add tools to the agent.
     */
    fun tools(init: ToolsBuilder.() -> Unit) {
        val builder = ToolsBuilder()
        builder.init()
        tools.putAll(builder.tools)
    }

    /**
     * Configure memory for the agent.
     */
    fun memory(memory: Memory) {
        this.memory = memory
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
            tools = tools,
            memory = memory
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
 * @property memory Optional memory system for conversation history
 */
class LLMAgent(
    name: String,
    val instructions: String,
    val model: LlmProvider,
    val tools: Map<String, Tool> = emptyMap(),
    val memory: ai.kastrax.memory.api.Memory? = null
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
        // 创建用户消息
        val userMessage = LlmMessage(role = LlmMessageRole.USER, content = prompt)

        // 如果有内存系统，创建或使用现有线程
        val threadId = options.threadId ?: if (memory != null) {
            memory.createThread(options.threadTitle)
        } else {
            null
        }

        // 如果有内存系统和线程ID，保存用户消息
        if (memory != null && threadId != null) {
            // 保存用户消息
            memory.saveMessage(userMessage.toMessage(), threadId)

            // 获取历史消息
            val historyMessages = memory.getMessages(threadId).map { it.message.toLlmMessage() }

            // 生成响应
            val response = generate(historyMessages.toList(), options)

            // 保存助手消息
            val assistantMessage = LlmMessage(
                role = LlmMessageRole.ASSISTANT,
                content = response.text,
                toolCalls = response.toolCalls
            )
            memory.saveMessage(assistantMessage.toMessage(), threadId)

            // 返回带有线程ID的响应
            return response.copy(threadId = threadId)
        } else {
            // 如果没有内存系统，直接生成响应
            val messages = listOf(userMessage)
            return generate(messages, options)
        }
    }

    /**
     * Stream a response.
     */
    override suspend fun stream(
        prompt: String,
        options: AgentStreamOptions
    ): AgentResponse {
        // 使用提供的线程ID或创建新的线程ID
        val threadId = options.threadId ?: if (memory != null) {
            // 如果有内存系统，创建新线程
            memory.createThread(options.threadTitle)
        } else {
            // 否则生成随机ID
            UUID.randomUUID().toString()
        }

        // 准备消息
        val userMessage = LlmMessage(role = LlmMessageRole.USER, content = prompt)

        // 如果有内存系统，保存用户消息
        if (memory != null) {
            memory.saveMessage(userMessage.toMessage(), threadId)
        }

        // 获取历史消息（如果有内存系统）
        val historyMessages = if (memory != null) {
            memory.getMessages(threadId).map { it.message.toLlmMessage() }
        } else emptyList()

        // 合并系统指令和历史消息
        val allMessages = if (historyMessages.isNotEmpty() && historyMessages[0].role == LlmMessageRole.SYSTEM) {
            historyMessages.toList() + userMessage
        } else {
            listOf(LlmMessage(role = LlmMessageRole.SYSTEM, content = instructions)) +
            historyMessages.toList() + userMessage
        }

        // 从LLM流式生成响应
        val responseStream = model.streamGenerate(allMessages, options.llmOptions)

        // 创建响应流，收集完整文本
        val responseBuilder = StringBuilder()
        val processedStream = flow {
            responseStream.collect { chunk ->
                responseBuilder.append(chunk)
                emit(chunk)
            }

            // 当流完成后，如果有内存系统，保存助手消息
            if (memory != null) {
                val assistantMessage = LlmMessage(
                    role = LlmMessageRole.ASSISTANT,
                    content = responseBuilder.toString()
                )
                memory.saveMessage(assistantMessage.toMessage(), threadId)
            }
        }

        // 创建响应对象
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
            val result = executeToolCall(toolCall)
            results[toolCall.id] = result
        }

        return results
    }

    /**
     * Execute a single tool call.
     */
    private suspend fun executeToolCall(toolCall: LlmToolCall): ToolCallResult {
        val toolId = toolCall.name
        val tool = tools[toolId]

        if (tool == null) {
            return ToolCallResult(
                success = false,
                error = "Tool not found: $toolId"
            )
        }

        return try {
            // Parse arguments as JSON
            val arguments = parseToolArguments(toolCall.arguments)

            // Execute the tool
            val result = tool.execute(arguments)

            ToolCallResult(
                success = true,
                result = result
            )
        } catch (e: IllegalArgumentException) {
            logger.error(e) { "Invalid arguments for tool call: ${toolCall.name}" }
            ToolCallResult(
                success = false,
                error = "Invalid arguments: ${e.message}"
            )
        } catch (e: Exception) {
            logger.error(e) { "Error executing tool call: ${toolCall.name}" }
            ToolCallResult(
                success = false,
                error = "Error executing tool: ${e.message}"
            )
        }
    }

    /**
     * Parse tool arguments as JSON.
     */
    private fun parseToolArguments(arguments: String): JsonElement {
        return try {
            Json.parseToJsonElement(arguments)
        } catch (e: kotlinx.serialization.SerializationException) {
            logger.warn { "Failed to parse tool arguments as JSON: $arguments" }
            JsonObject(emptyMap())
        }
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
