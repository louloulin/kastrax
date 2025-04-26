package ai.kastrax.core.agent

import ai.kastrax.core.agent.config.ResponseFormat
import ai.kastrax.core.agent.config.RetryStrategy
import ai.kastrax.core.agent.config.SafetySettings
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
 * Memory configuration options.
 *
 * @property enabled Whether memory is enabled
 * @property conversationId Conversation ID for memory
 * @property contextWindow Number of messages to include in context window
 * @property semanticSearch Whether to use semantic search for retrieving messages
 * @property semanticSearchTopK Number of messages to retrieve with semantic search
 */
data class MemoryOptions(
    val enabled: Boolean = false,
    val conversationId: String? = null,
    val contextWindow: Int = 10,
    val semanticSearch: Boolean = false,
    val semanticSearchTopK: Int = 5
)

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

    /**
     * Get the agent's current state.
     *
     * @return The agent's current state
     */
    suspend fun getState(): AgentState?

    /**
     * Update the agent's state.
     *
     * @param status New status
     * @return Updated state
     */
    suspend fun updateState(status: AgentStatus): AgentState?

    /**
     * Create a new session.
     *
     * @param title Session title
     * @param resourceId Resource ID
     * @param metadata Session metadata
     * @return Session info
     */
    suspend fun createSession(
        title: String? = null,
        resourceId: String? = null,
        metadata: Map<String, String> = emptyMap()
    ): SessionInfo?

    /**
     * Get session information.
     *
     * @param sessionId Session ID
     * @return Session info
     */
    suspend fun getSession(sessionId: String): SessionInfo?

    /**
     * Get session messages.
     *
     * @param sessionId Session ID
     * @param limit Maximum number of messages to return
     * @return List of session messages
     */
    suspend fun getSessionMessages(sessionId: String, limit: Int = 100): List<SessionMessage>?
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
 * @property instructions Optional instructions to override the agent's default instructions
 * @property toolsets Additional tool sets that can be used for this generation
 * @property context Additional context messages to include
 * @property memoryOptions Memory configuration options
 * @property runId Unique ID for this generation run
 * @property toolChoice Controls how tools are selected during generation
 * @property topP Top-p sampling parameter (nucleus sampling)
 * @property frequencyPenalty Frequency penalty parameter
 * @property presencePenalty Presence penalty parameter
 * @property stopSequences List of sequences where the API will stop generating further tokens
 * @property logitBias Modifies the likelihood of specified tokens appearing in the completion
 * @property seed Random seed for deterministic results
 * @property responseFormat Controls the format of the response (e.g., JSON)
 * @property toolCallBudget Maximum number of tool calls allowed in a single generation
 * @property timeoutMs Timeout in milliseconds for the entire generation process
 * @property retryStrategy Strategy for retrying failed generations
 * @property safetySettings Custom safety settings for content filtering
 * @property logProbabilities Whether to return log probabilities of the output tokens
 * @property examples Example conversations to guide the agent's behavior
 * @property metadata Custom metadata for tracking and analysis
 * @property debugMode Enable debug mode for detailed logging
 */
