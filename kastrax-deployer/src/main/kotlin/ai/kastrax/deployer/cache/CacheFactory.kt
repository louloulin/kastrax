package ai.kastrax.deployer.cache

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.KSerializer
import kotlinx.serialization.serializer
import java.time.Duration

private val logger = KotlinLogging.logger {}

/**
 * 缓存类型。
 */
enum class CacheType {
    MEMORY,
    REDIS
}

/**
 * 缓存工厂，用于创建不同类型的缓存。
 */
object CacheFactory {

    /**
     * 创建缓存。
     *
     * @param type 缓存类型
     * @param name 缓存名称
     * @param config 缓存配置，可选
     * @param defaultTtl 默认过期时间，可选
     * @return 缓存实例
     */
    inline fun <reified K, reified V> createCache(
        type: CacheType,
        name: String,
        config: Any? = null,
        defaultTtl: Duration? = null
    ): Cache<K, V> {
        return createCache(
            type = type,
            name = name,
            config = config,
            defaultTtl = defaultTtl,
            keySerializer = serializer<K>(),
            valueSerializer = serializer<V>()
        )
    }

    fun <K, V> createCache(
        type: CacheType,
        name: String,
        config: Any? = null,
        defaultTtl: Duration? = null,
        keySerializer: KSerializer<K>,
        valueSerializer: KSerializer<V>
    ): Cache<K, V> {
        logger.info { "Creating cache: $name, type: $type" }

        return when (type) {
            CacheType.MEMORY -> createMemoryCache(name, config, defaultTtl)
            CacheType.REDIS -> createRedisCache(name, config, defaultTtl, keySerializer, valueSerializer)
        }
    }

    /**
     * 创建内存缓存。
     *
     * @param name 缓存名称
     * @param config 缓存配置，可选
     * @param defaultTtl 默认过期时间，可选
     * @return 内存缓存实例
     */
    private fun <K, V> createMemoryCache(
        name: String,
        config: Any?,
        defaultTtl: Duration?
    ): MemoryCache<K, V> {
        val maxSize = when (config) {
            is Int -> config
            is Map<*, *> -> config["maxSize"] as? Int ?: 0
            else -> 0
        }

        return MemoryCache(name, maxSize, defaultTtl)
    }

    /**
     * 创建 Redis 缓存。
     *
     * @param name 缓存名称
     * @param config 缓存配置，可选
     * @param defaultTtl 默认过期时间，可选
     * @return Redis 缓存实例
     */
    private fun <K, V> createRedisCache(
        name: String,
        config: Any?,
        defaultTtl: Duration?,
        keySerializer: KSerializer<K>,
        valueSerializer: KSerializer<V>
    ): RedisCache<K, V> {
        val redisConfig = when (config) {
            is RedisCacheConfig -> config
            is Map<*, *> -> {
                RedisCacheConfig(
                    host = config["host"] as? String ?: "localhost",
                    port = config["port"] as? Int ?: 6379,
                    password = config["password"] as? String,
                    database = config["database"] as? Int ?: 0,
                    keyPrefix = config["keyPrefix"] as? String ?: ""
                )
            }
            else -> RedisCacheConfig()
        }

        return RedisCache(name, redisConfig, defaultTtl, keySerializer = keySerializer, valueSerializer = valueSerializer)
    }
}
