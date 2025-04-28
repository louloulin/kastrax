package ai.kastrax.mcp.security

import ai.kastrax.mcp.protocol.Resource
import ai.kastrax.mcp.protocol.Tool
import ai.kastrax.mcp.protocol.Prompt
import kotlinx.serialization.Serializable

/**
 * MCP 安全接口，用于管理 MCP 安全性和访问控制。
 */
interface MCPSecurity {
    /**
     * 验证客户端身份
     *
     * @param clientId 客户端 ID
     * @param clientSecret 客户端密钥
     * @return 是否验证成功
     */
    fun authenticateClient(clientId: String, clientSecret: String): Boolean

    /**
     * 验证服务器身份
     *
     * @param serverId 服务器 ID
     * @param serverToken 服务器令牌
     * @return 是否验证成功
     */
    fun authenticateServer(serverId: String, serverToken: String): Boolean

    /**
     * 检查是否有权限访问资源
     *
     * @param clientId 客户端 ID
     * @param resource 资源
     * @return 是否有权限
     */
    fun canAccessResource(clientId: String, resource: Resource): Boolean

    /**
     * 检查是否有权限使用工具
     *
     * @param clientId 客户端 ID
     * @param tool 工具
     * @return 是否有权限
     */
    fun canUseTool(clientId: String, tool: Tool): Boolean

    /**
     * 检查是否有权限使用提示
     *
     * @param clientId 客户端 ID
     * @param prompt 提示
     * @return 是否有权限
     */
    fun canUsePrompt(clientId: String, prompt: Prompt): Boolean

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
    fun validateAccessToken(token: String): TokenInfo?

    /**
     * 吊销访问令牌
     *
     * @param token 访问令牌
     * @return 是否吊销成功
     */
    fun revokeAccessToken(token: String): Boolean

    /**
     * 获取安全配置
     *
     * @return 安全配置
     */
    fun getSecurityConfig(): MCPSecurityConfig
}

/**
 * 令牌信息
 */
@Serializable
data class TokenInfo(
    /**
     * 客户端 ID
     */
    val clientId: String,

    /**
     * 权限范围
     */
    val scope: List<String>,

    /**
     * 过期时间（毫秒时间戳）
     */
    val expiresAt: Long,

    /**
     * 是否已吊销
     */
    val revoked: Boolean = false
)

/**
 * MCP 安全配置
 */
@Serializable
data class MCPSecurityConfig(
    /**
     * 是否启用安全性
     */
    val enabled: Boolean = false,

    /**
     * 是否启用身份验证
     */
    val authenticationEnabled: Boolean = false,

    /**
     * 是否启用授权
     */
    val authorizationEnabled: Boolean = false,

    /**
     * 是否启用加密
     */
    val encryptionEnabled: Boolean = false,

    /**
     * 是否启用令牌验证
     */
    val tokenValidationEnabled: Boolean = false,

    /**
     * 令牌过期时间（秒）
     */
    val tokenExpirationSeconds: Long = 3600,

    /**
     * 允许的客户端 ID 列表
     */
    val allowedClientIds: List<String> = emptyList(),

    /**
     * 允许的服务器 ID 列表
     */
    val allowedServerIds: List<String> = emptyList(),

    /**
     * 默认权限范围
     */
    val defaultScope: List<String> = listOf("resources:read", "tools:read", "prompts:read"),

    /**
     * 安全密钥
     */
    val secretKey: String = ""
)

/**
 * MCP 安全构建器
 */
interface MCPSecurityBuilder {
    /**
     * 设置安全配置
     */
    fun config(configure: MCPSecurityConfigBuilder.() -> Unit): MCPSecurityBuilder

    /**
     * 设置身份验证器
     */
    fun authenticator(authenticator: MCPAuthenticator): MCPSecurityBuilder

    /**
     * 设置授权器
     */
    fun authorizer(authorizer: MCPAuthorizer): MCPSecurityBuilder

    /**
     * 构建 MCP 安全
     */
    fun build(): MCPSecurity
}

/**
 * MCP 安全配置构建器
 */
interface MCPSecurityConfigBuilder {
    /**
     * 启用安全性
     */
    fun enable(enabled: Boolean): MCPSecurityConfigBuilder

    /**
     * 启用身份验证
     */
    fun enableAuthentication(enabled: Boolean): MCPSecurityConfigBuilder

    /**
     * 启用授权
     */
    fun enableAuthorization(enabled: Boolean): MCPSecurityConfigBuilder

    /**
     * 启用加密
     */
    fun enableEncryption(enabled: Boolean): MCPSecurityConfigBuilder

    /**
     * 启用令牌验证
     */
    fun enableTokenValidation(enabled: Boolean): MCPSecurityConfigBuilder

    /**
     * 设置令牌过期时间
     */
    fun tokenExpiration(seconds: Long): MCPSecurityConfigBuilder

    /**
     * 设置允许的客户端 ID
     */
    fun allowClientIds(clientIds: List<String>): MCPSecurityConfigBuilder

    /**
     * 设置允许的服务器 ID
     */
    fun allowServerIds(serverIds: List<String>): MCPSecurityConfigBuilder

    /**
     * 设置默认权限范围
     */
    fun defaultScope(scope: List<String>): MCPSecurityConfigBuilder

    /**
     * 设置安全密钥
     */
    fun secretKey(key: String): MCPSecurityConfigBuilder
}

/**
 * 创建 MCP 安全
 */
fun mcpSecurity(configure: MCPSecurityBuilder.() -> Unit): MCPSecurity {
    val builder = MCPSecurityBuilderImpl()
    builder.configure()
    return builder.build()
}
