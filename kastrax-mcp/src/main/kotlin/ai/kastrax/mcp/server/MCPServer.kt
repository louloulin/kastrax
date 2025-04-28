package ai.kastrax.mcp.server

import ai.kastrax.mcp.protocol.Resource
import ai.kastrax.mcp.protocol.Tool
import ai.kastrax.mcp.protocol.Prompt

/**
 * MCP 服务器接口，用于提供 MCP 服务。
 */
interface MCPServer {
    /**
     * 服务器名称
     */
    val name: String

    /**
     * 服务器版本
     */
    val version: String

    /**
     * 服务器 ID
     */
    val serverId: String

    /**
     * 启动服务器（标准输入/输出模式）
     */
    suspend fun start()

    /**
     * 启动 SSE 服务器
     *
     * @param host 主机名
     * @param port 端口号
     */
    suspend fun startSSE(host: String = "localhost", port: Int = 8080)

    /**
     * 停止服务器
     */
    suspend fun stop()

    /**
     * 注册资源
     *
     * @param resource 资源
     */
    fun registerResource(resource: Resource)

    /**
     * 注册工具
     *
     * @param tool 工具
     */
    fun registerTool(tool: Tool)

    /**
     * 注册提示
     *
     * @param prompt 提示
     */
    fun registerPrompt(prompt: Prompt)

    /**
     * 获取已注册的资源列表
     */
    fun getResources(): List<Resource>

    /**
     * 获取已注册的工具列表
     */
    fun getTools(): List<Tool>

    /**
     * 获取已注册的提示列表
     */
    fun getPrompts(): List<Prompt>

    /**
     * 验证客户端
     *
     * @param clientId 客户端 ID
     * @param clientSecret 客户端密钥
     * @return 是否验证成功
     */
    fun authenticateClient(clientId: String, clientSecret: String): Boolean

    /**
     * 检查客户端是否有权限访问资源
     *
     * @param clientId 客户端 ID
     * @param resourceId 资源 ID
     * @return 是否有权限
     */
    fun canAccessResource(clientId: String, resourceId: String): Boolean

    /**
     * 检查客户端是否有权限使用工具
     *
     * @param clientId 客户端 ID
     * @param toolId 工具 ID
     * @return 是否有权限
     */
    fun canUseTool(clientId: String, toolId: String): Boolean

    /**
     * 检查客户端是否有权限使用提示
     *
     * @param clientId 客户端 ID
     * @param promptId 提示 ID
     * @return 是否有权限
     */
    fun canUsePrompt(clientId: String, promptId: String): Boolean

    /**
     * 生成访问令牌
     *
     * @param clientId 客户端 ID
     * @param scope 权限范围
     * @param expiresIn 过期时间（秒）
     * @return 访问令牌
     */
    fun generateAccessToken(clientId: String, scope: List<String>, expiresIn: Long): String

    /**
     * 验证访问令牌
     *
     * @param token 访问令牌
     * @return 令牌信息，如果令牌无效则返回 null
     */
    fun validateAccessToken(token: String): ai.kastrax.mcp.security.TokenInfo?

    /**
     * 吸销访问令牌
     *
     * @param token 访问令牌
     * @return 是否吸销成功
     */
    fun revokeAccessToken(token: String): Boolean
}

/**
 * MCP 服务器构建器
 */
interface MCPServerBuilder {
    /**
     * 设置服务器名称
     */
    fun name(name: String): MCPServerBuilder

    /**
     * 设置服务器版本
     */
    fun version(version: String): MCPServerBuilder

    /**
     * 设置服务器 ID
     */
    fun serverId(serverId: String): MCPServerBuilder

    /**
     * 添加资源
     */
    fun resource(configure: ResourceBuilder.() -> Unit): MCPServerBuilder

    /**
     * 添加工具
     */
    fun tool(configure: ToolBuilder.() -> Unit): MCPServerBuilder

    /**
     * 添加提示
     */
    fun prompt(configure: PromptBuilder.() -> Unit): MCPServerBuilder

    /**
     * 设置安全配置
     */
    fun security(configure: ai.kastrax.mcp.security.MCPSecurityConfigBuilder.() -> Unit): MCPServerBuilder

    /**
     * 构建 MCP 服务器
     */
    fun build(): MCPServer
}

/**
 * 资源构建器
 */
interface ResourceBuilder {
    /**
     * 设置资源名称
     */
    var name: String

    /**
     * 设置资源描述
     */
    var description: String

    /**
     * 设置资源内容
     */
    var content: String

    /**
     * 设置资源元数据
     */
    var metadata: Map<String, String>
}

/**
 * 工具构建器
 */
interface ToolBuilder {
    /**
     * 设置工具名称
     */
    var name: String

    /**
     * 设置工具描述
     */
    var description: String

    /**
     * 设置工具参数
     */
    fun parameters(configure: ParametersBuilder.() -> Unit)

    /**
     * 设置工具处理器
     */
    fun handler(handler: suspend (Map<String, Any>) -> String)
}

/**
 * 参数构建器
 */
interface ParametersBuilder {
    /**
     * 添加参数
     */
    fun parameter(configure: ParameterBuilder.() -> Unit)
}

/**
 * 参数构建器
 */
interface ParameterBuilder {
    /**
     * 设置参数名称
     */
    var name: String

    /**
     * 设置参数类型
     */
    var type: String

    /**
     * 设置参数描述
     */
    var description: String

    /**
     * 设置参数是否必需
     */
    var required: Boolean
}

/**
 * 提示构建器
 */
interface PromptBuilder {
    /**
     * 设置提示名称
     */
    var name: String

    /**
     * 设置提示描述
     */
    var description: String

    /**
     * 设置提示内容
     */
    var content: String

    /**
     * 设置提示参数
     */
    fun parameters(configure: ParametersBuilder.() -> Unit)
}

/**
 * 创建 MCP 服务器
 * 注意：实际实现在 MCPServerImpl.kt 中
 */
// 移除重复的函数定义，因为它在 MCPServerImpl.kt 中已经定义
