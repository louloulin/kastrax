package ai.kastrax.deployer.cache

import java.time.Duration

/**
 * 缓存接口。
 *
 * @param K 键类型
 * @param V 值类型
 */
interface Cache<K, V> {
    /**
     * 获取缓存名称。
     */
    val name: String
    
    /**
     * 获取缓存项。
     *
     * @param key 键
     * @return 值，如果不存在则返回 null
     */
    suspend fun get(key: K): V?
    
    /**
     * 设置缓存项。
     *
     * @param key 键
     * @param value 值
     * @param ttl 过期时间，可选
     */
    suspend fun set(key: K, value: V, ttl: Duration? = null)
    
    /**
     * 删除缓存项。
     *
     * @param key 键
     * @return 是否成功删除
     */
    suspend fun delete(key: K): Boolean
    
    /**
     * 检查缓存项是否存在。
     *
     * @param key 键
     * @return 是否存在
     */
    suspend fun exists(key: K): Boolean
    
    /**
     * 清空缓存。
     */
    suspend fun clear()
    
    /**
     * 获取缓存大小。
     *
     * @return 缓存项数量
     */
    suspend fun size(): Int
    
    /**
     * 获取所有键。
     *
     * @return 键集合
     */
    suspend fun keys(): Set<K>
    
    /**
     * 获取缓存统计信息。
     *
     * @return 缓存统计信息
     */
    suspend fun stats(): CacheStats
}

/**
 * 缓存统计信息。
 *
 * @property hitCount 命中次数
 * @property missCount 未命中次数
 * @property evictionCount 驱逐次数
 * @property size 缓存大小
 * @property averageLoadPenalty 平均加载时间（毫秒）
 */
data class CacheStats(
    val hitCount: Long = 0,
    val missCount: Long = 0,
    val evictionCount: Long = 0,
    val size: Int = 0,
    val averageLoadPenalty: Double = 0.0
) {
    /**
     * 计算命中率。
     *
     * @return 命中率（0.0-1.0）
     */
    fun hitRate(): Double {
        val requestCount = hitCount + missCount
        return if (requestCount == 0L) 0.0 else hitCount.toDouble() / requestCount
    }
    
    /**
     * 计算未命中率。
     *
     * @return 未命中率（0.0-1.0）
     */
    fun missRate(): Double {
        val requestCount = hitCount + missCount
        return if (requestCount == 0L) 0.0 else missCount.toDouble() / requestCount
    }
}
