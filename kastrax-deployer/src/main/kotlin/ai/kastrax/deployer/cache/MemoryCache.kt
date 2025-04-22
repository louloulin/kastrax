package ai.kastrax.deployer.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference

private val logger = KotlinLogging.logger {}

/**
 * 内存缓存项。
 *
 * @property value 值
 * @property expireAt 过期时间
 * @property createdAt 创建时间
 * @property accessedAt 最后访问时间
 */
private data class CacheEntry<V>(
    val value: V,
    val expireAt: Instant?,
    val createdAt: Instant = Instant.now(),
    var accessedAt: AtomicReference<Instant> = AtomicReference(Instant.now())
)

/**
 * 内存缓存实现。
 *
 * @param K 键类型
 * @param V 值类型
 * @property name 缓存名称
 * @property maxSize 最大缓存项数量，0 表示无限制
 * @property defaultTtl 默认过期时间，null 表示永不过期
 */
class MemoryCache<K, V>(
    override val name: String,
    private val maxSize: Int = 0,
    private val defaultTtl: Duration? = null
) : Cache<K, V> {
    
    private val cache = ConcurrentHashMap<K, CacheEntry<V>>()
    private val mutex = Mutex()
    
    // 统计信息
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val evictionCount = AtomicLong(0)
    private val totalLoadTime = AtomicLong(0)
    private val loadCount = AtomicLong(0)
    
    init {
        logger.info { "Initializing memory cache: $name, maxSize: $maxSize, defaultTtl: $defaultTtl" }
    }
    
    override suspend fun get(key: K): V? {
        // 清理过期项
        cleanupExpired()
        
        val entry = cache[key]
        
        if (entry == null) {
            missCount.incrementAndGet()
            return null
        }
        
        // 检查是否过期
        if (isExpired(entry)) {
            mutex.withLock {
                cache.remove(key)
            }
            missCount.incrementAndGet()
            return null
        }
        
        // 更新访问时间
        entry.accessedAt.set(Instant.now())
        hitCount.incrementAndGet()
        
        return entry.value
    }
    
    override suspend fun set(key: K, value: V, ttl: Duration?) {
        val startTime = System.nanoTime()
        
        // 计算过期时间
        val expireAt = if (ttl != null) {
            Instant.now().plus(ttl)
        } else if (defaultTtl != null) {
            Instant.now().plus(defaultTtl)
        } else {
            null
        }
        
        val entry = CacheEntry(value, expireAt)
        
        mutex.withLock {
            // 检查是否需要驱逐
            if (maxSize > 0 && cache.size >= maxSize && !cache.containsKey(key)) {
                evictOldest()
            }
            
            cache[key] = entry
        }
        
        val endTime = System.nanoTime()
        totalLoadTime.addAndGet(endTime - startTime)
        loadCount.incrementAndGet()
    }
    
    override suspend fun delete(key: K): Boolean {
        return mutex.withLock {
            cache.remove(key) != null
        }
    }
    
    override suspend fun exists(key: K): Boolean {
        // 清理过期项
        cleanupExpired()
        
        val entry = cache[key] ?: return false
        
        // 检查是否过期
        if (isExpired(entry)) {
            mutex.withLock {
                cache.remove(key)
            }
            return false
        }
        
        return true
    }
    
    override suspend fun clear() {
        mutex.withLock {
            cache.clear()
        }
    }
    
    override suspend fun size(): Int {
        // 清理过期项
        cleanupExpired()
        
        return cache.size
    }
    
    override suspend fun keys(): Set<K> {
        // 清理过期项
        cleanupExpired()
        
        return cache.keys.toSet()
    }
    
    override suspend fun stats(): CacheStats {
        return CacheStats(
            hitCount = hitCount.get(),
            missCount = missCount.get(),
            evictionCount = evictionCount.get(),
            size = cache.size,
            averageLoadPenalty = if (loadCount.get() > 0) {
                totalLoadTime.get().toDouble() / loadCount.get() / 1_000_000 // 转换为毫秒
            } else {
                0.0
            }
        )
    }
    
    /**
     * 清理过期项。
     */
    private suspend fun cleanupExpired() {
        val now = Instant.now()
        
        mutex.withLock {
            val expiredKeys = cache.entries
                .filter { (_, entry) -> entry.expireAt != null && entry.expireAt.isBefore(now) }
                .map { it.key }
            
            expiredKeys.forEach { key ->
                cache.remove(key)
                evictionCount.incrementAndGet()
            }
        }
    }
    
    /**
     * 驱逐最旧的项。
     */
    private fun evictOldest() {
        if (cache.isEmpty()) return
        
        val oldestKey = cache.entries
            .minByOrNull { it.value.accessedAt.get() }
            ?.key
        
        oldestKey?.let {
            cache.remove(it)
            evictionCount.incrementAndGet()
        }
    }
    
    /**
     * 检查缓存项是否过期。
     *
     * @param entry 缓存项
     * @return 是否过期
     */
    private fun isExpired(entry: CacheEntry<V>): Boolean {
        val expireAt = entry.expireAt ?: return false
        return expireAt.isBefore(Instant.now())
    }
}
