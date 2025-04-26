package ai.kastrax.memory.impl

import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryBuilder
import redis.clients.jedis.JedisPool
import javax.sql.DataSource

/**
 * 创建内存实例的工厂类。
 */
object MemoryFactory {
    /**
     * 创建内存实例。
     */
    fun createMemory(init: MemoryBuilderImpl.() -> Unit): Memory {
        val builder = MemoryBuilderImpl()
        builder.init()
        return builder.build()
    }

    /**
     * 创建内存存储。
     */
    fun createInMemoryStorage(): MemoryStorage {
        return InMemoryStorage()
    }

    /**
     * 创建Redis内存存储。
     *
     * @param jedisPool Redis连接池
     * @param keyPrefix Redis键前缀
     * @param expireTime 过期时间（秒）
     */
    fun createRedisMemoryStorage(
        jedisPool: JedisPool,
        keyPrefix: String = "kastrax:memory:",
        expireTime: Long = 7776000 // 默认90天
    ): MemoryStorage {
        return RedisMemoryStorage(jedisPool, keyPrefix, expireTime)
    }

    /**
     * 创建SQLite内存存储。
     *
     * @param dbPath 数据库文件路径
     */
    fun createSQLiteMemoryStorage(dbPath: String): MemoryStorage {
        return SQLiteMemoryStorage(dbPath)
    }

    /**
     * 创建PostgreSQL内存存储。
     *
     * @param dataSource 数据源
     * @param tablePrefix 表前缀
     */
    fun createPostgresMemoryStorage(
        dataSource: DataSource,
        tablePrefix: String = "memory_"
    ): MemoryStorage {
        return PostgresMemoryStorage(dataSource, tablePrefix)
    }
}

/**
 * 创建内存实例的DSL函数。
 */
fun memory(init: MemoryBuilderImpl.() -> Unit): Memory {
    return MemoryFactory.createMemory(init)
}

/**
 * 创建内存存储的DSL函数。
 */
fun inMemoryStorage(): MemoryStorage {
    return MemoryFactory.createInMemoryStorage()
}

/**
 * 创建Redis内存存储的DSL函数。
 */
fun redisMemoryStorage(
    jedisPool: JedisPool,
    keyPrefix: String = "kastrax:memory:",
    expireTime: Long = 7776000 // 默认90天
): MemoryStorage {
    return MemoryFactory.createRedisMemoryStorage(jedisPool, keyPrefix, expireTime)
}

/**
 * 创建SQLite内存存储的DSL函数。
 */
fun sqliteMemoryStorage(dbPath: String): MemoryStorage {
    return MemoryFactory.createSQLiteMemoryStorage(dbPath)
}

/**
 * 创建PostgreSQL内存存储的DSL函数。
 */
fun postgresMemoryStorage(
    dataSource: DataSource,
    tablePrefix: String = "memory_"
): MemoryStorage {
    return MemoryFactory.createPostgresMemoryStorage(dataSource, tablePrefix)
}
