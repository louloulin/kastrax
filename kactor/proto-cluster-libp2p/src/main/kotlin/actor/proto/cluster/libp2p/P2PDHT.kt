package actor.proto.cluster.libp2p

import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterIdentity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * P2PDHT 负责 Actor 定位
 */
class P2PDHT(
    private val cluster: Cluster,
    private val config: P2PClusterConfig
) {
    companion object {
        private const val DHT_PREFIX = "/protoactor/actor/"
        private const val DEFAULT_RECORD_TTL_SECONDS = 3600L // 1小时
        private const val DEFAULT_REFRESH_INTERVAL_SECONDS = 1800L // 30分钟
        private const val DEFAULT_CLEANUP_INTERVAL_SECONDS = 300L // 5分钟
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isRunning = false

    // 本地 DHT 缓存
    private val localCache = ConcurrentHashMap<String, DHTRecord>()

    // 统计信息
    private val putCount = AtomicInteger(0)
    private val getCount = AtomicInteger(0)
    private val hitCount = AtomicInteger(0)
    private val missCount = AtomicInteger(0)
    private val expiredCount = AtomicInteger(0)
    private val refreshedCount = AtomicInteger(0)

    /**
     * 启动 DHT 服务
     */
    fun start() {
        if (isRunning) return

        logger.info { "Starting P2P DHT" }

        isRunning = true

        // 启动记录刷新任务
        startRecordRefreshTask()

        // 启动记录清理任务
        startRecordCleanupTask()
    }

    /**
     * 停止 DHT 服务
     */
    fun stop() {
        if (!isRunning) return

        logger.info { "Stopping P2P DHT" }

        isRunning = false

        // 清理本地缓存
        localCache.clear()
    }

    /**
     * 启动记录刷新任务
     */
    private fun startRecordRefreshTask() {
        scope.launch {
            while (isRunning) {
                try {
                    // 刷新即将过期的记录
                    refreshExpiringRecords()

                    // 等待下一个刷新间隔
                    delay(Duration.ofSeconds(DEFAULT_REFRESH_INTERVAL_SECONDS).toMillis())
                } catch (e: Exception) {
                    logger.error(e) { "Error refreshing DHT records" }
                    delay(1000) // 出错后等待一段时间再重试
                }
            }
        }
    }

    /**
     * 刷新即将过期的记录
     */
    private fun refreshExpiringRecords() {
        val now = System.currentTimeMillis()
        val refreshThreshold = now + Duration.ofSeconds(DEFAULT_REFRESH_INTERVAL_SECONDS).toMillis()

        // 找出即将过期的记录
        val expiringRecords = localCache.entries
            .filter { it.value.expiresAt < refreshThreshold }
            .map { it.key to it.value }

        if (expiringRecords.isEmpty()) return

        logger.debug { "Refreshing ${expiringRecords.size} expiring DHT records" }

        // 刷新记录
        expiringRecords.forEach { (key, record) ->
            try {
                // 更新过期时间
                val updatedRecord = record.copy(
                    expiresAt = now + Duration.ofSeconds(DEFAULT_RECORD_TTL_SECONDS).toMillis()
                )

                // 更新本地缓存
                localCache[key] = updatedRecord

                // 更新 DHT
                putToDHT(key, record.value)

                // 更新统计信息
                refreshedCount.incrementAndGet()
            } catch (e: Exception) {
                logger.error(e) { "Error refreshing DHT record: $key" }
            }
        }
    }

    /**
     * 启动记录清理任务
     */
    private fun startRecordCleanupTask() {
        scope.launch {
            while (isRunning) {
                try {
                    // 清理过期的记录
                    cleanupExpiredRecords()

                    // 等待下一个清理间隔
                    delay(Duration.ofSeconds(DEFAULT_CLEANUP_INTERVAL_SECONDS).toMillis())
                } catch (e: Exception) {
                    logger.error(e) { "Error cleaning up DHT records" }
                    delay(1000) // 出错后等待一段时间再重试
                }
            }
        }
    }

    /**
     * 清理过期的记录
     */
    private fun cleanupExpiredRecords() {
        val now = System.currentTimeMillis()

        // 找出过期的记录
        val expiredRecords = localCache.entries
            .filter { it.value.expiresAt < now }
            .map { it.key }

        if (expiredRecords.isEmpty()) return

        logger.debug { "Cleaning up ${expiredRecords.size} expired DHT records" }

        // 移除过期的记录
        expiredRecords.forEach { key ->
            localCache.remove(key)

            // 更新统计信息
            expiredCount.incrementAndGet()
        }
    }

    /**
     * 注册 Actor
     */
    suspend fun register(clusterIdentity: ClusterIdentity, pid: PID): Boolean {
        val key = getKey(clusterIdentity)

        try {
            // 序列化 PID
            val value = serializePID(pid)

            // 存储到 DHT
            return put(key, value)
        } catch (e: Exception) {
            logger.error(e) { "Error registering actor: $clusterIdentity" }
            return false
        }
    }

    /**
     * 查找 Actor
     */
    suspend fun lookup(clusterIdentity: ClusterIdentity): PID? {
        val key = getKey(clusterIdentity)

        try {
            // 从 DHT 获取
            val value = get(key) ?: return null

            // 反序列化 PID
            return deserializePID(value)
        } catch (e: Exception) {
            logger.error(e) { "Error looking up actor: $clusterIdentity" }
            return null
        }
    }

    /**
     * 取消注册 Actor
     */
    suspend fun unregister(clusterIdentity: ClusterIdentity): Boolean {
        val key = getKey(clusterIdentity)

        try {
            // 从 DHT 删除
            return remove(key)
        } catch (e: Exception) {
            logger.error(e) { "Error unregistering actor: $clusterIdentity" }
            return false
        }
    }

    /**
     * 存储键值对到 DHT
     */
    suspend fun put(key: String, value: ByteArray): Boolean {
        try {
            // 更新统计信息
            putCount.incrementAndGet()

            // 存储到本地缓存
            val now = System.currentTimeMillis()
            val expiresAt = now + Duration.ofSeconds(DEFAULT_RECORD_TTL_SECONDS).toMillis()
            localCache[key] = DHTRecord(value, now, expiresAt)

            // 存储到 DHT
            return putToDHT(key, value)
        } catch (e: Exception) {
            logger.error(e) { "Error putting to DHT: $key" }
            return false
        }
    }

    /**
     * 从 DHT 获取值
     */
    suspend fun get(key: String): ByteArray? {
        try {
            // 更新统计信息
            getCount.incrementAndGet()

            // 先查询本地缓存
            val cachedRecord = localCache[key]
            if (cachedRecord != null && cachedRecord.expiresAt > System.currentTimeMillis()) {
                // 更新统计信息
                hitCount.incrementAndGet()
                return cachedRecord.value
            }

            // 更新统计信息
            missCount.incrementAndGet()

            // 从 DHT 获取
            val value = getFromDHT(key) ?: return null

            // 存储到本地缓存
            val now = System.currentTimeMillis()
            val expiresAt = now + Duration.ofSeconds(DEFAULT_RECORD_TTL_SECONDS).toMillis()
            localCache[key] = DHTRecord(value, now, expiresAt)

            return value
        } catch (e: Exception) {
            logger.error(e) { "Error getting from DHT: $key" }
            return null
        }
    }

    /**
     * 从 DHT 删除键
     */
    suspend fun remove(key: String): Boolean {
        try {
            // 从本地缓存删除
            localCache.remove(key)

            // 从 DHT 删除
            return removeFromDHT(key)
        } catch (e: Exception) {
            logger.error(e) { "Error removing from DHT: $key" }
            return false
        }
    }

    /**
     * 存储到 DHT
     */
    private fun putToDHT(key: String, value: ByteArray): Boolean {
        // 在实际实现中，这里会使用 libp2p 的 DHT 存储
        // 这里使用模拟实现
        logger.debug { "Putting to DHT: $key with ${value.size} bytes of data" }
        return true
    }

    /**
     * 从 DHT 获取
     */
    private fun getFromDHT(key: String): ByteArray? {
        // 在实际实现中，这里会使用 libp2p 的 DHT 获取
        // 这里使用模拟实现
        logger.debug { "Getting from DHT: $key" }

        // 从本地缓存获取（模拟）
        return localCache[key]?.value
    }

    /**
     * 从 DHT 删除
     */
    private fun removeFromDHT(key: String): Boolean {
        // 在实际实现中，这里会使用 libp2p 的 DHT 删除
        // 这里使用模拟实现
        logger.debug { "Removing from DHT: $key" }
        return true
    }

    /**
     * 获取 Actor 的 DHT 键
     */
    private fun getKey(clusterIdentity: ClusterIdentity): String {
        return "$DHT_PREFIX${clusterIdentity.kind}/${clusterIdentity.identity}"
    }

    /**
     * 序列化 PID
     */
    private fun serializePID(pid: PID): ByteArray {
        val buffer = ByteBuffer.allocate(1024)

        // 地址长度: 2 字节
        val addressBytes = pid.address.toByteArray(StandardCharsets.UTF_8)
        buffer.putShort(addressBytes.size.toShort())

        // 地址
        buffer.put(addressBytes)

        // ID 长度: 2 字节
        val idBytes = pid.id.toByteArray(StandardCharsets.UTF_8)
        buffer.putShort(idBytes.size.toShort())

        // ID
        buffer.put(idBytes)

        // 设置位置并返回
        buffer.flip()
        val result = ByteArray(buffer.remaining())
        buffer.get(result)
        return result
    }

    /**
     * 反序列化 PID
     */
    private fun deserializePID(bytes: ByteArray): PID {
        val buffer = ByteBuffer.wrap(bytes)

        // 读取地址
        val addressLength = buffer.getShort().toInt()
        val addressBytes = ByteArray(addressLength)
        buffer.get(addressBytes)
        val address = String(addressBytes, StandardCharsets.UTF_8)

        // 读取 ID
        val idLength = buffer.getShort().toInt()
        val idBytes = ByteArray(idLength)
        buffer.get(idBytes)
        val id = String(idBytes, StandardCharsets.UTF_8)

        return PID(address, id)
    }

    /**
     * 获取统计信息
     */
    fun getStats(): DHTStats {
        return DHTStats(
            cacheSize = localCache.size,
            putCount = putCount.get(),
            getCount = getCount.get(),
            hitCount = hitCount.get(),
            missCount = missCount.get(),
            expiredCount = expiredCount.get(),
            refreshedCount = refreshedCount.get(),
            hitRatio = if (getCount.get() > 0) hitCount.get() * 100 / getCount.get() else 0
        )
    }
}

/**
 * DHT 记录
 */
data class DHTRecord(
    val value: ByteArray,
    val createdAt: Long,
    val expiresAt: Long
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DHTRecord

        if (!value.contentEquals(other.value)) return false
        if (createdAt != other.createdAt) return false
        if (expiresAt != other.expiresAt) return false

        return true
    }

    override fun hashCode(): Int {
        var result = value.contentHashCode()
        result = 31 * result + createdAt.hashCode()
        result = 31 * result + expiresAt.hashCode()
        return result
    }
}

/**
 * DHT 统计信息
 */
data class DHTStats(
    val cacheSize: Int,
    val putCount: Int,
    val getCount: Int,
    val hitCount: Int,
    val missCount: Int,
    val expiredCount: Int,
    val refreshedCount: Int,
    val hitRatio: Int
)
