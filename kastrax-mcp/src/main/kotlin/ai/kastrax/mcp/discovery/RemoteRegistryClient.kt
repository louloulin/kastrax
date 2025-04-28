package ai.kastrax.mcp.discovery

import io.github.oshai.kotlinlogging.KotlinLogging
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.http.*
import io.ktor.serialization.kotlinx.json.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.URL

private val logger = KotlinLogging.logger {}

/**
 * 远程注册表客户端，用于从远程注册表加载服务器。
 */
class RemoteRegistryClient(
    private val url: URL
) {
    private val client = HttpClient(CIO) {
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                prettyPrint = true
                isLenient = true
            })
        }
    }

    /**
     * 连接到远程注册表
     *
     * @return 注册表信息
     */
    suspend fun connect(): RemoteRegistryInfo {
        logger.info { "Connecting to remote registry: $url" }
        
        return try {
            val response = client.get(url.toString())
            if (response.status.isSuccess()) {
                val registryInfo = response.body<RemoteRegistryInfo>()
                logger.info { "Connected to remote registry: ${registryInfo.name}" }
                registryInfo
            } else {
                logger.error { "Failed to connect to remote registry: $url, status: ${response.status}" }
                throw RuntimeException("Failed to connect to remote registry: $url, status: ${response.status}")
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to connect to remote registry: $url" }
            throw e
        }
    }

    /**
     * 获取服务器列表
     *
     * @return 服务器列表
     */
    suspend fun listServers(): List<MCPRegistryEntry> {
        logger.info { "Listing servers from remote registry: $url" }
        
        return try {
            val response = client.get("$url/servers")
            if (response.status.isSuccess()) {
                val serverList = response.body<RemoteServerList>()
                logger.info { "Retrieved ${serverList.servers.size} servers from remote registry" }
                serverList.servers
            } else {
                logger.error { "Failed to list servers from remote registry: $url, status: ${response.status}" }
                emptyList()
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to list servers from remote registry: $url" }
            emptyList()
        }
    }

    /**
     * 获取服务器详情
     *
     * @param id 服务器 ID
     * @return 服务器条目，如果不存在则返回 null
     */
    suspend fun getServer(id: String): MCPRegistryEntry? {
        logger.info { "Getting server $id from remote registry: $url" }
        
        return try {
            val response = client.get("$url/servers/$id")
            if (response.status.isSuccess()) {
                val server = response.body<MCPRegistryEntry>()
                logger.info { "Retrieved server: ${server.name} (${server.id})" }
                server
            } else {
                logger.error { "Failed to get server $id from remote registry: $url, status: ${response.status}" }
                null
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to get server $id from remote registry: $url" }
            null
        }
    }

    /**
     * 搜索服务器
     *
     * @param query 搜索查询
     * @return 匹配的服务器列表
     */
    suspend fun searchServers(query: String): List<MCPRegistryEntry> {
        logger.info { "Searching servers with query '$query' from remote registry: $url" }
        
        return try {
            val response = client.get("$url/servers/search") {
                parameter("q", query)
            }
            if (response.status.isSuccess()) {
                val serverList = response.body<RemoteServerList>()
                logger.info { "Found ${serverList.servers.size} servers matching query '$query'" }
                serverList.servers
            } else {
                logger.error { "Failed to search servers from remote registry: $url, status: ${response.status}" }
                emptyList()
            }
        } catch (e: Exception) {
            logger.error(e) { "Failed to search servers from remote registry: $url" }
            emptyList()
        }
    }

    /**
     * 关闭客户端
     */
    fun close() {
        client.close()
    }
}

/**
 * 远程注册表信息
 */
@Serializable
data class RemoteRegistryInfo(
    val name: String,
    val description: String,
    val homepage: String? = null,
    val version: String? = null
)

/**
 * 远程服务器列表
 */
@Serializable
data class RemoteServerList(
    val servers: List<MCPRegistryEntry>
)
