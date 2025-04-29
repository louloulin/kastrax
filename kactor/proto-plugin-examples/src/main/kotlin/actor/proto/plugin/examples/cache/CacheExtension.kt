package actor.proto.plugin.examples.cache

import actor.proto.ActorSystem
import actor.proto.Context
import actor.proto.PID
import actor.proto.Receive
import actor.proto.ReceiveMiddleware
import actor.proto.Send
import actor.proto.SenderMiddleware
import actor.proto.plugin.ReceiveMiddlewareExtension
import actor.proto.plugin.SenderMiddlewareExtension
import org.pf4j.Extension
import org.slf4j.LoggerFactory
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * 缓存条目
 * @param value 缓存值
 * @param expireAt 过期时间
 */
data class CacheEntry<T>(
    val value: T,
    val expireAt: Long
)

/**
 * 缓存管理器
 * 管理所有缓存
 */
object CacheManager {
    private val logger = LoggerFactory.getLogger(CacheManager::class.java)
    private val caches = ConcurrentHashMap<String, MutableMap<Any, CacheEntry<Any>>>()
    private val scheduler: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

    init {
        // 定期清理过期缓存
        scheduler.scheduleAtFixedRate(
            { cleanupExpiredEntries() },
            1,
            1,
            TimeUnit.MINUTES
        )
    }

    /**
     * 获取缓存
     * @param cacheId 缓存ID
     * @param key 缓存键
     * @return 缓存值，如果不存在或已过期则返回null
     */
    fun get(cacheId: String, key: Any): Any? {
        val cache = caches[cacheId] ?: return null
        val entry = cache[key] ?: return null

        // 检查是否过期
        if (entry.expireAt < System.currentTimeMillis()) {
            cache.remove(key)
            return null
        }

        return entry.value
    }

    /**
     * 设置缓存
     * @param cacheId 缓存ID
     * @param key 缓存键
     * @param value 缓存值
     * @param ttl 生存时间
     */
    fun set(cacheId: String, key: Any, value: Any, ttl: Duration) {
        val cache = caches.computeIfAbsent(cacheId) { ConcurrentHashMap() }
        val expireAt = System.currentTimeMillis() + ttl.toMillis()
        cache[key] = CacheEntry(value, expireAt)
    }

    /**
     * 清除缓存
     * @param cacheId 缓存ID
     * @param key 缓存键
     */
    fun remove(cacheId: String, key: Any) {
        val cache = caches[cacheId] ?: return
        cache.remove(key)
    }

    /**
     * 清除所有缓存
     */
    fun clear() {
        caches.clear()
    }

    /**
     * 清理过期条目
     */
    private fun cleanupExpiredEntries() {
        val now = System.currentTimeMillis()
        caches.forEach { (_, cache) ->
            val expiredKeys = cache.entries
                .filter { it.value.expireAt < now }
                .map { it.key }

            expiredKeys.forEach { cache.remove(it) }
        }
    }

    /**
     * 关闭缓存管理器
     */
    fun shutdown() {
        scheduler.shutdown()
        caches.clear()
    }
}

/**
 * 缓存接收中间件扩展
 * 用于缓存Actor的响应
 */
@Extension
class CacheReceiveMiddleware : ReceiveMiddlewareExtension {
    private val logger = LoggerFactory.getLogger(CacheReceiveMiddleware::class.java)

    override fun id(): String = "cache-receive-middleware"

    override fun version(): String = "1.0.0"

    override fun init(system: ActorSystem) {
        logger.info("Cache receive middleware initialized")
    }

    override fun shutdown() {
        logger.info("Cache receive middleware shutdown")
    }

    // 默认缓存配置
    private val defaultTtl = Duration.ofMinutes(5)

    override fun getReceiveMiddleware(): ReceiveMiddleware = { next ->
        { ctx ->
            val actorType = ctx.actor.javaClass.simpleName
            val messageType = ctx.message.javaClass.simpleName
            val cacheId = "receive:$actorType"

            // 检查是否可缓存
            if (isCacheable(ctx.message)) {
                // 尝试从缓存获取
                val cacheKey = getCacheKey(ctx.message)
                val cachedResponse = CacheManager.get(cacheId, cacheKey)

                if (cachedResponse != null) {
                    // 使用缓存的响应
                    logger.debug("Cache hit for actor: {}, message: {}", actorType, messageType)
                    ctx.respond(cachedResponse)
                } else {
                    // 处理消息
                    next(ctx)

                    // 缓存响应
                    if (ctx.sender != null) {
                        // 注意：这里假设响应已经通过ctx.respond发送
                        // 实际上，我们需要一种方式来捕获响应
                        // 这里只是一个示例，实际实现可能需要更复杂的机制
                    }
                }
            } else {
                // 不可缓存，直接处理
                next(ctx)
            }
        }
    }

    /**
     * 检查消息是否可缓存
     * @param message 消息
     * @return 如果可缓存则返回true，否则返回false
     */
    private fun isCacheable(message: Any): Boolean {
        // 这里可以根据消息类型或内容决定是否可缓存
        // 例如，只缓存查询类消息，不缓存命令类消息
        return message is String && message.startsWith("query:")
    }

    /**
     * 获取缓存键
     * @param message 消息
     * @return 缓存键
     */
    private fun getCacheKey(message: Any): Any {
        // 这里可以根据消息内容生成缓存键
        // 例如，对于查询消息，可以使用查询参数作为键
        return message.hashCode()
    }
}

/**
 * 缓存注解
 * 用于标记可缓存的消息
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class Cacheable(
    val ttl: Long = 300, // 默认5分钟
    val timeUnit: TimeUnit = TimeUnit.SECONDS
)
