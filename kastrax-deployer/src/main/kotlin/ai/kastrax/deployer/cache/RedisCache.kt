package ai.kastrax.deployer.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import io.lettuce.core.RedisClient
import io.lettuce.core.RedisURI
import io.lettuce.core.api.StatefulRedisConnection
import io.lettuce.core.api.async.RedisAsyncCommands
import kotlinx.coroutines.future.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.time.Duration
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * Redis 缓存配置。
 *
 * @property host Redis 主机
 * @property port Redis 端口
 * @property password Redis 密码，可选
 * @property database Redis 数据库，默认为 0
 * @property keyPrefix 键前缀，默认为空
 */
data class RedisCacheConfig(
    val host: String = "localhost",
    val port: Int = 6379,
    val password: String? = null,
    val database: Int = 0,
    val keyPrefix: String = ""
)

/**
 * Redis 缓存实现。
 *
 * @param K 键类型
 * @param V 值类型
 * @property name 缓存名称
 * @property config Redis 配置
 * @property defaultTtl 默认过期时间，null 表示永不过期
 * @property json JSON 序列化器
 */
class RedisCache<K, V>(
    override val name: String,
    private val config: RedisCacheConfig = RedisCacheConfig(),
    private val defaultTtl: Duration? = null,
    private val json: Json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    },
    private val valueSerializer: KSerializer<V>,
    private val keySerializer: KSerializer<K>
) : Cache<K, V> {

    private val redisClient: RedisClient
    private val connection: StatefulRedisConnection<String, String>
    private val asyncCommands: RedisAsyncCommands<String, String>

    // 统计信息
    private val hitCount = AtomicLong(0)
    private val missCount = AtomicLong(0)
    private val evictionCount = AtomicLong(0)
    private val totalLoadTime = AtomicLong(0)
    private val loadCount = AtomicLong(0)

    init {
        logger.info { "Initializing Redis cache: $name, host: ${config.host}, port: ${config.port}" }

        val redisUri = RedisURI.builder()
            .withHost(config.host)
            .withPort(config.port)
            .apply {
                if (config.password != null) {
                    withPassword(config.password.toCharArray())
                }
                withDatabase(config.database)
            }
            .build()

        redisClient = RedisClient.create(redisUri)
        connection = redisClient.connect()
        asyncCommands = connection.async()
    }

    override suspend fun get(key: K): V? {
        val redisKey = formatKey(key)

        val startTime = System.nanoTime()
        val value = asyncCommands.get(redisKey).await()
        val endTime = System.nanoTime()

        if (value == null) {
            missCount.incrementAndGet()
            return null
        }

        hitCount.incrementAndGet()

        return try {
            json.decodeFromString(valueSerializer, value)
        } catch (e: Exception) {
            logger.error(e) { "Failed to deserialize value for key: $redisKey" }
            null
        }
    }

    override suspend fun set(key: K, value: V, ttl: Duration?) {
        val redisKey = formatKey(key)

        val startTime = System.nanoTime()

        val serializedValue = try {
            json.encodeToString(valueSerializer, value)
        } catch (e: Exception) {
            logger.error(e) { "Failed to serialize value for key: $redisKey" }
            return
        }

        val effectiveTtl = ttl ?: defaultTtl

        if (effectiveTtl != null) {
            asyncCommands.psetex(redisKey, effectiveTtl.toMillis(), serializedValue).await()
        } else {
            asyncCommands.set(redisKey, serializedValue).await()
        }

        val endTime = System.nanoTime()
        totalLoadTime.addAndGet(endTime - startTime)
        loadCount.incrementAndGet()
    }

    override suspend fun delete(key: K): Boolean {
        val redisKey = formatKey(key)
        val result = asyncCommands.del(redisKey).await()
        return result > 0
    }

    override suspend fun exists(key: K): Boolean {
        val redisKey = formatKey(key)
        val result = asyncCommands.exists(redisKey).await()
        return result > 0
    }

    override suspend fun clear() {
        val pattern = if (config.keyPrefix.isEmpty()) {
            "$name:*"
        } else {
            "${config.keyPrefix}$name:*"
        }

        val keys = asyncCommands.keys(pattern).await()

        if (keys.isNotEmpty()) {
            asyncCommands.del(*keys.toTypedArray()).await()
        }
    }

    override suspend fun size(): Int {
        val pattern = if (config.keyPrefix.isEmpty()) {
            "$name:*"
        } else {
            "${config.keyPrefix}$name:*"
        }

        val keys = asyncCommands.keys(pattern).await()
        return keys.size
    }

    override suspend fun keys(): Set<K> {
        val pattern = if (config.keyPrefix.isEmpty()) {
            "$name:*"
        } else {
            "${config.keyPrefix}$name:*"
        }

        val redisKeys = asyncCommands.keys(pattern).await()

        return redisKeys.mapNotNull { redisKey ->
            try {
                val keyStr = redisKey.removePrefix("${config.keyPrefix}$name:")
                json.decodeFromString(keySerializer, keyStr)
            } catch (e: Exception) {
                logger.error(e) { "Failed to parse key: $redisKey" }
                null
            }
        }.toSet()
    }

    override suspend fun stats(): CacheStats {
        return CacheStats(
            hitCount = hitCount.get(),
            missCount = missCount.get(),
            evictionCount = evictionCount.get(),
            size = size(),
            averageLoadPenalty = if (loadCount.get() > 0) {
                totalLoadTime.get().toDouble() / loadCount.get() / 1_000_000 // 转换为毫秒
            } else {
                0.0
            }
        )
    }

    /**
     * 格式化键。
     *
     * @param key 键
     * @return 格式化后的键
     */
    private fun formatKey(key: K): String {
        val keyString = json.encodeToString(keySerializer, key)
        return "${config.keyPrefix}$name:$keyString"
    }

    /**
     * 关闭连接。
     */
    fun close() {
        connection.close()
        redisClient.shutdown()
    }
}
