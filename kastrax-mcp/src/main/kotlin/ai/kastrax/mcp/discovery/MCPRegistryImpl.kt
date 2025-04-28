package ai.kastrax.mcp.discovery

import io.github.oshai.kotlinlogging.KotlinLogging
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * MCP 注册表实现
 */
class MCPRegistryImpl(
    override val name: String,
    override val description: String,
    override val homepage: String?,
    override val url: URL?
) : MCPRegistry {
    private val servers = ConcurrentHashMap<String, MCPRegistryEntry>()

    override fun registerServer(entry: MCPRegistryEntry): Boolean {
        if (servers.containsKey(entry.id)) {
            logger.warn { "Server with ID ${entry.id} already exists in registry" }
            return false
        }

        servers[entry.id] = entry
        logger.info { "Registered server: ${entry.name} (${entry.id})" }
        return true
    }

    override fun unregisterServer(id: String): Boolean {
        val removed = servers.remove(id)
        if (removed != null) {
            logger.info { "Unregistered server: ${removed.name} ($id)" }
            return true
        }
        
        logger.warn { "Server with ID $id not found in registry" }
        return false
    }

    override fun listServers(): List<MCPRegistryEntry> {
        return servers.values.toList()
    }

    override fun getServer(id: String): MCPRegistryEntry? {
        return servers[id]
    }

    override fun searchServers(query: String): List<MCPRegistryEntry> {
        if (query.isBlank()) {
            return listServers()
        }

        val lowerQuery = query.lowercase()
        return servers.values.filter { server ->
            server.name.lowercase().contains(lowerQuery) ||
            server.description.lowercase().contains(lowerQuery) ||
            server.id.lowercase().contains(lowerQuery)
        }
    }

    override fun searchServersByCapabilities(capabilities: List<String>): List<MCPRegistryEntry> {
        if (capabilities.isEmpty()) {
            return listServers()
        }

        return servers.values.filter { server ->
            capabilities.all { capability ->
                when (capability) {
                    "resources" -> server.capabilities.resources
                    "tools" -> server.capabilities.tools
                    "prompts" -> server.capabilities.prompts
                    "sampling" -> server.capabilities.sampling
                    "cancelRequest" -> server.capabilities.cancelRequest
                    "progress" -> server.capabilities.progress
                    else -> false
                }
            }
        }
    }
}
