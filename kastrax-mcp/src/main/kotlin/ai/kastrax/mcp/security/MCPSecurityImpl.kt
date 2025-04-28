package ai.kastrax.mcp.security

import ai.kastrax.mcp.protocol.Resource
import ai.kastrax.mcp.protocol.Tool
import ai.kastrax.mcp.protocol.Prompt
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import java.util.Base64
import java.time.Instant

private val logger = KotlinLogging.logger {}

/**
 * MCP 安全实现
 */
class MCPSecurityImpl(
    private val config: MCPSecurityConfig,
    private val authenticator: MCPAuthenticator,
    private val authorizer: MCPAuthorizer
) : MCPSecurity {
    private val tokens = ConcurrentHashMap<String, TokenInfo>()

    override fun authenticateClient(clientId: String, clientSecret: String): Boolean {
        if (!config.enabled || !config.authenticationEnabled) {
            return true
        }

        // 检查客户端 ID 是否在允许列表中
        if (config.allowedClientIds.isNotEmpty() && !config.allowedClientIds.contains(clientId)) {
            logger.warn { "客户端 ID $clientId 不在允许列表中" }
            return false
        }

        return authenticator.authenticateClient(clientId, clientSecret)
    }

    override fun authenticateServer(serverId: String, serverToken: String): Boolean {
        if (!config.enabled || !config.authenticationEnabled) {
            return true
        }

        // 检查服务器 ID 是否在允许列表中
        if (config.allowedServerIds.isNotEmpty() && !config.allowedServerIds.contains(serverId)) {
            logger.warn { "服务器 ID $serverId 不在允许列表中" }
            return false
        }

        return authenticator.authenticateServer(serverId, serverToken)
    }

    override fun canAccessResource(clientId: String, resource: Resource): Boolean {
        if (!config.enabled || !config.authorizationEnabled) {
            return true
        }

        return authorizer.canAccessResource(clientId, resource)
    }

    override fun canUseTool(clientId: String, tool: Tool): Boolean {
        if (!config.enabled || !config.authorizationEnabled) {
            return true
        }

        return authorizer.canUseTool(clientId, tool)
    }

    override fun canUsePrompt(clientId: String, prompt: Prompt): Boolean {
        if (!config.enabled || !config.authorizationEnabled) {
            return true
        }

        return authorizer.canUsePrompt(clientId, prompt)
    }

    override fun generateAccessToken(clientId: String, scope: List<String>, expiresIn: Long): String {
        if (!config.enabled || !config.tokenValidationEnabled) {
            // 如果安全性未启用，返回一个简单的令牌
            return "mcp-token-${UUID.randomUUID()}"
        }

        val actualScope = scope.ifEmpty { config.defaultScope }
        val expiresAt = Instant.now().plusSeconds(expiresIn).toEpochMilli()
        val tokenId = UUID.randomUUID().toString()
        val tokenInfo = TokenInfo(clientId, actualScope, expiresAt)

        // 存储令牌信息
        tokens[tokenId] = tokenInfo

        // 生成令牌签名
        val signature = generateSignature(tokenId, clientId, expiresAt.toString())

        // 返回令牌
        return "$tokenId:$signature"
    }

    override fun validateAccessToken(token: String): TokenInfo? {
        if (!config.enabled || !config.tokenValidationEnabled) {
            // 如果安全性未启用，返回一个默认的令牌信息
            return TokenInfo(
                clientId = "default-client",
                scope = config.defaultScope,
                expiresAt = Instant.now().plusSeconds(3600).toEpochMilli()
            )
        }

        // 解析令牌
        val parts = token.split(":")
        if (parts.size != 2) {
            logger.warn { "无效的令牌格式" }
            return null
        }

        val tokenId = parts[0]
        val signature = parts[1]

        // 获取令牌信息
        val tokenInfo = tokens[tokenId]
        if (tokenInfo == null) {
            logger.warn { "令牌不存在" }
            return null
        }

        // 检查令牌是否已吊销
        if (tokenInfo.revoked) {
            logger.warn { "令牌已吊销" }
            return null
        }

        // 检查令牌是否已过期
        if (tokenInfo.expiresAt < Instant.now().toEpochMilli()) {
            logger.warn { "令牌已过期" }
            return null
        }

        // 验证令牌签名
        val expectedSignature = generateSignature(tokenId, tokenInfo.clientId, tokenInfo.expiresAt.toString())
        if (signature != expectedSignature) {
            logger.warn { "令牌签名无效" }
            return null
        }

        return tokenInfo
    }

    override fun revokeAccessToken(token: String): Boolean {
        if (!config.enabled || !config.tokenValidationEnabled) {
            return true
        }

        // 解析令牌
        val parts = token.split(":")
        if (parts.size != 2) {
            logger.warn { "无效的令牌格式" }
            return false
        }

        val tokenId = parts[0]

        // 获取令牌信息
        val tokenInfo = tokens[tokenId] ?: return false

        // 吊销令牌
        tokens[tokenId] = tokenInfo.copy(revoked = true)
        return true
    }

    override fun getSecurityConfig(): MCPSecurityConfig {
        return config
    }

    /**
     * 生成令牌签名
     *
     * @param tokenId 令牌 ID
     * @param clientId 客户端 ID
     * @param expiresAt 过期时间
     * @return 签名
     */
    private fun generateSignature(tokenId: String, clientId: String, expiresAt: String): String {
        val data = "$tokenId:$clientId:$expiresAt"
        val secretKey = config.secretKey.ifEmpty { "default-secret-key" }

        return try {
            val mac = Mac.getInstance("HmacSHA256")
            val secretKeySpec = SecretKeySpec(secretKey.toByteArray(), "HmacSHA256")
            mac.init(secretKeySpec)
            val hmacBytes = mac.doFinal(data.toByteArray())
            Base64.getEncoder().encodeToString(hmacBytes)
        } catch (e: Exception) {
            logger.error(e) { "生成签名时发生错误" }
            ""
        }
    }
}

