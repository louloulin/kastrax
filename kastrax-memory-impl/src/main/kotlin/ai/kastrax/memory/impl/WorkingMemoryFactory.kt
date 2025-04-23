package ai.kastrax.memory.impl

import ai.kastrax.memory.api.WorkingMemory
import ai.kastrax.memory.api.WorkingMemoryConfig
import redis.clients.jedis.JedisPool
import javax.sql.DataSource

/**
 * 工作内存工厂，用于创建不同类型的工作内存实现。
 */
object WorkingMemoryFactory {
    /**
     * 创建内存中的工作内存实现。
     *
     * @return 内存中的工作内存实现
     */
    fun createInMemoryWorkingMemory(): WorkingMemory {
        return InMemoryWorkingMemory()
    }

    /**
     * 创建Redis工作内存实现。
     *
     * @param jedisPool Redis连接池
     * @param keyPrefix Redis键前缀
     * @param expireTime 过期时间（秒）
     * @return Redis工作内存实现
     */
    fun createRedisWorkingMemory(
        jedisPool: JedisPool,
        keyPrefix: String = "kastrax:working_memory:",
        expireTime: Long = 2592000 // 默认30天过期
    ): WorkingMemory {
        return RedisWorkingMemory(jedisPool, keyPrefix, expireTime)
    }

    /**
     * 创建PostgreSQL工作内存实现。
     *
     * @param dataSource 数据源
     * @param tableName 表名
     * @return PostgreSQL工作内存实现
     */
    fun createPostgresWorkingMemory(
        dataSource: DataSource,
        tableName: String = "working_memory"
    ): WorkingMemory {
        return PostgresWorkingMemory(dataSource, tableName)
    }

    /**
     * 根据配置创建工作内存实现。
     *
     * @param config 工作内存配置
     * @param storage 存储实现（可以是JedisPool或DataSource）
     * @return 工作内存实现
     */
    fun createWorkingMemory(
        config: WorkingMemoryConfig,
        storage: Any? = null
    ): WorkingMemory? {
        println("Creating working memory with config: $config, storage: $storage")
        if (!config.enabled) {
            println("Working memory disabled, returning null")
            return null
        }

        val workingMemory = when (storage) {
            is JedisPool -> {
                println("Creating Redis working memory")
                createRedisWorkingMemory(storage)
            }
            is DataSource -> {
                println("Creating Postgres working memory")
                createPostgresWorkingMemory(storage)
            }
            else -> {
                println("Creating in-memory working memory")
                createInMemoryWorkingMemory()
            }
        }
        println("Created working memory: $workingMemory")
        return workingMemory
    }
}
