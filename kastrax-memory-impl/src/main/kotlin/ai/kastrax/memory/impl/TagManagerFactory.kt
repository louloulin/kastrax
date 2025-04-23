package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryTagManager
import redis.clients.jedis.JedisPool
import javax.sql.DataSource

/**
 * 标签管理器工厂，用于创建不同类型的标签管理器实现。
 */
object TagManagerFactory {
    /**
     * 创建内存中的标签管理器。
     *
     * @return 内存中的标签管理器
     */
    fun createInMemoryTagManager(): MemoryTagManager {
        return InMemoryTagManager()
    }
    
    /**
     * 创建Redis标签管理器。
     *
     * @param jedisPool Redis连接池
     * @param keyPrefix Redis键前缀
     * @param expireTime 过期时间（秒）
     * @return Redis标签管理器
     */
    fun createRedisTagManager(
        jedisPool: JedisPool,
        keyPrefix: String = "kastrax:tags:",
        expireTime: Long = 2592000 // 默认30天过期
    ): MemoryTagManager {
        return RedisTagManager(jedisPool, keyPrefix, expireTime)
    }
    
    /**
     * 根据存储类型创建标签管理器。
     *
     * @param storage 存储实现（可以是JedisPool或DataSource）
     * @return 标签管理器
     */
    fun createTagManager(storage: Any?): MemoryTagManager {
        return when (storage) {
            is JedisPool -> createRedisTagManager(storage)
            is DataSource -> {
                // 这里可以实现PostgreSQL标签管理器
                // 暂时返回内存实现
                createInMemoryTagManager()
            }
            else -> createInMemoryTagManager()
        }
    }
}
