package ai.kastrax.mcp.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * MCP 工具
 * 
 * 工具是 MCP 服务器提供的功能，可以被客户端调用。
 */
@Serializable
data class Tool(
    /**
     * 工具 ID
     */
    val id: String,
    
    /**
     * 工具名称
     */
    val name: String,
    
    /**
     * 工具描述
     */
    val description: String,
    
    /**
     * 工具参数定义
     */
    val parameters: ToolParameters,
    
    /**
     * 工具返回值定义
     */
    val returns: ToolReturns? = null,
    
    /**
     * 工具元数据
     */
    val metadata: Map<String, String> = emptyMap()
)

/**
 * 工具参数定义
 */
@Serializable
data class ToolParameters(
    /**
     * 参数类型
     */
    val type: String = "object",
    
    /**
     * 必需的参数
     */
    val required: List<String> = emptyList(),
    
    /**
     * 参数属性
     */
    val properties: Map<String, ToolParameterProperty> = emptyMap()
)

/**
 * 工具参数属性
 */
@Serializable
data class ToolParameterProperty(
    /**
     * 参数类型
     */
    val type: String,
    
    /**
     * 参数描述
     */
    val description: String,
    
    /**
     * 参数枚举值
     */
    val enum: List<String>? = null,
    
    /**
     * 参数默认值
     */
    val default: JsonElement? = null
)

/**
 * 工具返回值定义
 */
@Serializable
data class ToolReturns(
    /**
     * 返回值类型
     */
    val type: String,
    
    /**
     * 返回值描述
     */
    val description: String
)

/**
 * 工具处理器
 */
interface ToolHandler {
    /**
     * 处理工具调用
     * 
     * @param toolId 工具 ID
     * @param parameters 工具参数
     * @return 工具执行结果
     */
    suspend fun handleToolCall(toolId: String, parameters: Map<String, Any>): String
}

/**
 * 函数式工具处理器
 */
class FunctionToolHandler(
    private val handlers: Map<String, suspend (Map<String, Any>) -> String>
) : ToolHandler {
    override suspend fun handleToolCall(toolId: String, parameters: Map<String, Any>): String {
        val handler = handlers[toolId] ?: throw ToolNotFoundException(toolId)
        return try {
            handler(parameters)
        } catch (e: Exception) {
            throw ToolExecutionException(toolId, e)
        }
    }
}

/**
 * 工具不存在异常
 */
class ToolNotFoundException : Exception {
    constructor(toolId: String) : super("Tool not found: $toolId")
    constructor(toolId: String, cause: Throwable) : super("Tool not found: $toolId", cause)
}

/**
 * 工具执行异常
 */
class ToolExecutionException : Exception {
    constructor(toolId: String, message: String) : super("Tool execution failed: $toolId - $message")
    constructor(toolId: String, cause: Throwable) : super("Tool execution failed: $toolId - ${cause.message}", cause)
}
