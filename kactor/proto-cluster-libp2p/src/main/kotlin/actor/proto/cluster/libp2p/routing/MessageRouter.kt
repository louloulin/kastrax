package actor.proto.cluster.libp2p.routing

import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * MessageRouter 负责优化消息路由，提高消息传递效率
 */
class MessageRouter(
    private val cluster: Cluster
) {
    companion object {
        private const val DEFAULT_CACHE_SIZE = 10000
        private const val DEFAULT_CACHE_TTL_MS = 60000L // 1分钟
        private const val DEFAULT_CLEANUP_INTERVAL_MS = 30000L // 30秒
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isRunning = false
    
    // 路由缓存
    private val routeCache = ConcurrentHashMap<String, RouteEntry>()
    
    // 统计信息
    private val cacheHits = AtomicInteger(0)
    private val cacheMisses = AtomicInteger(0)
    private val routeComputations = AtomicInteger(0)
    private val routeUpdates = AtomicInteger(0)
    private val routeEvictions = AtomicInteger(0)
    private val totalRoutingTime = AtomicLong(0)
    private val totalMessages = AtomicInteger(0)
    
    /**
     * 启动路由器
     */
    fun start() {
        if (isRunning) return
        
        logger.info { "Starting message router" }
        
        isRunning = true
        
        // 启动缓存清理任务
        startCacheCleanupTask()
    }
    
    /**
     * 停止路由器
     */
    fun stop() {
        if (!isRunning) return
        
        logger.info { "Stopping message router" }
        
        isRunning = false
        
        // 清理缓存
        routeCache.clear()
    }
    
    /**
     * 启动缓存清理任务
     */
    private fun startCacheCleanupTask() {
        scope.launch {
            while (isRunning) {
                try {
                    // 清理过期的路由
                    cleanupExpiredRoutes()
                    
                    // 等待下一个清理间隔
                    delay(DEFAULT_CLEANUP_INTERVAL_MS)
                } catch (e: Exception) {
                    logger.error(e) { "Error cleaning up route cache" }
                    delay(1000) // 出错后等待一段时间再重试
                }
            }
        }
    }
    
    /**
     * 清理过期的路由
     */
    private fun cleanupExpiredRoutes() {
        val now = System.currentTimeMillis()
        val expiredRoutes = mutableListOf<String>()
        
        // 找出过期的路由
        routeCache.forEach { (key, entry) ->
            if (now - entry.lastUsed > DEFAULT_CACHE_TTL_MS) {
                expiredRoutes.add(key)
            }
        }
        
        // 移除过期的路由
        expiredRoutes.forEach { key ->
            routeCache.remove(key)
            routeEvictions.incrementAndGet()
        }
        
        // 如果缓存太大，移除最旧的路由
        if (routeCache.size > DEFAULT_CACHE_SIZE) {
            val routesToRemove = routeCache.size - DEFAULT_CACHE_SIZE
            val sortedRoutes = routeCache.entries
                .sortedBy { it.value.lastUsed }
                .take(routesToRemove)
                .map { it.key }
            
            sortedRoutes.forEach { key ->
                routeCache.remove(key)
                routeEvictions.incrementAndGet()
            }
        }
        
        if (expiredRoutes.isNotEmpty()) {
            logger.debug { "Cleaned up ${expiredRoutes.size} expired routes" }
        }
    }
    
    /**
     * 路由消息
     */
    suspend fun routeMessage(identity: ClusterIdentity): PID {
        totalMessages.incrementAndGet()
        val startTime = System.currentTimeMillis()
        
        try {
            // 生成缓存键
            val cacheKey = "${identity.kind}/${identity.identity}"
            
            // 检查缓存
            val cachedRoute = routeCache[cacheKey]
            if (cachedRoute != null) {
                // 更新最后使用时间
                cachedRoute.lastUsed = System.currentTimeMillis()
                
                // 更新统计信息
                cacheHits.incrementAndGet()
                
                return cachedRoute.pid
            }
            
            // 缓存未命中
            cacheMisses.incrementAndGet()
            
            // 计算路由
            routeComputations.incrementAndGet()
            val pid = computeRoute(identity)
            
            // 缓存路由
            routeCache[cacheKey] = RouteEntry(pid, System.currentTimeMillis())
            routeUpdates.incrementAndGet()
            
            return pid
        } finally {
            // 更新统计信息
            val routingTime = System.currentTimeMillis() - startTime
            totalRoutingTime.addAndGet(routingTime)
        }
    }
    
    /**
     * 计算路由
     */
    private suspend fun computeRoute(identity: ClusterIdentity): PID {
        // 使用集群的身份查找服务
        return cluster.identityLookup.lookup(identity)
    }
    
    /**
     * 更新路由
     */
    fun updateRoute(identity: ClusterIdentity, pid: PID) {
        // 生成缓存键
        val cacheKey = "${identity.kind}/${identity.identity}"
        
        // 更新缓存
        routeCache[cacheKey] = RouteEntry(pid, System.currentTimeMillis())
        routeUpdates.incrementAndGet()
    }
    
    /**
     * 移除路由
     */
    fun removeRoute(identity: ClusterIdentity) {
        // 生成缓存键
        val cacheKey = "${identity.kind}/${identity.identity}"
        
        // 从缓存中移除
        routeCache.remove(cacheKey)
        routeEvictions.incrementAndGet()
    }
    
    /**
     * 获取统计信息
     */
    fun getStats(): RouterStats {
        val totalCount = cacheHits.get() + cacheMisses.get()
        val hitRatio = if (totalCount > 0) cacheHits.get() * 100 / totalCount else 0
        val avgRoutingTime = if (totalMessages.get() > 0) totalRoutingTime.get() / totalMessages.get() else 0
        
        return RouterStats(
            cacheSize = routeCache.size,
            cacheHits = cacheHits.get(),
            cacheMisses = cacheMisses.get(),
            hitRatio = hitRatio,
            routeComputations = routeComputations.get(),
            routeUpdates = routeUpdates.get(),
            routeEvictions = routeEvictions.get(),
            avgRoutingTimeMs = avgRoutingTime
        )
    }
}

/**
 * 路由条目
 */
data class RouteEntry(
    val pid: PID,
    @Volatile var lastUsed: Long
)

/**
 * 路由器统计信息
 */
data class RouterStats(
    val cacheSize: Int,
    val cacheHits: Int,
    val cacheMisses: Int,
    val hitRatio: Int,
    val routeComputations: Int,
    val routeUpdates: Int,
    val routeEvictions: Int,
    val avgRoutingTimeMs: Long
)
