package ai.kastrax.mcp.protocol

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * MCP 消息基类
 */
@Serializable
sealed class MCPMessage {
    /**
     * 消息 ID
     */
    abstract val id: String?
    
    /**
     * JSON-RPC 版本，固定为 "2.0"
     */
    val jsonrpc: String = "2.0"
}

/**
 * MCP 请求消息
 */
@Serializable
data class MCPRequest(
    override val id: String,
    val method: String,
    val params: JsonElement? = null
) : MCPMessage()

/**
 * MCP 响应消息
 */
@Serializable
data class MCPResponse(
    override val id: String?,
    val result: JsonElement? = null,
    val error: MCPError? = null
) : MCPMessage()

/**
 * MCP 通知消息（无需响应的请求）
 */
@Serializable
data class MCPNotification(
    override val id: String? = null,
    val method: String,
    val params: JsonElement? = null
) : MCPMessage()

/**
 * MCP 错误
 */
@Serializable
data class MCPError(
    val code: Int,
    val message: String,
    val data: JsonElement? = null
)

/**
 * MCP 错误代码
 */
object MCPErrorCodes {
    /**
     * 解析错误
     */
    const val PARSE_ERROR = -32700
    
    /**
     * 无效请求
     */
    const val INVALID_REQUEST = -32600
    
    /**
     * 方法不存在
     */
    const val METHOD_NOT_FOUND = -32601
    
    /**
     * 无效参数
     */
    const val INVALID_PARAMS = -32602
    
    /**
     * 内部错误
     */
    const val INTERNAL_ERROR = -32603
    
    /**
     * 服务器错误
     */
    const val SERVER_ERROR_START = -32000
    const val SERVER_ERROR_END = -32099
    
    /**
     * 请求取消
     */
    const val REQUEST_CANCELLED = -32800
    
    /**
     * 内容太大
     */
    const val CONTENT_TOO_LARGE = -32801
    
    /**
     * 资源不存在
     */
    const val RESOURCE_NOT_FOUND = -32900
    
    /**
     * 工具不存在
     */
    const val TOOL_NOT_FOUND = -32901
    
    /**
     * 提示不存在
     */
    const val PROMPT_NOT_FOUND = -32902
}

/**
 * MCP 方法名称
 */
object MCPMethods {
    /**
     * 初始化
     */
    const val INITIALIZE = "initialize"
    
    /**
     * 关闭
     */
    const val SHUTDOWN = "shutdown"
    
    /**
     * 退出
     */
    const val EXIT = "exit"
    
    /**
     * 列出资源
     */
    const val LIST_RESOURCES = "listResources"
    
    /**
     * 获取资源
     */
    const val GET_RESOURCE = "getResource"
    
    /**
     * 列出工具
     */
    const val LIST_TOOLS = "listTools"
    
    /**
     * 调用工具
     */
    const val CALL_TOOL = "callTool"
    
    /**
     * 列出提示
     */
    const val LIST_PROMPTS = "listPrompts"
    
    /**
     * 获取提示
     */
    const val GET_PROMPT = "getPrompt"
    
    /**
     * 采样
     */
    const val SAMPLE = "sample"
    
    /**
     * 取消请求
     */
    const val CANCEL_REQUEST = "$/cancelRequest"
    
    /**
     * 进度通知
     */
    const val PROGRESS = "$/progress"
    
    /**
     * 日志通知
     */
    const val LOG = "$/log"
}

/**
 * 初始化参数
 */
@Serializable
data class InitializeParams(
    /**
     * 客户端名称
     */
    val name: String,
    
    /**
     * 客户端版本
     */
    val version: String,
    
    /**
     * 客户端能力
     */
    val capabilities: ClientCapabilities
)

/**
 * 客户端能力
 */
