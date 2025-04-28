package ai.kastrax.mcp.discovery

import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.client.mcpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.serialization.json.Json
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * MCP 发现服务实现
 */
class MCPDiscoveryServiceImpl(
    initialRegistries: List<MCPRegistry> = emptyList()
) : MCPDiscoveryService {
    private val registries = ConcurrentHashMap<String, MCPRegistry>()
    private val json = Json { ignoreUnknownKeys = true }

    private val _serverChanges = MutableSharedFlow<ServerChangeEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )

    init {
        // 添加初始注册表
        initialRegistries.forEach { registry ->
            addRegistry(registry)
        }

        // 添加默认本地注册表（如果没有提供注册表）
        if (registries.isEmpty()) {
            val localRegistry = mcpRegistry {
                name("Local Registry")
                description("Default local registry for MCP servers")
            }
            addRegistry(localRegistry)
        }
    }

    override fun addRegistry(registry: MCPRegistry): Boolean {
        if (registries.containsKey(registry.name)) {
            logger.warn { "Registry with name ${registry.name} already exists" }
            return false
        }

        registries[registry.name] = registry
        logger.info { "Added registry: ${registry.name}" }
        return true
    }

    override fun removeRegistry(name: String): Boolean {
        val removed = registries.remove(name)
        if (removed != null) {
            logger.info { "Removed registry: $name" }
            return true
        }

        logger.warn { "Registry with name $name not found" }
        return false
    }

    override fun listRegistries(): List<MCPRegistry> {
        return registries.values.toList()
    }

    override fun getRegistry(name: String): MCPRegistry? {
        return registries[name]
    }

    override fun discoverServers(query: String): List<MCPRegistryEntry> {
        return registries.values.flatMap { registry ->
            registry.searchServers(query)
        }
    }

    override fun discoverServersByCapabilities(capabilities: List<String>): List<MCPRegistryEntry> {
        return registries.values.flatMap { registry ->
            registry.searchServersByCapabilities(capabilities)
        }
    }

    override suspend fun connectToServer(entry: MCPRegistryEntry): MCPClient {
        logger.info { "Connecting to server: ${entry.name} (${entry.id})" }

        // 根据服务器配置创建客户端
        val client = createClientFromEntry(entry)

        // 连接到服务器
        client.connect()

        return client
    }

    override suspend fun connectToServer(id: String, registryName: String?): MCPClient? {
        // 如果指定了注册表名称，则只在该注册表中查找
        if (registryName != null) {
            val registry = getRegistry(registryName)
            if (registry == null) {
                logger.warn { "Registry with name $registryName not found" }
                return null
            }

            val entry = registry.getServer(id)
            if (entry == null) {
                logger.warn { "Server with ID $id not found in registry $registryName" }
                return null
            }

            return connectToServer(entry)
        }

        // 否则，在所有注册表中查找
        for (registry in registries.values) {
            val entry = registry.getServer(id)
            if (entry != null) {
                return connectToServer(entry)
            }
        }

        logger.warn { "Server with ID $id not found in any registry" }
        return null
    }

    override suspend fun loadFromRemoteRegistry(url: URL): Int {
        logger.info { "Loading servers from remote registry: $url" }

        try {
            // 创建远程注册表客户端
            val remoteClient = RemoteRegistryClient(url)

            // 连接到远程注册表
            val registryInfo = remoteClient.connect()

            // 创建注册表
            val registry = mcpRegistry {
                name(registryInfo.name)
                description(registryInfo.description)
                if (registryInfo.homepage != null) {
                    homepage(registryInfo.homepage)
                }
                url(url.toString())
            }

            // 添加注册表
            addRegistry(registry)

            // 获取服务器列表
            val servers = remoteClient.listServers()

            // 注册服务器
            var count = 0
            for (server in servers) {
                if (registry.registerServer(server)) {
                    count++
                    publishServerChangeEvent(ServerChangeEvent.ServerAdded(server, registry))
                }
            }

            // 关闭客户端
            remoteClient.close()

            logger.info { "Loaded $count servers from remote registry: $url" }
            return count
        } catch (e: Exception) {
            logger.error(e) { "Failed to load servers from remote registry: $url" }
            return 0
        }
    }

    override fun observeServerChanges(): Flow<ServerChangeEvent> {
        return _serverChanges.asSharedFlow()
    }

    /**
     * 从服务器条目创建 MCP 客户端
     */
    private fun createClientFromEntry(entry: MCPRegistryEntry): MCPClient {
        // 获取第一个可用的配置模式
        val schema = entry.schemas.firstOrNull()
            ?: throw IllegalArgumentException("No configuration schema available for server: ${entry.id}")

        return mcpClient {
            name("${entry.name}Client")
            version("1.0.0")

            server {
                stdio {
                    command = schema.command
                    args = schema.args
                    env = schema.env.mapValues { (_, schema) -> schema.default ?: "" }
                }
            }
        }
    }

    /**
     * 发布服务器变化事件
     */
    fun publishServerChangeEvent(event: ServerChangeEvent) {
        _serverChanges.tryEmit(event)
    }
}
