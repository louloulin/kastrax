package ai.kastrax.sdk.js

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.js.Promise

/**
 * JavaScript API for KastraX.
 */
@JsExport
class Agent(config: AgentConfig) {
    private val id: String = "agent-${Math.random().toString(36).substring(2, 9)}"
    private val name: String = config.name
    private val model: String = config.model
    private val options: AgentOptions = config.options ?: AgentOptions()
    private val tools: MutableList<Tool> = mutableListOf()
    
    /**
     * Run the agent with the given input.
     */
    fun run(input: String): Promise<String> {
        return Promise { resolve, reject ->
            try {
                // In a real implementation, this would call the KastraX backend
                val response = "Response from agent '$name' using model '$model': $input"
                resolve(response)
            } catch (e: Throwable) {
                reject(e)
            }
        }
    }
    
    /**
     * Add a tool to the agent.
     */
    fun addTool(tool: Tool) {
        tools.add(tool)
    }
    
    /**
     * Get the current state of the agent.
     */
    fun getState(): AgentState {
        return AgentState(
            id = id,
            name = name,
            model = model,
            toolCount = tools.size
        )
    }
    
    /**
     * Convert the agent to a JSON string.
     */
    fun toJson(): String {
        return Json.encodeToString(
            AgentState(
                id = id,
                name = name,
                model = model,
                toolCount = tools.size
            )
        )
    }
}

/**
 * Configuration for creating an agent.
 */
@JsExport
@Serializable
data class AgentConfig(
    val name: String,
    val model: String,
    val options: AgentOptions? = null
)

/**
 * Options for agent configuration.
 */
@JsExport
@Serializable
data class AgentOptions(
    val temperature: Double = 0.7,
    val maxTokens: Int = 1000,
    val topP: Double = 1.0,
    val topK: Int = 40
)

/**
 * Represents the current state of an agent.
 */
@JsExport
@Serializable
data class AgentState(
    val id: String,
    val name: String,
    val model: String,
    val toolCount: Int
)

/**
 * Tool that can be used by an agent.
 */
@JsExport
class Tool(config: ToolConfig) {
    val name: String = config.name
    val description: String = config.description ?: ""
    val function: (String) -> String = config.function
    
    /**
     * Execute the tool with the given input.
     */
    fun execute(input: String): String {
        return function(input)
    }
}

/**
 * Configuration for creating a tool.
 */
@JsExport
data class ToolConfig(
    val name: String,
    val description: String? = null,
    val function: (String) -> String
)

/**
 * Memory for storing agent interactions.
 */
@JsExport
class Memory(config: MemoryConfig) {
    private val type: String = config.type
    private val options: MemoryOptions = config.options ?: MemoryOptions()
    private val messages: MutableList<MemoryMessage> = mutableListOf()
    
    /**
     * Add a message to memory.
     */
    fun addMessage(message: MemoryMessage) {
        messages.add(message)
        if (messages.size > options.maxMessages) {
            messages.removeAt(0)
        }
    }
    
    /**
     * Get all messages in memory.
     */
    fun getMessages(): Array<MemoryMessage> {
        return messages.toTypedArray()
    }
    
    /**
     * Clear all messages from memory.
     */
    fun clear() {
        messages.clear()
    }
}

/**
 * Configuration for creating memory.
 */
@JsExport
data class MemoryConfig(
    val type: String,
    val options: MemoryOptions? = null
)

/**
 * Options for memory configuration.
 */
@JsExport
@Serializable
data class MemoryOptions(
    val maxMessages: Int = 100
)

/**
 * Message stored in memory.
 */
@JsExport
@Serializable
data class MemoryMessage(
    val role: String,
    val content: String,
    val timestamp: Long = js("Date.now()").unsafeCast<Long>()
)
