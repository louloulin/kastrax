package ai.kastrax.codebase.store

import ai.kastrax.store.VectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * 多租户向量存储配置
 *
 * @property maxTenantsInMemory 内存中最大租户数量
 * @property maxVectorsPerTenant 每个租户的最大向量数量
 * @property evictionStrategy 驱逐策略
 */
data class MultiTenantVectorStoreConfig(
    val maxTenantsInMemory: Int = 10,
    val maxVectorsPerTenant: Int = 100000,
    val evictionStrategy: EvictionStrategy = EvictionStrategy.LRU
)

/**
 * 驱逐策略
 */
enum class EvictionStrategy {
    /**
     * 最近最少使用
     */
    LRU,
    
    /**
     * 最少使用
     */
    LFU
}

/**
 * 租户统计
 *
 * @property tenantId 租户ID
 * @property vectorCount 向量数量
 * @property lastAccessTime 最后访问时间
 * @property accessCount 访问次数
 */
data class TenantStats(
    val tenantId: String,
    val vectorCount: Int,
    val lastAccessTime: Long,
    val accessCount: Int
)

/**
 * 多租户向量存储
 *
 * 支持多租户索引共享，优化内存使用
 *
 * @property baseVectorStoreFactory 基础向量存储工厂
 * @property config 配置
 */
