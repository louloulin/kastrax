package ai.kastrax.a2a.discovery

import ai.kastrax.a2a.client.A2AClient
import ai.kastrax.a2a.client.A2AClientConfig
import ai.kastrax.a2a.model.AgentCard
import ai.kastrax.a2a.model.Capability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * A2A 代理发现服务配置
 */
data class A2ADiscoveryConfig(
    /**
     * 自动发现间隔（毫秒）
     */
    val discoveryInterval: Long = TimeUnit.MINUTES.toMillis(5),
    
    /**
     * 是否启用自动发现
     */
    val enableAutoDiscovery: Boolean = true,
    
    /**
     * 代理注册表过期时间（毫秒）
     */
    val registryExpiration: Long = TimeUnit.HOURS.toMillis(1)
)

/**
 * A2A 代理发现服务，用于发现和注册 A2A 代理
 */
class A2ADiscoveryService(
    private val config: A2ADiscoveryConfig = A2ADiscoveryConfig(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    /**
     * 代理注册表
     */
    private val registry = ConcurrentHashMap<String, AgentRegistryEntry>()
    
    /**
     * 已知的服务器 URL
     */
    private val knownServers = ConcurrentHashMap.newKeySet<String>()
    
    /**
     * 互斥锁，用于保护注册表
     */
    private val mutex = Mutex()
    
    init {
        if (config.enableAutoDiscovery) {
            startAutoDiscovery()
        }
    }
    
    /**
     * 注册代理
     */
    suspend fun registerAgent(agentCard: AgentCard) {
        mutex.withLock {
            registry[agentCard.id] = AgentRegistryEntry(
                agentCard = agentCard,
                registeredAt = System.currentTimeMillis(),
                lastSeenAt = System.currentTimeMillis()
            )
        }
    }
    
    /**
     * 注销代理
     */
    suspend fun unregisterAgent(agentId: String) {
        mutex.withLock {
            registry.remove(agentId)
        }
    }
    
    /**
     * 获取代理
     */
    suspend fun getAgent(agentId: String): AgentCard? {
        return mutex.withLock {
            registry[agentId]?.agentCard
        }
    }
    
    /**
     * 获取所有代理
     */
    suspend fun getAllAgents(): List<AgentCard> {
        return mutex.withLock {
            registry.values.map { it.agentCard }
        }
    }
    
    /**
     * 按能力查找代理
     */
    suspend fun findAgentsByCapability(capabilityId: String): List<AgentCard> {
        return mutex.withLock {
            registry.values
                .filter { entry ->
                    entry.agentCard.capabilities.any { it.id == capabilityId }
                }
                .map { it.agentCard }
        }
    }
    
    /**
     * 添加服务器 URL
     */
    fun addServer(serverUrl: String) {
        knownServers.add(serverUrl)
    }
    
    /**
     * 移除服务器 URL
     */
    fun removeServer(serverUrl: String) {
        knownServers.remove(serverUrl)
    }
    
    /**
     * 启动自动发现
     */
    private fun startAutoDiscovery() {
        scope.launch {
            while (true) {
                try {
                    discoverAgents()
                    cleanupRegistry()
                } catch (e: Exception) {
                    // 记录错误但不中断循环
                    println("Error during auto discovery: ${e.message}")
                }
                
                delay(config.discoveryInterval)
            }
        }
    }
    
    /**
     * 发现代理
     */
    private suspend fun discoverAgents() {
        for (serverUrl in knownServers) {
            try {
                val client = A2AClient(A2AClientConfig(serverUrl = serverUrl))
                
                // 获取服务器上的所有代理
                val agents = client.getAgentCard()
                
                // 注册代理
                registerAgent(agents)
                
                client.close()
            } catch (e: Exception) {
                // 记录错误但继续处理其他服务器
                println("Error discovering agents from $serverUrl: ${e.message}")
            }
        }
    }
    
    /**
     * 清理注册表
     */
    private suspend fun cleanupRegistry() {
        val now = System.currentTimeMillis()
        val expiredAgents = mutableListOf<String>()
        
        mutex.withLock {
            for ((agentId, entry) in registry) {
                if (now - entry.lastSeenAt > config.registryExpiration) {
                    expiredAgents.add(agentId)
                }
            }
            
            for (agentId in expiredAgents) {
                registry.remove(agentId)
            }
        }
    }
}

/**
 * 代理注册表条目
 */
data class AgentRegistryEntry(
    /**
     * 代理卡片
     */
    val agentCard: AgentCard,
    
    /**
     * 注册时间
     */
    val registeredAt: Long,
    
    /**
     * 最后一次看到的时间
     */
    var lastSeenAt: Long
)