@Serializable
data class ClientCapabilities(
    /**
     * 是否支持资源
     */
    val resources: Boolean = false,
    
    /**
     * 是否支持工具
     */
    val tools: Boolean = false,
    
    /**
     * 是否支持提示
     */
    val prompts: Boolean = false,
    
    /**
     * 是否支持采样
     */
    val sampling: Boolean = false,
    
    /**
     * 是否支持取消请求
     */
    val cancelRequest: Boolean = false,
    
    /**
     * 是否支持进度通知
     */
    val progress: Boolean = false
)

/**
 * 初始化结果
 */
@Serializable
data class InitializeResult(
    /**
     * 服务器名称
     */
    val name: String,
    
    /**
     * 服务器版本
     */
    val version: String,
    
    /**
     * 服务器能力
     */
    val capabilities: ServerCapabilities
)

/**
 * 服务器能力
 */
@Serializable
data class ServerCapabilities(
    /**
     * 是否支持资源
     */
    val resources: Boolean = false,
    
    /**
     * 是否支持工具
     */
    val tools: Boolean = false,
    
    /**
     * 是否支持提示
     */
    val prompts: Boolean = false,
    
    /**
     * 是否支持采样
     */
    val sampling: Boolean = false,
    
    /**
     * 是否支持取消请求
     */
    val cancelRequest: Boolean = false,
    
    /**
     * 是否支持进度通知
     */
    val progress: Boolean = false
)

/**
 * 列出资源结果
 */
@Serializable
data class ListResourcesResult(
    /**
     * 资源列表
     */
    val resources: List<Resource>
)

/**
 * 获取资源参数
 */
@Serializable
data class GetResourceParams(
    /**
     * 资源 ID
     */
    val id: String
)

/**
 * 获取资源结果
 */
@Serializable
data class GetResourceResult(
    /**
     * 资源内容
     */
    val content: String
)

/**
 * 列出工具结果
 */
@Serializable
data class ListToolsResult(
    /**
     * 工具列表
     */
    val tools: List<Tool>
)

/**
 * 调用工具参数
 */
@Serializable
data class CallToolParams(
    /**
     * 工具 ID
     */
    val id: String,
    
    /**
     * 工具参数
     */
    val parameters: JsonElement
)

/**
 * 调用工具结果
 */
@Serializable
data class CallToolResult(
    /**
     * 工具执行结果
     */
    val result: String
)

/**
 * 列出提示结果
 */
@Serializable
data class ListPromptsResult(
    /**
     * 提示列表
     */
    val prompts: List<Prompt>
)

/**
 * 获取提示参数
 */
@Serializable
data class GetPromptParams(
    /**
     * 提示 ID
     */
    val id: String
)

/**
 * 获取提示结果
 */
@Serializable
data class GetPromptResult(
    /**
     * 提示内容
     */
    val content: String
)

/**
 * 采样参数
 */
@Serializable
data class SampleParams(
    /**
     * 提示
     */
    val prompt: String,
    
    /**
     * 采样选项
     */
    val options: SampleOptions? = null
)

/**
 * 采样选项
 */
@Serializable
data class SampleOptions(
    /**
     * 温度
     */
    val temperature: Double? = null,
    
    /**
     * 最大令牌数
     */
    val maxTokens: Int? = null,
    
    /**
     * 停止序列
     */
    val stop: List<String>? = null
)

/**
 * 采样结果
 */
@Serializable
data class SampleResult(
    /**
     * 采样结果
     */
    val result: String
)

/**
 * 取消请求参数
 */
@Serializable
data class CancelRequestParams(
    /**
     * 要取消的请求 ID
     */
    val id: String
)

/**
 * 进度通知参数
 */
@Serializable
data class ProgressParams(
    /**
     * 请求 ID
     */
    val id: String,
    
    /**
     * 进度值（0-100）
     */
    val value: Int,
    
    /**
     * 进度消息
     */
    val message: String? = null
)

/**
 * 日志通知参数
 */
@Serializable
data class LogParams(
    /**
     * 日志级别
     */
    val level: LogLevel,
    
    /**
     * 日志消息
     */
    val message: String
)

/**
 * 日志级别
 */
@Serializable
enum class LogLevel {
    DEBUG, INFO, WARN, ERROR
}
