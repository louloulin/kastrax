package ai.kastrax.mcp.discovery

import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.client.mcpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.net.URL

private val logger = KotlinLogging.logger {}

/**
 * MCP 发现服务接口，用于发现和连接到 MCP 服务器。
 */
interface MCPDiscoveryService {
    /**
     * 添加注册表
     *
     * @param registry 注册表
     * @return 是否添加成功
     */
    fun addRegistry(registry: MCPRegistry): Boolean

    /**
     * 移除注册表
     *
     * @param name 注册表名称
     * @return 是否移除成功
     */
    fun removeRegistry(name: String): Boolean

    /**
     * 获取所有注册表
     *
     * @return 注册表列表
     */
    fun listRegistries(): List<MCPRegistry>

    /**
     * 获取注册表
     *
     * @param name 注册表名称
     * @return 注册表，如果不存在则返回 null
     */
    fun getRegistry(name: String): MCPRegistry?

    /**
     * 发现服务器
     *
     * @param query 搜索查询
     * @return 匹配的服务器列表
     */
    fun discoverServers(query: String = ""): List<MCPRegistryEntry>

    /**
     * 根据能力发现服务器
     *
     * @param capabilities 所需的能力列表
     * @return 匹配的服务器列表
     */
    fun discoverServersByCapabilities(capabilities: List<String>): List<MCPRegistryEntry>

    /**
     * 连接到服务器
     *
     * @param entry 服务器条目
     * @return MCP 客户端
     */
    suspend fun connectToServer(entry: MCPRegistryEntry): MCPClient

    /**
     * 连接到服务器
     *
     * @param id 服务器 ID
     * @param registryName 注册表名称，如果为 null 则搜索所有注册表
     * @return MCP 客户端，如果服务器不存在则返回 null
     */
    suspend fun connectToServer(id: String, registryName: String? = null): MCPClient?

    /**
     * 从远程注册表加载服务器
     *
     * @param url 注册表 URL
     * @return 加载的服务器数量
     */
    suspend fun loadFromRemoteRegistry(url: URL): Int

    /**
     * 监听服务器变化
     *
     * @return 服务器变化流
     */
    fun observeServerChanges(): Flow<ServerChangeEvent>
}

/**
 * 服务器变化事件
 */
sealed class ServerChangeEvent {
    /**
     * 服务器添加事件
     */
    data class ServerAdded(val entry: MCPRegistryEntry, val registry: MCPRegistry) : ServerChangeEvent()

    /**
     * 服务器移除事件
     */
    data class ServerRemoved(val id: String, val registry: MCPRegistry) : ServerChangeEvent()

    /**
     * 服务器更新事件
     */
    data class ServerUpdated(val entry: MCPRegistryEntry, val registry: MCPRegistry) : ServerChangeEvent()
}

/**
 * MCP 发现服务构建器
 */
interface MCPDiscoveryServiceBuilder {
    /**
     * 添加注册表
     */
    fun registry(configure: MCPRegistryBuilder.() -> Unit): MCPDiscoveryServiceBuilder

    /**
     * 构建 MCP 发现服务
     */
    fun build(): MCPDiscoveryService
}

/**
 * 创建 MCP 发现服务
 */
fun mcpDiscoveryService(configure: MCPDiscoveryServiceBuilder.() -> Unit): MCPDiscoveryService {
    val builder = MCPDiscoveryServiceBuilderImpl()
    builder.configure()
    return builder.build()
}

/**
 * MCP 发现服务构建器实现
 */
private class MCPDiscoveryServiceBuilderImpl : MCPDiscoveryServiceBuilder {
    private val registries = mutableListOf<MCPRegistry>()

    override fun registry(configure: MCPRegistryBuilder.() -> Unit): MCPDiscoveryServiceBuilder {
        val registry = mcpRegistry(configure)
        registries.add(registry)
        return this
    }

    override fun build(): MCPDiscoveryService {
        return MCPDiscoveryServiceImpl(registries)
    }
}
