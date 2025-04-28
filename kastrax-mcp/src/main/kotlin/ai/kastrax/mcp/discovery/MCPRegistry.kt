package ai.kastrax.mcp.discovery

import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.protocol.ServerCapabilities
import kotlinx.serialization.Serializable
import java.net.URL

/**
 * MCP 注册表接口，用于注册和发现 MCP 服务器。
 */
interface MCPRegistry {
    /**
     * 注册表名称
     */
    val name: String

    /**
     * 注册表描述
     */
    val description: String

    /**
     * 注册表主页
     */
    val homepage: String?

    /**
     * 注册表 URL
     */
    val url: URL?

    /**
     * 注册 MCP 服务器
     *
     * @param entry 服务器条目
     * @return 是否注册成功
     */
    fun registerServer(entry: MCPRegistryEntry): Boolean

    /**
     * 注销 MCP 服务器
     *
     * @param id 服务器 ID
     * @return 是否注销成功
     */
    fun unregisterServer(id: String): Boolean

    /**
     * 获取所有注册的服务器
     *
     * @return 服务器列表
     */
    fun listServers(): List<MCPRegistryEntry>

    /**
     * 根据 ID 获取服务器
     *
     * @param id 服务器 ID
     * @return 服务器条目，如果不存在则返回 null
     */
    fun getServer(id: String): MCPRegistryEntry?

    /**
     * 搜索服务器
     *
     * @param query 搜索查询
     * @return 匹配的服务器列表
     */
    fun searchServers(query: String): List<MCPRegistryEntry>

    /**
     * 根据能力搜索服务器
     *
     * @param capabilities 所需的能力列表
     * @return 匹配的服务器列表
     */
    fun searchServersByCapabilities(capabilities: List<String>): List<MCPRegistryEntry>
}

/**
 * MCP 注册表条目，表示注册表中的一个 MCP 服务器。
 */
@Serializable
data class MCPRegistryEntry(
    /**
     * 服务器 ID
     */
    val id: String,

    /**
     * 服务器名称
     */
    val name: String,

    /**
     * 服务器描述
     */
    val description: String,

    /**
     * 服务器版本
     */
    val version: String,

    /**
     * 发布者信息
     */
    val publisher: PublisherInfo? = null,

    /**
     * 是否是官方服务器
     */
    val isOfficial: Boolean = false,

    /**
     * 源代码 URL
     */
    val sourceUrl: String? = null,

    /**
     * 服务器能力
     */
    val capabilities: ServerCapabilities = ServerCapabilities(),

    /**
     * 服务器配置模式
     */
    val schemas: List<ServerSchema> = emptyList(),

    /**
     * 服务器元数据
     */
    val metadata: Map<String, String> = emptyMap(),

    /**
     * 创建时间
     */
    val createdAt: String? = null,

    /**
     * 更新时间
     */
    val updatedAt: String? = null
)

/**
 * 发布者信息
 */
@Serializable
data class PublisherInfo(
    /**
     * 发布者 ID
     */
    val id: String,

    /**
     * 发布者名称
     */
    val name: String,

    /**
     * 发布者 URL
     */
    val url: String? = null
)

/**
 * 服务器配置模式
 */
@Serializable
data class ServerSchema(
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
    val env: Map<String, EnvVarSchema> = emptyMap(),

    /**
     * 运行时参数
     */
    val runtimeArgs: RuntimeArgsSchema? = null
)

/**
 * 环境变量模式
 */
@Serializable
data class EnvVarSchema(
    /**
     * 描述
     */
    val description: String? = null,

    /**
     * 是否必需
     */
    val required: Boolean = false,

    /**
     * 默认值
     */
    val default: String? = null
)

/**
 * 运行时参数模式
 */
@Serializable
data class RuntimeArgsSchema(
    /**
     * 描述
     */
    val description: String? = null,

    /**
     * 是否允许多个值
     */
    val multiple: Boolean = false
)

/**
 * MCP 注册表构建器
 */
interface MCPRegistryBuilder {
    /**
     * 设置注册表名称
     */
    fun name(name: String): MCPRegistryBuilder

    /**
     * 设置注册表描述
     */
    fun description(description: String): MCPRegistryBuilder

    /**
     * 设置注册表主页
     */
    fun homepage(homepage: String): MCPRegistryBuilder

    /**
     * 设置注册表 URL
     */
    fun url(url: String): MCPRegistryBuilder

    /**
     * 构建 MCP 注册表
     */
    fun build(): MCPRegistry
}

/**
 * 创建 MCP 注册表
 */
fun mcpRegistry(configure: MCPRegistryBuilder.() -> Unit): MCPRegistry {
    val builder = MCPRegistryBuilderImpl()
    builder.configure()
    return builder.build()
}

/**
 * MCP 注册表构建器实现
 */
private class MCPRegistryBuilderImpl : MCPRegistryBuilder {
    private var name: String = "Local MCP Registry"
    private var description: String = "A local registry for MCP servers"
    private var homepage: String? = null
    private var url: String? = null

    override fun name(name: String): MCPRegistryBuilder {
        this.name = name
        return this
    }

    override fun description(description: String): MCPRegistryBuilder {
        this.description = description
        return this
    }

    override fun homepage(homepage: String): MCPRegistryBuilder {
        this.homepage = homepage
        return this
    }

    override fun url(url: String): MCPRegistryBuilder {
        this.url = url
        return this
    }

    override fun build(): MCPRegistry {
        return MCPRegistryImpl(
            name = name,
            description = description,
            homepage = homepage,
            url = url?.let { URL(it) }
        )
    }
}