data class AgentGenerateOptions(
    val maxSteps: Int = 1,
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val executeTools: Boolean = true,
    val output: JsonElement? = null,
    val onStepFinish: ((StepResult) -> Unit)? = null,
    val threadId: String? = null,
    val threadTitle: String? = null,
    val instructions: String? = null,
    val toolsets: Map<String, Map<String, Tool>>? = null,
    val context: List<LlmMessage>? = null,
    val memoryOptions: MemoryOptions? = null,
    val runId: String? = null,
    val toolChoice: ToolChoice = ToolChoice.Auto,
    val topP: Double? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val stopSequences: List<String>? = null,
    val logitBias: Map<String, Float>? = null,
    val seed: Long? = null,
    val responseFormat: ResponseFormat? = null,
    val toolCallBudget: Int? = null,
    val timeoutMs: Long? = null,
    val retryStrategy: RetryStrategy? = null,
    val safetySettings: SafetySettings? = null,
    val logProbabilities: Boolean = false,
    val examples: List<List<LlmMessage>>? = null,
    val metadata: Map<String, String>? = null,
    val debugMode: Boolean = false
) {
    /**
     * Convert to LLM options.
     */
    val llmOptions: LlmOptions
        get() = LlmOptions(
            temperature = temperature,
            maxTokens = maxTokens,
            topP = topP,
            frequencyPenalty = frequencyPenalty,
            presencePenalty = presencePenalty,
            stop = stopSequences ?: emptyList(),
            logitBias = logitBias,
            seed = seed,
            responseFormat = null, // TODO: Implement proper conversion
            timeoutMs = timeoutMs,
            logProbabilities = logProbabilities,
            safetySettings = null // TODO: Implement proper conversion
        )

    /**
     * Merge with another options object, with the other object taking precedence.
     */
    fun merge(other: AgentGenerateOptions): AgentGenerateOptions {
        // 合并基本参数
        val result = mergeBasicParams(other)

        // 合并工具集
        val mergedToolsets = mergeToolsets(other.toolsets)

        // 合并上下文
        val mergedContext = mergeContext(other.context)

        // 返回合并后的选项
        return result.copy(
            toolsets = mergedToolsets,
            context = mergedContext
        )
    }

    /**
     * 合并基本参数
     */
    private fun mergeBasicParams(other: AgentGenerateOptions): AgentGenerateOptions {
        // 先合并基本执行参数
        val result = mergeExecutionParams(other)

        // 再合并线程和指令参数
        return mergeThreadParams(other, result)
    }

    /**
     * 合并执行参数
     */
    private fun mergeExecutionParams(other: AgentGenerateOptions): AgentGenerateOptions {
        return copy(
            maxSteps = other.maxSteps.takeIf { it != 1 } ?: maxSteps,
            temperature = other.temperature.takeIf { it != 0.7 } ?: temperature,
            maxTokens = other.maxTokens ?: maxTokens,
            executeTools = other.executeTools,
            output = other.output ?: output,
            onStepFinish = other.onStepFinish ?: onStepFinish,
            toolChoice = if (other.toolChoice != ToolChoice.Auto) other.toolChoice else toolChoice,
            topP = other.topP ?: topP,
            frequencyPenalty = other.frequencyPenalty ?: frequencyPenalty,
            presencePenalty = other.presencePenalty ?: presencePenalty
        )
    }

    /**
     * 合并线程和指令参数
     */
    private fun mergeThreadParams(other: AgentGenerateOptions, base: AgentGenerateOptions): AgentGenerateOptions {
        return base.copy(
            threadId = other.threadId ?: threadId,
            threadTitle = other.threadTitle ?: threadTitle,
            instructions = other.instructions ?: instructions,
            memoryOptions = other.memoryOptions ?: memoryOptions,
            runId = other.runId ?: runId
        )
    }

    /**
     * 合并工具集
     */
    private fun mergeToolsets(otherToolsets: Map<String, Map<String, Tool>>?): Map<String, Map<String, Tool>>? {
        return otherToolsets?.let {
            if (toolsets == null) {
                it
            } else {
                toolsets + it
            }
        } ?: toolsets
    }

    /**
     * 合并上下文
     */
    private fun mergeContext(otherContext: List<LlmMessage>?): List<LlmMessage>? {
        return otherContext?.let {
            if (context == null) {
                it
            } else {
                context + it
            }
        } ?: context
    }
}

/**
 * Tool choice options for controlling how tools are selected during generation.
 */
enum class ToolChoice {
    /** Let the model decide whether to use tools */
    Auto,
    /** Do not use any tools */
    None,
    /** Require the model to use a tool */
    Required,
    /** Require the model to use a specific tool */
    Specific;

    /** Name of the specific tool to use (only relevant for Specific) */
    var toolName: String? = null

    companion object {
        /**
         * Create a specific tool choice
         */
        fun specific(toolName: String): ToolChoice {
            val choice = Specific
            choice.toolName = toolName
            return choice
        }
    }
}

