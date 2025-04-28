package ai.kastrax.mcp.security

import ai.kastrax.mcp.protocol.Resource
import ai.kastrax.mcp.protocol.Tool
import ai.kastrax.mcp.protocol.Prompt
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class MCPSecurityTest {
    private lateinit var security: MCPSecurity
    private lateinit var authenticator: InMemoryMCPAuthenticator
    private lateinit var authorizer: InMemoryMCPAuthorizer

    @BeforeEach
    fun setUp() {
        // 创建身份验证器
        authenticator = InMemoryMCPAuthenticator()
        authenticator.registerClient("test-client", "test-secret")
        authenticator.registerServer("test-server", "test-token")

        // 创建授权器
        authorizer = InMemoryMCPAuthorizer()
        authorizer.grantResourceAccess("test-client", "test-resource")
        authorizer.grantToolAccess("test-client", "test-tool")
        authorizer.grantPromptAccess("test-client", "test-prompt")
        authorizer.grantPermission("test-client", "resources:read")
        authorizer.grantPermission("test-client", "tools:use")
        authorizer.grantPermission("test-client", "prompts:use")

        // 创建安全配置
        val config = MCPSecurityConfig(
            enabled = true,
            authenticationEnabled = true,
            authorizationEnabled = true,
            tokenValidationEnabled = true,
            secretKey = "test-secret-key"
        )

        // 创建安全实现
        security = MCPSecurityImpl(config, authenticator, authorizer)
    }

    @Test
    fun `test authenticate client`() {
        // 测试有效的客户端身份验证
        assertTrue(security.authenticateClient("test-client", "test-secret"))

        // 测试无效的客户端身份验证
        assertFalse(security.authenticateClient("test-client", "wrong-secret"))
        assertFalse(security.authenticateClient("unknown-client", "test-secret"))
    }

    @Test
    fun `test authenticate server`() {
        // 测试有效的服务器身份验证
        assertTrue(security.authenticateServer("test-server", "test-token"))

        // 测试无效的服务器身份验证
        assertFalse(security.authenticateServer("test-server", "wrong-token"))
        assertFalse(security.authenticateServer("unknown-server", "test-token"))
    }

    @Test
    fun `test access control`() {
        // 创建测试资源
        val resource = Resource("test-resource", "Test Resource", "Test resource description")
        val tool = Tool("test-tool", "Test Tool", "Test tool description", emptyList())
        val prompt = Prompt("test-prompt", "Test Prompt", "Test prompt description", emptyList())

        // 测试资源访问控制
        assertTrue(security.canAccessResource("test-client", resource))
        assertFalse(security.canAccessResource("unknown-client", resource))

        // 测试工具访问控制
        assertTrue(security.canUseTool("test-client", tool))
        assertFalse(security.canUseTool("unknown-client", tool))

        // 测试提示访问控制
        assertTrue(security.canUsePrompt("test-client", prompt))
        assertFalse(security.canUsePrompt("unknown-client", prompt))
    }

    @Test
    fun `test token management`() = runBlocking {
        // 生成访问令牌
        val token = security.generateAccessToken(
            clientId = "test-client",
            scope = listOf("resources:read", "tools:use"),
            expiresIn = 3600
        )

        // 验证访问令牌
        val tokenInfo = security.validateAccessToken(token)
        assertNotNull(tokenInfo)
        assertEquals("test-client", tokenInfo?.clientId)
        assertEquals(listOf("resources:read", "tools:use"), tokenInfo?.scope)
        assertTrue(tokenInfo?.expiresAt!! > Instant.now().toEpochMilli())
        assertFalse(tokenInfo?.revoked ?: true)

        // 吊销访问令牌
        assertTrue(security.revokeAccessToken(token))

        // 验证已吊销的令牌
        val revokedTokenInfo = security.validateAccessToken(token)
        assertNull(revokedTokenInfo)
    }

    @Test
    fun `test security config`() {
        // 获取安全配置
        val config = security.getSecurityConfig()

        // 验证配置
        assertTrue(config.enabled)
        assertTrue(config.authenticationEnabled)
        assertTrue(config.authorizationEnabled)
        assertTrue(config.tokenValidationEnabled)
        assertEquals("test-secret-key", config.secretKey)
    }

    @Test
    fun `test security builder`() {
        // 使用构建器创建安全实例
        val security = mcpSecurity {
            config {
                enable(true)
                enableAuthentication(true)
                enableAuthorization(true)
                enableTokenValidation(true)
                secretKey("builder-secret-key")
                allowClientIds(listOf("client1", "client2"))
                allowServerIds(listOf("server1", "server2"))
                defaultScope(listOf("resources:read", "tools:use"))
            }
            authenticator(InMemoryMCPAuthenticator())
            authorizer(InMemoryMCPAuthorizer())
        }

        // 验证配置
        val config = security.getSecurityConfig()
        assertTrue(config.enabled)
        assertTrue(config.authenticationEnabled)
        assertTrue(config.authorizationEnabled)
        assertTrue(config.tokenValidationEnabled)
        assertEquals("builder-secret-key", config.secretKey)
        assertEquals(listOf("client1", "client2"), config.allowedClientIds)
        assertEquals(listOf("server1", "server2"), config.allowedServerIds)
        assertEquals(listOf("resources:read", "tools:use"), config.defaultScope)
    }
}