/**
 * MCP 安全构建器实现
 */
class MCPSecurityBuilderImpl : MCPSecurityBuilder {
    private var config: MCPSecurityConfig = MCPSecurityConfig()
    private var authenticator: MCPAuthenticator = InMemoryMCPAuthenticator()
    private var authorizer: MCPAuthorizer = InMemoryMCPAuthorizer()

    override fun config(configure: MCPSecurityConfigBuilder.() -> Unit): MCPSecurityBuilder {
        val builder = MCPSecurityConfigBuilderImpl()
        builder.configure()
        config = builder.build()
        return this
    }

    override fun authenticator(authenticator: MCPAuthenticator): MCPSecurityBuilder {
        this.authenticator = authenticator
        return this
    }

    override fun authorizer(authorizer: MCPAuthorizer): MCPSecurityBuilder {
        this.authorizer = authorizer
        return this
    }

    override fun build(): MCPSecurity {
        return MCPSecurityImpl(config, authenticator, authorizer)
    }
}

/**
 * MCP 安全配置构建器实现
 */
class MCPSecurityConfigBuilderImpl : MCPSecurityConfigBuilder {
    private var enabled: Boolean = false
    private var authenticationEnabled: Boolean = false
    private var authorizationEnabled: Boolean = false
    private var encryptionEnabled: Boolean = false
    private var tokenValidationEnabled: Boolean = false
    private var tokenExpirationSeconds: Long = 3600
    private var allowedClientIds: List<String> = emptyList()
    private var allowedServerIds: List<String> = emptyList()
    private var defaultScope: List<String> = listOf("resources:read", "tools:read", "prompts:read")
    private var secretKey: String = ""

    override fun enable(enabled: Boolean): MCPSecurityConfigBuilder {
        this.enabled = enabled
        return this
    }

    override fun enableAuthentication(enabled: Boolean): MCPSecurityConfigBuilder {
        this.authenticationEnabled = enabled
        return this
    }

    override fun enableAuthorization(enabled: Boolean): MCPSecurityConfigBuilder {
        this.authorizationEnabled = enabled
        return this
    }

    override fun enableEncryption(enabled: Boolean): MCPSecurityConfigBuilder {
        this.encryptionEnabled = enabled
        return this
    }

    override fun enableTokenValidation(enabled: Boolean): MCPSecurityConfigBuilder {
        this.tokenValidationEnabled = enabled
        return this
    }

    override fun tokenExpiration(seconds: Long): MCPSecurityConfigBuilder {
        this.tokenExpirationSeconds = seconds
        return this
    }

    override fun allowClientIds(clientIds: List<String>): MCPSecurityConfigBuilder {
        this.allowedClientIds = clientIds
        return this
    }

    override fun allowServerIds(serverIds: List<String>): MCPSecurityConfigBuilder {
        this.allowedServerIds = serverIds
        return this
    }

    override fun defaultScope(scope: List<String>): MCPSecurityConfigBuilder {
        this.defaultScope = scope
        return this
    }

    override fun secretKey(key: String): MCPSecurityConfigBuilder {
        this.secretKey = key
        return this
    }

    fun build(): MCPSecurityConfig {
        return MCPSecurityConfig(
            enabled = enabled,
            authenticationEnabled = authenticationEnabled,
            authorizationEnabled = authorizationEnabled,
            encryptionEnabled = encryptionEnabled,
            tokenValidationEnabled = tokenValidationEnabled,
            tokenExpirationSeconds = tokenExpirationSeconds,
            allowedClientIds = allowedClientIds,
            allowedServerIds = allowedServerIds,
            defaultScope = defaultScope,
            secretKey = secretKey
        )
    }
}
