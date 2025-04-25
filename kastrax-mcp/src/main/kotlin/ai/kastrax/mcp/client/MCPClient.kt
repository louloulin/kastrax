package ai.kastrax.mcp.client

import ai.kastrax.mcp.protocol.Resource
import ai.kastrax.mcp.protocol.Tool
import ai.kastrax.mcp.protocol.Prompt

/**
 * MCP 客户端接口，用于连接到 MCP 服务器并与之交互。
 */
interface MCPClient {
    /**
     * 客户端名称
     */
    val name: String

    /**
     * 客户端版本
     */
    val version: String

    /**
     * 连接到 MCP 服务器
     */
    suspend fun connect()

    /**
     * 断开与 MCP 服务器的连接
     */
    suspend fun disconnect()

    /**
     * 获取服务器提供的资源列表
     */
    suspend fun resources(): List<Resource>

    /**
     * 获取资源内容
     *
     * @param resourceId 资源 ID
     * @return 资源内容
     */
    suspend fun getResource(resourceId: String): String

    /**
     * 获取服务器提供的工具列表
     */
    suspend fun tools(): List<Tool>

    /**
     * 调用工具
     *
     * @param toolId 工具 ID
     * @param parameters 工具参数
     * @return 工具执行结果
     */
    suspend fun callTool(toolId: String, parameters: Map<String, Any>): String

    /**
     * 获取服务器提供的提示列表
     */
    suspend fun prompts(): List<Prompt>

    /**
     * 获取提示内容
     *
     * @param promptId 提示 ID
     * @return 提示内容
     */
    suspend fun getPrompt(promptId: String): String

    /**
     * 检查服务器是否支持特定功能
     *
     * @param capability 功能名称
     * @return 是否支持
     */
    fun supportsCapability(capability: String): Boolean
}

/**
 * MCP 客户端构建器
 */
interface MCPClientBuilder {
    /**
     * 设置客户端名称
     */
    fun name(name: String): MCPClientBuilder

    /**
     * 设置客户端版本
     */
    fun version(version: String): MCPClientBuilder

    /**
     * 设置服务器配置
     */
    fun server(configure: MCPServerConfig.() -> Unit): MCPClientBuilder

    /**
     * 设置超时时间（毫秒）
     */
    fun timeout(timeoutMs: Long): MCPClientBuilder

    /**
     * 构建 MCP 客户端
     */
    fun build(): MCPClient
}

/**
 * MCP 服务器配置
 */
interface MCPServerConfig {
    /**
     * 配置标准输入/输出服务器
     */
    fun stdio(configure: StdioServerConfig.() -> Unit)

    /**
     * 配置 SSE 服务器
     */
    fun sse(configure: SSEServerConfig.() -> Unit)
}

/**
 * 标准输入/输出服务器配置
 */
interface StdioServerConfig {
    /**
     * 设置命令
     */
    var command: String

    /**
     * 设置参数
     */
    var args: List<String>

    /**
     * 设置环境变量
     */
    var env: Map<String, String>
}

/**
 * SSE 服务器配置
 */
interface SSEServerConfig {
    /**
     * 设置 URL
     */
    var url: String

    /**
     * 设置请求头
     */
    var headers: Map<String, String>
}

/**
 * 创建 MCP 客户端
 */
fun mcpClient(configure: MCPClientBuilder.() -> Unit): MCPClient {
    // 实际实现将在具体的实现类中提供
    throw NotImplementedError("MCP client implementation not available")
}