/**
 * Options for agent streaming.
 *
 * @property threadId Optional thread ID for memory
 * @property resourceId Optional resource ID
 * @property threadTitle Optional title for new threads
 * @property temperature Controls randomness (0.0 to 1.0)
 * @property maxTokens Maximum number of tokens to generate
 * @property instructions Optional instructions to override the agent's default instructions
 * @property toolsets Additional tool sets that can be used for this generation
 * @property context Additional context messages to include
 * @property memoryOptions Memory configuration options
 * @property runId Unique ID for this generation run
 * @property onFinish Callback fired when streaming completes
 * @property onStepFinish Callback fired after each generation step completes
 * @property maxSteps Maximum number of steps allowed for generation
 * @property output Schema for structured output
 * @property toolChoice Controls how tools are selected during generation
 * @property topP Top-p sampling parameter (nucleus sampling)
 * @property frequencyPenalty Frequency penalty parameter
 * @property presencePenalty Presence penalty parameter
 * @property stopSequences List of sequences where the API will stop generating further tokens
 * @property logitBias Modifies the likelihood of specified tokens appearing in the completion
 * @property seed Random seed for deterministic results
 * @property responseFormat Controls the format of the response (e.g., JSON)
 * @property toolCallBudget Maximum number of tool calls allowed in a single generation
 * @property timeoutMs Timeout in milliseconds for the entire generation process
 * @property retryStrategy Strategy for retrying failed generations
 * @property safetySettings Custom safety settings for content filtering
 * @property logProbabilities Whether to return log probabilities of the output tokens
 * @property examples Example conversations to guide the agent's behavior
 * @property metadata Custom metadata for tracking and analysis
 * @property debugMode Enable debug mode for detailed logging
 */
