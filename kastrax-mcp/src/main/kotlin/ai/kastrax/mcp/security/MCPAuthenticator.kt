package ai.kastrax.mcp.security

/**
 * MCP 身份验证器接口，用于验证 MCP 客户端和服务器的身份。
 */
interface MCPAuthenticator {
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
     * 生成客户端密钥
     *
     * @param clientId 客户端 ID
     * @return 客户端密钥
     */
    fun generateClientSecret(clientId: String): String

    /**
     * 生成服务器令牌
     *
     * @param serverId 服务器 ID
     * @return 服务器令牌
     */
    fun generateServerToken(serverId: String): String

    /**
     * 注册客户端
     *
     * @param clientId 客户端 ID
     * @param clientSecret 客户端密钥
     * @return 是否注册成功
     */
    fun registerClient(clientId: String, clientSecret: String): Boolean

    /**
     * 注册服务器
     *
     * @param serverId 服务器 ID
     * @param serverToken 服务器令牌
     * @return 是否注册成功
     */
    fun registerServer(serverId: String, serverToken: String): Boolean

    /**
     * 吊销客户端
     *
     * @param clientId 客户端 ID
     * @return 是否吊销成功
     */
    fun revokeClient(clientId: String): Boolean

    /**
     * 吊销服务器
     *
     * @param serverId 服务器 ID
     * @return 是否吊销成功
     */
    fun revokeServer(serverId: String): Boolean
}

/**
 * 基于内存的 MCP 身份验证器实现
 */
class InMemoryMCPAuthenticator : MCPAuthenticator {
    private val clientSecrets = mutableMapOf<String, String>()
    private val serverTokens = mutableMapOf<String, String>()
    private val revokedClients = mutableSetOf<String>()
    private val revokedServers = mutableSetOf<String>()

    override fun authenticateClient(clientId: String, clientSecret: String): Boolean {
        if (revokedClients.contains(clientId)) {
            return false
        }

        val storedSecret = clientSecrets[clientId] ?: return false
        return storedSecret == clientSecret
    }

    override fun authenticateServer(serverId: String, serverToken: String): Boolean {
        if (revokedServers.contains(serverId)) {
            return false
        }

        val storedToken = serverTokens[serverId] ?: return false
        return storedToken == serverToken
    }

    override fun generateClientSecret(clientId: String): String {
        val secret = generateRandomString(32)
        clientSecrets[clientId] = secret
        return secret
    }

    override fun generateServerToken(serverId: String): String {
        val token = generateRandomString(64)
        serverTokens[serverId] = token
        return token
    }

    override fun registerClient(clientId: String, clientSecret: String): Boolean {
        if (revokedClients.contains(clientId)) {
            return false
        }

        clientSecrets[clientId] = clientSecret
        return true
    }

    override fun registerServer(serverId: String, serverToken: String): Boolean {
        if (revokedServers.contains(serverId)) {
            return false
        }

        serverTokens[serverId] = serverToken
        return true
    }

    override fun revokeClient(clientId: String): Boolean {
        clientSecrets.remove(clientId)
        revokedClients.add(clientId)
        return true
    }

    override fun revokeServer(serverId: String): Boolean {
        serverTokens.remove(serverId)
        revokedServers.add(serverId)
        return true
    }

    /**
     * 生成随机字符串
     *
     * @param length 字符串长度
     * @return 随机字符串
     */
    private fun generateRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
