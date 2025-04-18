package ai.kastrax.core.tools

import kotlinx.serialization.json.*
import kotlinx.serialization.Serializable

/**
 * Interface for tools that can be used by agents.
 */
interface Tool {
    /**
     * The tool's unique identifier.
     */
    val id: String
    
    /**
     * The tool's display name.
     */
    val name: String
    
    /**
     * Description of what the tool does.
     */
    val description: String
    
    /**
     * JSON schema describing the tool's input parameters.
     */
    val inputSchema: JsonElement
    
    /**
     * JSON schema describing the tool's output format.
     */
    val outputSchema: JsonElement?
    
    /**
     * Execute the tool with the given input.
     *
     * @param input Tool input parameters as JSON
     * @return Tool execution result as JSON
     */
    suspend fun execute(input: JsonElement): JsonElement
    
    /**
     * Execute the tool with context information.
     *
     * @param input Tool input parameters as JSON
     * @param threadId Optional thread ID for context
     * @param resourceId Optional resource ID for context
     * @return Tool execution result as JSON
     */
    suspend fun executeWithContext(
        input: JsonElement,
        threadId: String? = null,
        resourceId: String? = null
    ): JsonElement {
        // Default implementation calls the regular execute method
        return execute(input)
    }
}

/**
 * Result of a tool call execution.
 *
 * @property success Whether the tool execution was successful
 * @property result The result of the tool execution (if successful)
 * @property error Error message (if unsuccessful)
 */
@Serializable
data class ToolCallResult(
    val success: Boolean,
    val result: JsonElement? = null,
    val error: String? = null
)

/**
 * Builder for creating Tool instances.
 */
class ToolBuilder {
    var id: String = ""
    var name: String = ""
    var description: String = ""
    var inputSchema: JsonElement? = null
    var outputSchema: JsonElement? = null
    lateinit var execute: suspend (JsonElement) -> JsonElement
    var executeWithContext: (suspend (JsonElement, String?, String?) -> JsonElement)? = null
    
    /**
     * Build a Tool instance from the builder configuration.
     */
    fun build(): Tool {
        require(id.isNotEmpty()) { "Tool ID must not be empty" }
        require(name.isNotEmpty()) { "Tool name must not be empty" }
        require(description.isNotEmpty()) { "Tool description must not be empty" }
        requireNotNull(inputSchema) { "Tool input schema must be defined" }
        
        return object : Tool {
            override val id: String = this@ToolBuilder.id
            override val name: String = this@ToolBuilder.name
            override val description: String = this@ToolBuilder.description
            override val inputSchema: JsonElement = this@ToolBuilder.inputSchema!!
            override val outputSchema: JsonElement? = this@ToolBuilder.outputSchema
            
            override suspend fun execute(input: JsonElement): JsonElement {
                return this@ToolBuilder.execute(input)
            }
            
            override suspend fun executeWithContext(
                input: JsonElement,
                threadId: String?,
                resourceId: String?
            ): JsonElement {
                return if (this@ToolBuilder.executeWithContext != null) {
                    this@ToolBuilder.executeWithContext!!(input, threadId, resourceId)
                } else {
                    execute(input)
                }
            }
        }
    }
}

/**
 * DSL function for creating a Tool.
 */
fun tool(init: ToolBuilder.() -> Unit): Tool {
    val builder = ToolBuilder()
    builder.init()
    return builder.build()
}

/**
 * Helper function to create a JSON object from key-value pairs.
 */
fun jsonObject(vararg pairs: Pair<String, JsonElement>): JsonElement {
    return JsonObject(pairs.toMap())
}

/**
 * Helper function to create a JSON object from a block.
 */
fun jsonObject(block: JsonObjectBuilder.() -> Unit): JsonElement {
    val builder = JsonObjectBuilder()
    builder.block()
    return builder.build()
}

/**
 * Builder for creating JSON objects.
 */
class JsonObjectBuilder {
    private val properties = mutableMapOf<String, JsonElement>()
    
    /**
     * Add a string property.
     */
    infix fun String.to(value: String) {
        properties[this] = JsonPrimitive(value)
    }
    
    /**
     * Add a number property.
     */
    infix fun String.to(value: Number) {
        properties[this] = JsonPrimitive(value)
    }
    
    /**
     * Add a boolean property.
     */
    infix fun String.to(value: Boolean) {
        properties[this] = JsonPrimitive(value)
    }
    
    /**
     * Add a JSON element property.
     */
    infix fun String.to(value: JsonElement) {
        properties[this] = value
    }
    
    /**
     * Build the JSON object.
     */
    fun build(): JsonElement {
        return JsonObject(properties)
    }
}
