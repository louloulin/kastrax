package ai.kastrax.mcp

import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.config.MCPConfig
import ai.kastrax.mcp.config.ServerConfig
import ai.kastrax.mcp.config.StdioServerConfig
import ai.kastrax.mcp.config.SSEServerConfig
import ai.kastrax.mcp.integration.MCPToolset
import ai.kastrax.mcp.transport.StdioTransport
import ai.kastrax.mcp.transport.SSETransport
import ai.kastrax.mcp.transport.Transport
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * MCP 管理器
 *
 * 用于管理多个 MCP 客户端
 */
class MCPManager(
    private val config: MCPConfig
) {
    private val clients = ConcurrentHashMap<String, MCPClient>()
    private val toolsets = ConcurrentHashMap<String, MCPToolset>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * 初始化管理器
     */
    fun initialize() {
        // 创建客户端
        config.servers.forEach { (name, serverConfig) ->
            try {
                val client = createClient(name, serverConfig)
                clients[name] = client

                // 连接客户端
                scope.launch {
                    try {
                        client.connect()

                        // 创建工具集
                        val toolset = MCPToolset(client)
                        toolset.initialize()
                        toolsets[name] = toolset

                        logger.info { "Connected to MCP server: $name" }
                    } catch (e: Exception) {
                        logger.error(e) { "Failed to connect to MCP server: $name" }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Failed to create MCP client: $name" }
            }
        }
    }

    /**
     * 获取客户端
     */
    fun getClient(name: String): MCPClient? {
        return clients[name]
    }

    /**
     * 获取所有客户端
     */
    fun getClients(): Map<String, MCPClient> {
        return clients.toMap()
    }

    /**
     * 获取工具集
     */
    fun getToolset(name: String): MCPToolset? {
        return toolsets[name]
    }

    /**
     * 获取所有工具集
     */
    fun getToolsets(): Map<String, MCPToolset> {
        return toolsets.toMap()
    }

    /**
     * 断开所有连接
     */
    fun disconnectAll() {
        runBlocking {
            clients.forEach { (name, client) ->
                try {
                    client.disconnect()
                    logger.info { "Disconnected from MCP server: $name" }
                } catch (e: Exception) {
                    logger.error(e) { "Failed to disconnect from MCP server: $name" }
                }
            }
        }
    }

    /**
     * 创建客户端
     */
    private fun createClient(name: String, serverConfig: ServerConfig): MCPClient {
        val transport = when (serverConfig) {
            is StdioServerConfig -> {
                StdioTransport(
                    command = serverConfig.command,
                    args = serverConfig.args,
                    env = serverConfig.env
                )
            }
            is SSEServerConfig -> {
                SSETransport(
                    url = serverConfig.url,
                    headers = serverConfig.headers
                )
            }
            else -> {
                throw IllegalArgumentException("Unknown server config type: ${serverConfig::class.java.name}")
            }
        }

        return MCPClientImpl(
            name = name,
            version = "1.0.0",
            transport = transport
        )
    }
}