data class AgentStreamOptions(
    val threadId: String? = null,
    val resourceId: String? = null,
    val threadTitle: String? = null,
    val temperature: Double = 0.7,
    val maxTokens: Int? = null,
    val instructions: String? = null,
    val toolsets: Map<String, Map<String, Tool>>? = null,
    val context: List<LlmMessage>? = null,
    val memoryOptions: MemoryOptions? = null,
    val runId: String? = null,
    val onFinish: ((String) -> Unit)? = null,
    val onStepFinish: ((StepResult) -> Unit)? = null,
    val maxSteps: Int = 1,
    val output: JsonElement? = null,
    val toolChoice: ToolChoice = ToolChoice.Auto,
    val topP: Double? = null,
    val frequencyPenalty: Double? = null,
    val presencePenalty: Double? = null,
    val stopSequences: List<String>? = null,
    val logitBias: Map<String, Float>? = null,
    val seed: Long? = null,
    val responseFormat: ResponseFormat? = null,
    val toolCallBudget: Int? = null,
    val timeoutMs: Long? = null,
    val retryStrategy: RetryStrategy? = null,
    val safetySettings: SafetySettings? = null,
    val logProbabilities: Boolean = false,
    val examples: List<List<LlmMessage>>? = null,
    val metadata: Map<String, String>? = null,
    val debugMode: Boolean = false
) {
    /**
     * Convert to LLM options.
     */
    val llmOptions: LlmOptions
        get() = LlmOptions(
            temperature = temperature,
            maxTokens = maxTokens,
            topP = topP,
            frequencyPenalty = frequencyPenalty,
            presencePenalty = presencePenalty,
            stop = stopSequences ?: emptyList(),
            logitBias = logitBias,
            seed = seed,
            responseFormat = null, // TODO: Implement proper conversion
            timeoutMs = timeoutMs,
            logProbabilities = logProbabilities,
            safetySettings = null // TODO: Implement proper conversion
        )

    /**
     * Merge with another options object, with the other object taking precedence.
     */
    fun merge(other: AgentStreamOptions): AgentStreamOptions {
        // 合并基本参数
        val result = mergeBasicParams(other)

        // 合并工具集
        val mergedToolsets = mergeToolsets(other.toolsets)

        // 合并上下文
        val mergedContext = mergeContext(other.context)

        // 返回合并后的选项
        return result.copy(
            toolsets = mergedToolsets,
            context = mergedContext
        )
    }

    /**
     * 合并基本参数
     */
    private fun mergeBasicParams(other: AgentStreamOptions): AgentStreamOptions {
        // 先合并基本执行参数
        val result = mergeExecutionParams(other)

        // 再合并线程和回调参数
        return mergeThreadAndCallbackParams(other, result)
    }

    /**
     * 合并执行参数
     */
    private fun mergeExecutionParams(other: AgentStreamOptions): AgentStreamOptions {
        return copy(
            temperature = other.temperature.takeIf { it != 0.7 } ?: temperature,
            maxTokens = other.maxTokens ?: maxTokens,
            maxSteps = other.maxSteps.takeIf { it != 1 } ?: maxSteps,
            output = other.output ?: output,
            toolChoice = if (other.toolChoice != ToolChoice.Auto) other.toolChoice else toolChoice,
            topP = other.topP ?: topP,
            frequencyPenalty = other.frequencyPenalty ?: frequencyPenalty,
            presencePenalty = other.presencePenalty ?: presencePenalty
        )
    }

    /**
     * 合并线程和回调参数
     */
    private fun mergeThreadAndCallbackParams(other: AgentStreamOptions, base: AgentStreamOptions): AgentStreamOptions {
        return base.copy(
            threadId = other.threadId ?: threadId,
            resourceId = other.resourceId ?: resourceId,
            threadTitle = other.threadTitle ?: threadTitle,
            instructions = other.instructions ?: instructions,
            memoryOptions = other.memoryOptions ?: memoryOptions,
            runId = other.runId ?: runId,
            onFinish = other.onFinish ?: onFinish,
            onStepFinish = other.onStepFinish ?: onStepFinish
        )
    }

    /**
     * 合并工具集
     */
    private fun mergeToolsets(otherToolsets: Map<String, Map<String, Tool>>?): Map<String, Map<String, Tool>>? {
        return otherToolsets?.let {
            if (toolsets == null) {
                it
            } else {
                toolsets + it
            }
        } ?: toolsets
    }

    /**
     * 合并上下文
     */
    private fun mergeContext(otherContext: List<LlmMessage>?): List<LlmMessage>? {
        return otherContext?.let {
            if (context == null) {
                it
            } else {
                context + it
            }
        } ?: context
    }
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
 * @property state Agent state (if using state management)
 * @property sessionInfo Session information (if using session management)
 */
data class AgentResponse(
    val text: String = "",
    val toolCalls: List<LlmToolCall> = emptyList(),
    val toolResults: Map<String, ToolCallResult> = emptyMap(),
    val usage: LlmUsage? = null,
    val result: Any? = null,
    val textStream: Flow<String>? = null,
    val threadId: String? = null,
    val state: AgentState? = null,
    val sessionInfo: SessionInfo? = null
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
    var defaultGenerateOptions: AgentGenerateOptions = AgentGenerateOptions()
    var defaultStreamOptions: AgentStreamOptions = AgentStreamOptions()
    var toolsets: MutableMap<String, MutableMap<String, Tool>> = mutableMapOf()
    var sessionManager: SessionManager? = null
    var stateManager: StateManager? = null

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
     * Configure default generate options.
     */
    fun defaultGenerateOptions(init: DefaultGenerateOptionsBuilder.() -> Unit) {
        val builder = DefaultGenerateOptionsBuilder(defaultGenerateOptions)
        builder.init()
        defaultGenerateOptions = builder.options
    }

    /**
     * Configure default stream options.
     */
    fun defaultStreamOptions(init: DefaultStreamOptionsBuilder.() -> Unit) {
        val builder = DefaultStreamOptionsBuilder(defaultStreamOptions)
        builder.init()
        defaultStreamOptions = builder.options
    }

    /**
     * Add a toolset.
     */
    fun toolset(name: String, init: ToolsBuilder.() -> Unit) {
        val builder = ToolsBuilder()
        builder.init()
        toolsets[name] = builder.tools
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
     * Builder for default generate options.
     */
    class DefaultGenerateOptionsBuilder(options: AgentGenerateOptions) {
        var options: AgentGenerateOptions = options
            private set

        fun temperature(value: Double) {
            options = options.copy(temperature = value)
        }

        fun maxTokens(value: Int?) {
            options = options.copy(maxTokens = value)
        }

        fun maxSteps(value: Int) {
            options = options.copy(maxSteps = value)
        }

        fun executeTools(value: Boolean) {
            options = options.copy(executeTools = value)
        }

        fun toolChoice(value: ToolChoice) {
            options = options.copy(toolChoice = value)
        }

        fun topP(value: Double?) {
            options = options.copy(topP = value)
        }

        fun frequencyPenalty(value: Double?) {
            options = options.copy(frequencyPenalty = value)
        }

        fun presencePenalty(value: Double?) {
            options = options.copy(presencePenalty = value)
        }

        fun stopSequences(value: List<String>) {
            options = options.copy(stopSequences = value)
        }

        fun logitBias(value: Map<String, Float>) {
            options = options.copy(logitBias = value)
        }

        fun seed(value: Long) {
            options = options.copy(seed = value)
        }

        fun responseFormat(value: ResponseFormat) {
            options = options.copy(responseFormat = value)
        }

        fun toolCallBudget(value: Int) {
            options = options.copy(toolCallBudget = value)
        }

        fun timeout(value: Long) {
            options = options.copy(timeoutMs = value)
        }

        fun retryStrategy(value: RetryStrategy) {
            options = options.copy(retryStrategy = value)
        }

        fun safetySettings(value: SafetySettings) {
            options = options.copy(safetySettings = value)
        }

        fun logProbabilities(value: Boolean) {
            options = options.copy(logProbabilities = value)
        }

        fun examples(value: List<List<LlmMessage>>) {
            options = options.copy(examples = value)
        }

        fun metadata(value: Map<String, String>) {
            options = options.copy(metadata = value)
        }

        fun debugMode(value: Boolean) {
            options = options.copy(debugMode = value)
        }
    }

    /**
     * Builder for default stream options.
     */
    class DefaultStreamOptionsBuilder(options: AgentStreamOptions) {
        var options: AgentStreamOptions = options
            private set

        fun temperature(value: Double) {
            options = options.copy(temperature = value)
        }

        fun maxTokens(value: Int?) {
            options = options.copy(maxTokens = value)
        }

        fun maxSteps(value: Int) {
            options = options.copy(maxSteps = value)
        }

        fun toolChoice(value: ToolChoice) {
            options = options.copy(toolChoice = value)
        }

        fun topP(value: Double?) {
            options = options.copy(topP = value)
        }

        fun frequencyPenalty(value: Double?) {
            options = options.copy(frequencyPenalty = value)
        }

        fun presencePenalty(value: Double?) {
            options = options.copy(presencePenalty = value)
        }

        fun stopSequences(value: List<String>) {
            options = options.copy(stopSequences = value)
        }

        fun logitBias(value: Map<String, Float>) {
            options = options.copy(logitBias = value)
        }

        fun seed(value: Long) {
            options = options.copy(seed = value)
        }

        fun responseFormat(value: ResponseFormat) {
            options = options.copy(responseFormat = value)
        }

        fun toolCallBudget(value: Int) {
            options = options.copy(toolCallBudget = value)
        }

        fun timeout(value: Long) {
            options = options.copy(timeoutMs = value)
        }

        fun retryStrategy(value: RetryStrategy) {
            options = options.copy(retryStrategy = value)
        }

        fun safetySettings(value: SafetySettings) {
            options = options.copy(safetySettings = value)
        }

        fun logProbabilities(value: Boolean) {
            options = options.copy(logProbabilities = value)
        }

        fun examples(value: List<List<LlmMessage>>) {
            options = options.copy(examples = value)
        }

        fun metadata(value: Map<String, String>) {
            options = options.copy(metadata = value)
        }

        fun debugMode(value: Boolean) {
            options = options.copy(debugMode = value)
        }
    }

    /**
     * 设置会话管理器
     */
    fun sessionManager(manager: SessionManager) {
        sessionManager = manager
    }

    /**
     * 设置状态管理器
     */
    fun stateManager(manager: StateManager) {
        stateManager = manager
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
            memory = memory,
            defaultGenerateOptions = defaultGenerateOptions,
            defaultStreamOptions = defaultStreamOptions,
            toolsets = toolsets,
            sessionManager = sessionManager,
            stateManager = stateManager
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
 * @property defaultGenerateOptions Default options for generation
 * @property defaultStreamOptions Default options for streaming
 * @property toolsets Additional tool sets that can be used for generation
 * @property sessionManager Optional session manager for session control
 * @property stateManager Optional state manager for state management
 */
class LLMAgent(
    name: String,
    val instructions: String,
    val model: LlmProvider,
    val tools: Map<String, Tool> = emptyMap(),
    val memory: ai.kastrax.memory.api.Memory? = null,
    val defaultGenerateOptions: AgentGenerateOptions = AgentGenerateOptions(),
    val defaultStreamOptions: AgentStreamOptions = AgentStreamOptions(),
    val toolsets: Map<String, Map<String, Tool>> = emptyMap(),
    val sessionManager: SessionManager? = null,
    val stateManager: StateManager? = null
) : KastraXBase(component = "AGENT", name = name), Agent {

    // 当前状态ID
    private var currentStateId: String? = null

    /**
     * Generate a response from multiple messages.
     */
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: AgentGenerateOptions
    ): AgentResponse {
        // 更新状态为思考中
        updateState(AgentStatus.THINKING)
        // Merge default options with provided options
        val mergedOptions = defaultGenerateOptions.merge(options)

        // Use override instructions if provided
        val effectiveInstructions = mergedOptions.instructions ?: instructions

        // Prepare messages with system instructions and context
        val contextMessages = mergedOptions.context ?: emptyList()
        val allMessages = if (messages.isNotEmpty() && messages[0].role == LlmMessageRole.SYSTEM) {
            contextMessages + messages
        } else {
            listOf(LlmMessage(role = LlmMessageRole.SYSTEM, content = effectiveInstructions)) +
                contextMessages + messages
        }

        // Collect all tools: base tools + toolsets
        val allTools = mutableMapOf<String, Tool>().apply {
            putAll(tools)
            mergedOptions.toolsets?.forEach { (_, toolset) ->
                putAll(toolset)
            }
        }

        // Generate response from LLM
        val llmOptions = mergedOptions.llmOptions.copy(
            tools = allTools.values.map { tool ->
                buildJsonObject {
                    put("type", JsonPrimitive("function"))
                    put("function", buildJsonObject {
                        put("name", JsonPrimitive(tool.id))
                        put("description", JsonPrimitive(tool.description))
                        put("parameters", tool.inputSchema)
                    })
                }
            },
            toolChoice = when (mergedOptions.toolChoice) {
                ToolChoice.Auto -> "auto"
                ToolChoice.None -> "none"
                ToolChoice.Required -> "required"
                ToolChoice.Specific -> buildJsonObject {
                    put("type", JsonPrimitive("function"))
                    put("function", buildJsonObject {
                        put("name", JsonPrimitive(mergedOptions.toolChoice.toolName ?: ""))
                    })
                }
            }
        )

        val response = model.generate(allMessages, llmOptions)

        // Handle tool calls if present and execution is enabled
        val toolResults = if (options.executeTools && response.toolCalls.isNotEmpty()) {
            // 更新状态为执行工具
            updateState(AgentStatus.EXECUTING)
            executeToolCalls(response.toolCalls)
        } else {
            emptyMap()
        }

        // 更新状态为响应中
        updateState(AgentStatus.RESPONDING)

        // Create step result for callback
        val stepResult = StepResult(
            text = response.content,
            toolCalls = response.toolCalls,
            toolResults = toolResults
        )

        // Call step finish callback if provided
        options.onStepFinish?.invoke(stepResult)

        // 更新状态为空闲
        val finalState = updateState(AgentStatus.IDLE)

        // 获取会话信息
        val sessionInfo = if (options.threadId != null && sessionManager != null) {
            sessionManager.getSession(options.threadId)
        } else {
            null
        }

        return AgentResponse(
            text = response.content,
            toolCalls = response.toolCalls,
            toolResults = toolResults,
            usage = response.usage,
            threadId = options.threadId,
            state = finalState,
            sessionInfo = sessionInfo
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
        // 更新状态为思考中
        updateState(AgentStatus.THINKING)
        // Merge default options with provided options
        val mergedOptions = defaultStreamOptions.merge(options)

        // Use override instructions if provided
        val effectiveInstructions = mergedOptions.instructions ?: instructions

        // 准备流式生成
        val threadId = prepareThreadId(mergedOptions)
        val userMessage = LlmMessage(role = LlmMessageRole.USER, content = prompt)
        saveUserMessage(userMessage, threadId)

        // 准备消息和工具
        val allMessages = prepareMessages(userMessage, effectiveInstructions, mergedOptions, threadId)
        val llmOptions = prepareLlmOptions(mergedOptions)

        // 流式生成响应
        return streamResponse(allMessages, llmOptions, threadId)
    }

    /**
     * 准备线程ID
     */
    private suspend fun prepareThreadId(options: AgentStreamOptions): String {
        return options.threadId ?: if (memory != null) {
            // 如果有内存系统，创建新线程
            memory.createThread(options.threadTitle)
        } else {
            // 否则生成随机ID
            options.runId ?: UUID.randomUUID().toString()
        }
    }

    /**
     * 保存用户消息
     */
    private suspend fun saveUserMessage(userMessage: LlmMessage, threadId: String) {
        if (memory != null) {
            memory.saveMessage(userMessage.toMessage(), threadId)
        }
    }

    /**
     * 准备消息
     */
    private suspend fun prepareMessages(
        userMessage: LlmMessage,
        effectiveInstructions: String,
        options: AgentStreamOptions,
        threadId: String
    ): List<LlmMessage> {
        // 获取历史消息（如果有内存系统）
        val historyMessages = if (memory != null) {
            memory.getMessages(threadId).map { it.message.toLlmMessage() }
        } else emptyList()

        // 添加上下文消息
        val contextMessages = options.context ?: emptyList()

        // 合并系统指令、上下文和历史消息
        return if (historyMessages.isNotEmpty() && historyMessages[0].role == LlmMessageRole.SYSTEM) {
            contextMessages + historyMessages.toList() + userMessage
        } else {
            listOf(LlmMessage(role = LlmMessageRole.SYSTEM, content = effectiveInstructions)) +
            contextMessages + historyMessages.toList() + userMessage
        }
    }

    /**
     * 准备LLM选项
     */
    private fun prepareLlmOptions(options: AgentStreamOptions): LlmOptions {
        // Collect all tools: base tools + toolsets
        val allTools = mutableMapOf<String, Tool>().apply {
            putAll(tools)
            options.toolsets?.forEach { (_, toolset) ->
                putAll(toolset)
            }
        }

        // 准备LLM选项
        return options.llmOptions.copy(
            tools = allTools.values.map { tool ->
                buildJsonObject {
                    put("type", JsonPrimitive("function"))
                    put("function", buildJsonObject {
                        put("name", JsonPrimitive(tool.id))
                        put("description", JsonPrimitive(tool.description))
                        put("parameters", tool.inputSchema)
                    })
                }
            },
            toolChoice = when (options.toolChoice) {
                ToolChoice.Auto -> "auto"
                ToolChoice.None -> "none"
                ToolChoice.Required -> "required"
                ToolChoice.Specific -> buildJsonObject {
                    put("type", JsonPrimitive("function"))
                    put("function", buildJsonObject {
                        put("name", JsonPrimitive(options.toolChoice.toolName ?: ""))
                    })
                }
            }
        )
    }

    /**
     * 流式生成响应
     */
    private suspend fun streamResponse(
        messages: List<LlmMessage>,
        llmOptions: LlmOptions,
        threadId: String
    ): AgentResponse {
        // 更新状态为响应中
        updateState(AgentStatus.RESPONDING)
        // 从LLM流式生成响应
        val responseStream = model.streamGenerate(messages, llmOptions)

        // 创建响应流，收集完整文本
        val responseBuilder = StringBuilder()
        val processedStream = flow {
            responseStream.collect { chunk ->
                responseBuilder.append(chunk)
                emit(chunk)
            }

            // 当流完成后，保存助手消息
            val content = responseBuilder.toString()

            // 如果有内存系统，保存助手消息
            if (memory != null) {
                val assistantMessage = LlmMessage(
                    role = LlmMessageRole.ASSISTANT,
                    content = content
                )
                memory.saveMessage(assistantMessage.toMessage(), threadId)
            }

            // 如果有会话管理器，保存助手消息
            if (sessionManager != null && !threadId.isNullOrEmpty()) {
                val assistantMessage = LlmMessage(
                    role = LlmMessageRole.ASSISTANT,
                    content = content
                )
                sessionManager.saveMessage(assistantMessage, threadId)
            }

            // 更新状态为空闲
            updateState(AgentStatus.IDLE)
        }

        // 获取当前状态
        val currentState = getState()

        // 获取会话信息
        val sessionInfo = if (sessionManager != null && !threadId.isNullOrEmpty()) {
            sessionManager.getSession(threadId)
        } else {
            null
        }

        // 创建响应对象
        return AgentResponse(
            textStream = processedStream,
            threadId = threadId,
            state = currentState,
            sessionInfo = sessionInfo
        )
    }

    /**
     * 保存助手消息
     */
    private suspend fun saveAssistantMessage(content: String, threadId: String) {
        if (memory != null) {
            val assistantMessage = LlmMessage(
                role = LlmMessageRole.ASSISTANT,
                content = content
            )
            memory.saveMessage(assistantMessage.toMessage(), threadId)
        }
    }

    // reset方法已在下面实现

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

    /**
     * Get the agent's current state.
     */
    override suspend fun getState(): AgentState? {
        if (stateManager == null) {
            logger.warn { "No state manager configured" }
            return null
        }

        return if (currentStateId != null) {
            stateManager.getState(currentStateId!!)
        } else {
            null
        }
    }

    /**
     * Update the agent's state.
     */
    override suspend fun updateState(status: AgentStatus): AgentState? {
        if (stateManager == null) {
            logger.warn { "No state manager configured" }
            return null
        }

        val currentState = if (currentStateId != null) {
            stateManager.getState(currentStateId!!)
        } else {
            AgentState()
        }

        val updatedState = currentState?.withStatus(status) ?: AgentState(status = status)
        val savedState = stateManager.saveState(updatedState)
        currentStateId = savedState.id

        logger.debug { "Updated agent state to $status" }
        return savedState
    }

    /**
     * Create a new session.
     */
    override suspend fun createSession(
        title: String?,
        resourceId: String?,
        metadata: Map<String, String>
    ): SessionInfo? {
        if (sessionManager == null) {
            logger.warn { "No session manager configured" }
            return null
        }

        val session = sessionManager.createSession(title, resourceId, metadata)

        // 如果有状态管理器，更新状态中的线程ID
        if (stateManager != null && currentStateId != null) {
            val currentState = stateManager.getState(currentStateId!!)
            if (currentState != null) {
                val updatedState = currentState.withThreadId(session.id)
                stateManager.saveState(updatedState)
            }
        }

        logger.debug { "Created session: ${session.id}" }
        return session
    }

    /**
     * Get session information.
     */
    override suspend fun getSession(sessionId: String): SessionInfo? {
        if (sessionManager == null) {
            logger.warn { "No session manager configured" }
            return null
        }

        return sessionManager.getSession(sessionId)
    }

    /**
     * Get session messages.
     */
    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        if (sessionManager == null) {
            logger.warn { "No session manager configured" }
            return null
        }

        return sessionManager.getMessages(sessionId, limit)
    }

    /**
     * Reset the agent's state.
     */
    override suspend fun reset() {
        // 重置状态
        if (stateManager != null && currentStateId != null) {
            val currentState = stateManager.getState(currentStateId!!)
            if (currentState != null) {
                val resetState = currentState.withStatus(AgentStatus.IDLE)
                stateManager.saveState(resetState)
            }
        }

        currentStateId = null
        logger.debug { "Reset agent state" }
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