class MultiTenantVectorStore(
    private val baseVectorStoreFactory: (String) -> VectorStore,
    private val config: MultiTenantVectorStoreConfig = MultiTenantVectorStoreConfig()
) {
    // 租户向量存储映射
    private val tenantStores = ConcurrentHashMap<String, CodeVectorStore>()
    
    // 租户访问统计
    private val tenantAccessTimes = ConcurrentHashMap<String, Long>()
    private val tenantAccessCounts = ConcurrentHashMap<String, AtomicInteger>()
    
    // 互斥锁，用于租户管理
    private val mutex = Mutex()
    
    /**
     * 获取租户向量存储
     *
     * @param tenantId 租户ID
     * @return 代码向量存储
     */
    suspend fun getTenantStore(tenantId: String): CodeVectorStore = mutex.withLock {
        // 更新访问统计
        tenantAccessTimes[tenantId] = System.currentTimeMillis()
        tenantAccessCounts.computeIfAbsent(tenantId) { AtomicInteger(0) }.incrementAndGet()
        
        // 如果租户存储已存在，直接返回
        val existingStore = tenantStores[tenantId]
        if (existingStore != null) {
            return@withLock existingStore
        }
        
        // 如果达到最大租户数量，需要驱逐一个租户
        if (tenantStores.size >= config.maxTenantsInMemory) {
            evictTenant()
        }
        
        // 创建新的租户存储
        val baseStore = baseVectorStoreFactory(tenantId)
        val codeStore = CodeVectorStore(
            baseVectorStore = baseStore,
            config = CodeVectorStoreConfig(
                maxVectors = config.maxVectorsPerTenant,
                indexName = "code_vectors_$tenantId",
                userId = tenantId
            )
        )
        
        // 添加到映射
        tenantStores[tenantId] = codeStore
        
        logger.info { "创建租户存储: $tenantId" }
        
        return@withLock codeStore
    }
    
    /**
     * 添加向量
     *
     * @param tenantId 租户ID
     * @param vector 向量数据
     * @param metadata 元数据
     * @return 向量ID
     */
    suspend fun addVector(
        tenantId: String,
        vector: FloatArray,
        metadata: Map<String, Any> = emptyMap()
    ): String = withContext(Dispatchers.Default) {
        val store = getTenantStore(tenantId)
        return@withContext store.addVector(vector, metadata)
    }
    
    /**
     * 批量添加向量
     *
     * @param tenantId 租户ID
     * @param vectors 向量数据列表
     * @param metadata 元数据列表
     * @return 向量ID列表
     */
    suspend fun addVectors(
        tenantId: String,
        vectors: List<FloatArray>,
        metadata: List<Map<String, Any>> = List(vectors.size) { emptyMap() }
    ): List<String> = withContext(Dispatchers.Default) {
        val store = getTenantStore(tenantId)
        return@withContext store.addVectors(vectors, metadata)
    }
    
    /**
     * 添加嵌入
     *
     * @param tenantId 租户ID
     * @param text 文本
     * @param metadata 元数据
     * @param embeddingService 嵌入服务
     * @return 向量ID
     */
    suspend fun addEmbedding(
        tenantId: String,
        text: String,
        metadata: Map<String, Any> = emptyMap(),
        embeddingService: EmbeddingService
    ): String = withContext(Dispatchers.Default) {
        val store = getTenantStore(tenantId)
        return@withContext store.addEmbedding(text, metadata, embeddingService)
    }
    
    /**
     * 批量添加嵌入
     *
     * @param tenantId 租户ID
     * @param texts 文本列表
     * @param metadata 元数据列表
     * @param embeddingService 嵌入服务
     * @return 向量ID列表
     */
    suspend fun addEmbeddings(
        tenantId: String,
        texts: List<String>,
        metadata: List<Map<String, Any>> = List(texts.size) { emptyMap() },
        embeddingService: EmbeddingService
    ): List<String> = withContext(Dispatchers.Default) {
        val store = getTenantStore(tenantId)
        return@withContext store.addEmbeddings(texts, metadata, embeddingService)
    }
    
    /**
     * 搜索向量
     *
     * @param tenantId 租户ID
     * @param vector 查询向量
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    suspend fun searchVector(
        tenantId: String,
        vector: FloatArray,
        limit: Int = 10,
        minScore: Double = 0.0
    ): List<CodeSearchResult> = withContext(Dispatchers.Default) {
        val store = getTenantStore(tenantId)
        return@withContext store.searchVector(vector, limit, minScore)
    }
    
    /**
     * 搜索嵌入
     *
     * @param tenantId 租户ID
     * @param text 查询文本
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @param embeddingService 嵌入服务
     * @return 搜索结果列表
     */
    suspend fun searchEmbedding(
        tenantId: String,
        text: String,
        limit: Int = 10,
        minScore: Double = 0.0,
        embeddingService: EmbeddingService
    ): List<CodeSearchResult> = withContext(Dispatchers.Default) {
        val store = getTenantStore(tenantId)
        return@withContext store.searchEmbedding(text, limit, minScore, embeddingService)
    }
    
    /**
     * 删除向量
     *
     * @param tenantId 租户ID
     * @param id 向量ID
     * @return 是否成功删除
     */
    suspend fun deleteVector(
        tenantId: String,
        id: String
    ): Boolean = withContext(Dispatchers.Default) {
        val store = getTenantStore(tenantId)
        return@withContext store.deleteVector(id)
    }
    
    /**
     * 批量删除向量
     *
     * @param tenantId 租户ID
     * @param ids 向量ID列表
     * @return 是否成功删除
     */
    suspend fun deleteVectors(
        tenantId: String,
        ids: List<String>
    ): Boolean = withContext(Dispatchers.Default) {
        val store = getTenantStore(tenantId)
        return@withContext store.deleteVectors(ids)
    }
    
    /**
     * 清空租户存储
     *
     * @param tenantId 租户ID
     * @return 是否成功清空
     */
    suspend fun clearTenant(tenantId: String): Boolean = mutex.withLock {
        val store = tenantStores[tenantId] ?: return@withLock false
        
        // 清空存储
        val success = store.clear()
        
        // 从映射中移除
        tenantStores.remove(tenantId)
        tenantAccessTimes.remove(tenantId)
        tenantAccessCounts.remove(tenantId)
        
        logger.info { "清空租户存储: $tenantId" }
        
        return@withLock success
    }
    
    /**
     * 获取租户统计
     *
     * @return 租户统计列表
     */
    suspend fun getTenantStats(): List<TenantStats> = mutex.withLock {
        return@withLock tenantStores.keys.map { tenantId ->
            val store = tenantStores[tenantId]
            val vectorCount = store?.getVectorCount() ?: 0
            val lastAccessTime = tenantAccessTimes[tenantId] ?: 0
            val accessCount = tenantAccessCounts[tenantId]?.get() ?: 0
            
            TenantStats(
                tenantId = tenantId,
                vectorCount = vectorCount,
                lastAccessTime = lastAccessTime,
                accessCount = accessCount
            )
        }
    }
    
    /**
     * 驱逐租户
     */
    private suspend fun evictTenant() {
        // 根据驱逐策略选择要驱逐的租户
        val tenantToEvict = when (config.evictionStrategy) {
            EvictionStrategy.LRU -> {
                // 选择最近最少使用的租户
                tenantAccessTimes.entries
                    .minByOrNull { it.value }
                    ?.key
            }
            EvictionStrategy.LFU -> {
                // 选择最少使用的租户
                tenantAccessCounts.entries
                    .minByOrNull { it.value.get() }
                    ?.key
            }
        }
        
        if (tenantToEvict != null) {
            // 从映射中移除
            val store = tenantStores.remove(tenantToEvict)
            tenantAccessTimes.remove(tenantToEvict)
            tenantAccessCounts.remove(tenantToEvict)
            
            logger.info { "驱逐租户: $tenantToEvict" }
        }
    }
    
    /**
     * 获取租户数量
     *
     * @return 租户数量
     */
    fun getTenantCount(): Int {
        return tenantStores.size
    }
    
    /**
     * 获取所有租户ID
     *
     * @return 租户ID列表
     */
    fun getAllTenantIds(): List<String> {
        return tenantStores.keys.toList()
    }
}
