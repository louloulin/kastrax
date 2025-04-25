package ai.kastrax.mcp.config

import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.server.MCPServer
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * MCP 配置
 */
@Serializable
data class MCPConfig(
    /**
     * 配置 ID
     */
    val id: String? = null,
    
    /**
     * 服务器配置
     */
    val servers: Map<String, ServerConfig> = emptyMap()
)

/**
 * 服务器配置
 */
@Serializable
sealed class ServerConfig {
    /**
     * 服务器名称
     */
    abstract val name: String
}

/**
 * 标准输入/输出服务器配置
 */
@Serializable
data class StdioServerConfig(
    override val name: String,
    
    /**
     * 命令
     */
    val command: String,
    
    /**
     * 参数
     */
    val args: List<String> = emptyList(),
    
    /**
     * 环境变量
     */
    val env: Map<String, String> = emptyMap()
) : ServerConfig()

/**
 * SSE 服务器配置
 */
@Serializable
data class SSEServerConfig(
    override val name: String,
    
    /**
     * URL
     */
    val url: String,
    
    /**
     * 请求头
     */
    val headers: Map<String, String> = emptyMap()
) : ServerConfig()

/**
 * MCP 配置管理器
 */
class MCPConfigManager {
    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    /**
     * 加载配置
     * 
     * @param configPath 配置文件路径
     * @return MCP 配置
     */
    fun loadConfig(configPath: String): MCPConfig {
        val configFile = java.io.File(configPath)
        if (!configFile.exists()) {
            return MCPConfig()
        }
        
        val configJson = configFile.readText()
        return json.decodeFromString(MCPConfig.serializer(), configJson)
    }
    
    /**
     * 保存配置
     * 
     * @param config MCP 配置
     * @param configPath 配置文件路径
     */
    fun saveConfig(config: MCPConfig, configPath: String) {
        val configJson = json.encodeToString(MCPConfig.serializer(), config)
        java.io.File(configPath).writeText(configJson)
    }
    
    /**
     * 创建客户端
     * 
     * @param config MCP 配置
     * @param serverName 服务器名称
     * @return MCP 客户端
     */
    fun createClient(config: MCPConfig, serverName: String): MCPClient {
        val serverConfig = config.servers[serverName] ?: throw IllegalArgumentException("Server not found: $serverName")
        
        // 实际实现将在具体的实现类中提供
        throw NotImplementedError("MCP client implementation not available")
    }
    
    /**
     * 创建所有客户端
     * 
     * @param config MCP 配置
     * @return MCP 客户端映射
     */
    fun createAllClients(config: MCPConfig): Map<String, MCPClient> {
        return config.servers.keys.associateWith { createClient(config, it) }
    }
}

/**
 * MCP 配置构建器
 */
class MCPConfigBuilder {
    private var id: String? = null
    private val servers = mutableMapOf<String, ServerConfig>()
    
    /**
     * 设置配置 ID
     */
    fun id(id: String): MCPConfigBuilder {
        this.id = id
        return this
    }
    
    /**
     * 添加标准输入/输出服务器
     */
    fun stdioServer(name: String, configure: StdioServerConfigBuilder.() -> Unit): MCPConfigBuilder {
        val builder = StdioServerConfigBuilder(name)
        builder.configure()
        servers[name] = builder.build()
        return this
    }
    
    /**
     * 添加 SSE 服务器
     */
    fun sseServer(name: String, configure: SSEServerConfigBuilder.() -> Unit): MCPConfigBuilder {
        val builder = SSEServerConfigBuilder(name)
        builder.configure()
        servers[name] = builder.build()
        return this
    }
    
    /**
     * 构建 MCP 配置
     */
    fun build(): MCPConfig {
        return MCPConfig(
            id = id,
            servers = servers.toMap()
        )
    }
}

/**
 * 标准输入/输出服务器配置构建器
 */
class StdioServerConfigBuilder(private val name: String) {
    private var command: String = ""
    private val args = mutableListOf<String>()
    private val env = mutableMapOf<String, String>()
    
    /**
     * 设置命令
     */
    fun command(command: String): StdioServerConfigBuilder {
        this.command = command
        return this
    }
    
    /**
     * 设置参数
     */
    fun args(vararg args: String): StdioServerConfigBuilder {
        this.args.addAll(args)
        return this
    }
    
    /**
     * 设置环境变量
     */
    fun env(key: String, value: String): StdioServerConfigBuilder {
        this.env[key] = value
        return this
    }
    
    /**
     * 构建标准输入/输出服务器配置
     */
    fun build(): StdioServerConfig {
        require(command.isNotBlank()) { "Command must not be blank" }
        return StdioServerConfig(
            name = name,
            command = command,
            args = args.toList(),
            env = env.toMap()
        )
    }
}

/**
 * SSE 服务器配置构建器
 */
class SSEServerConfigBuilder(private val name: String) {
    private var url: String = ""
    private val headers = mutableMapOf<String, String>()
    
    /**
     * 设置 URL
     */
    fun url(url: String): SSEServerConfigBuilder {
        this.url = url
        return this
    }
    
    /**
     * 设置请求头
     */
    fun header(key: String, value: String): SSEServerConfigBuilder {
        this.headers[key] = value
        return this
    }
    
    /**
     * 构建 SSE 服务器配置
     */
    fun build(): SSEServerConfig {
        require(url.isNotBlank()) { "URL must not be blank" }
        return SSEServerConfig(
            name = name,
            url = url,
            headers = headers.toMap()
        )
    }
}

/**
 * 创建 MCP 配置
 */
fun mcpConfig(configure: MCPConfigBuilder.() -> Unit): MCPConfig {
    val builder = MCPConfigBuilder()
    builder.configure()
    return builder.build()
}
