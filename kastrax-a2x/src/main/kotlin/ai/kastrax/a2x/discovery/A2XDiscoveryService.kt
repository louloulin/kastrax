package ai.kastrax.a2x.discovery

import ai.kastrax.a2x.model.EntityCard
import ai.kastrax.a2x.model.EntityType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap

/**
 * A2X 发现服务配置
 */
data class A2XDiscoveryConfig(
    /**
     * 发现服务 URL
     */
    val discoveryUrl: String? = null,
    
    /**
     * 是否启用自动发现
     */
    val enableAutoDiscovery: Boolean = false,
    
    /**
     * 自动发现间隔（毫秒）
     */
    val autoDiscoveryIntervalMillis: Long = 60000,
    
    /**
     * 是否启用多播发现
     */
    val enableMulticastDiscovery: Boolean = false,
    
    /**
     * 多播地址
     */
    val multicastAddress: String = "239.255.255.250",
    
    /**
     * 多播端口
     */
    val multicastPort: Int = 1900,
    
    /**
     * 是否启用 DNS-SD 发现
     */
    val enableDnsDiscovery: Boolean = false,
    
    /**
     * DNS-SD 服务类型
     */
    val dnsServiceType: String = "_a2x._tcp",
    
    /**
     * DNS-SD 域
     */
    val dnsDomain: String = "local"
)

/**
 * A2X 发现服务
 */
class A2XDiscoveryService(
    private val config: A2XDiscoveryConfig = A2XDiscoveryConfig(),
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default)
) {
    /**
     * 已注册的服务器
     */
    private val servers = ConcurrentHashMap<String, ServerInfo>()
    
    /**
     * 已发现的实体
     */
    private val entities = ConcurrentHashMap<String, EntityInfo>()
    
    /**
     * 发现事件流
     */
    private val _discoveryEvents = MutableSharedFlow<DiscoveryEvent>()
    val discoveryEvents = _discoveryEvents.asSharedFlow()
    
    /**
     * 注册服务器
     */
    fun registerServer(serverUrl: String, metadata: Map<String, String> = emptyMap()) {
        servers[serverUrl] = ServerInfo(
            url = serverUrl,
            metadata = metadata,
            timestamp = System.currentTimeMillis()
        )
        
        scope.launch {
            _discoveryEvents.emit(DiscoveryEvent.ServerRegistered(serverUrl))
        }
    }
    
    /**
     * 注销服务器
     */
    fun unregisterServer(serverUrl: String) {
        servers.remove(serverUrl)
        
        scope.launch {
            _discoveryEvents.emit(DiscoveryEvent.ServerUnregistered(serverUrl))
        }
    }
    
    /**
     * 获取所有服务器
     */
    fun getAllServers(): List<ServerInfo> {
        return servers.values.toList()
    }
    
    /**
     * 注册实体
     */
    fun registerEntity(entityCard: EntityCard, serverUrl: String) {
        entities[entityCard.id] = EntityInfo(
            entityCard = entityCard,
            serverUrl = serverUrl,
            timestamp = System.currentTimeMillis()
        )
        
        scope.launch {
            _discoveryEvents.emit(DiscoveryEvent.EntityRegistered(entityCard.id, entityCard.type))
        }
    }
    
    /**
     * 注销实体
     */
    fun unregisterEntity(entityId: String) {
        entities.remove(entityId)
        
        scope.launch {
            _discoveryEvents.emit(DiscoveryEvent.EntityUnregistered(entityId))
        }
    }
    
    /**
     * 获取所有实体
     */
    fun getAllEntities(): List<EntityInfo> {
        return entities.values.toList()
    }
    
    /**
     * 获取指定类型的所有实体
     */
    fun getEntitiesByType(entityType: EntityType): List<EntityInfo> {
        return entities.values.filter { it.entityCard.type == entityType }
    }
    
    /**
     * 获取实体
     */
    fun getEntity(entityId: String): EntityInfo? {
        return entities[entityId]
    }
    
    /**
     * 发现服务器
     */
    suspend fun discoverServers() {
        // 实现服务器发现逻辑
        // 可以使用 HTTP、多播或 DNS-SD 等方式
    }
    
    /**
     * 发现实体
     */
    suspend fun discoverEntities(serverUrl: String) {
        // 实现实体发现逻辑
        // 从服务器获取实体列表
    }
}

/**
 * 服务器信息
 */
data class ServerInfo(
    /**
     * 服务器 URL
     */
    val url: String,
    
    /**
     * 服务器元数据
     */
    val metadata: Map<String, String>,
    
    /**
     * 注册时间戳
     */
    val timestamp: Long
)

/**
 * 实体信息
 */
data class EntityInfo(
    /**
     * 实体卡片
     */
    val entityCard: EntityCard,
    
    /**
     * 服务器 URL
     */
    val serverUrl: String,
    
    /**
     * 注册时间戳
     */
    val timestamp: Long
)

/**
 * 发现事件
 */
sealed class DiscoveryEvent {
    /**
     * 服务器注册事件
     */
    data class ServerRegistered(val serverUrl: String) : DiscoveryEvent()
    
    /**
     * 服务器注销事件
     */
    data class ServerUnregistered(val serverUrl: String) : DiscoveryEvent()
    
    /**
     * 实体注册事件
     */
    data class EntityRegistered(val entityId: String, val entityType: EntityType) : DiscoveryEvent()
    
    /**
     * 实体注销事件
     */
    data class EntityUnregistered(val entityId: String) : DiscoveryEvent()
}
