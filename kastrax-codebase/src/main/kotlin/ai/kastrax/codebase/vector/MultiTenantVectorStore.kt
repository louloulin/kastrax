package ai.kastrax.codebase.vector

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.VectorStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * 驱逐策略
 */
enum class EvictionStrategy {
    /**
     * 最近最少使用
     */
    LRU,

    /**
     * 最不经常使用
     */
    LFU,

    /**
     * 先进先出
     */
    FIFO
}

/**
 * 多租户向量存储配置
 *
 * @property maxTenantsInMemory 内存中最大租户数量
 * @property maxVectorsPerTenant 每个租户的最大向量数量
 * @property evictionStrategy 驱逐策略
 * @property dimension 向量维度
 * @property metric 相似度度量方式
 */
data class MultiTenantVectorStoreConfig(
    val maxTenantsInMemory: Int = 10,
    val maxVectorsPerTenant: Int = 10000,
    val evictionStrategy: EvictionStrategy = EvictionStrategy.LRU,
    val dimension: Int = 1536,
    val metric: SimilarityMetric = SimilarityMetric.COSINE
)

/**
 * 多租户向量存储
 *
 * 支持多租户的代码向量存储，每个租户有独立的向量存储
 *
 * @property baseVectorStoreFactory 基础向量存储工厂函数
 * @property config 配置
 */
class MultiTenantVectorStore(
    private val baseVectorStoreFactory: (String) -> VectorStore,
    private val config: MultiTenantVectorStoreConfig = MultiTenantVectorStoreConfig()
) {
    // 租户存储映射表
    private val tenantStores = ConcurrentHashMap<String, CodeVectorStore>()

    // 租户访问计数
    private val tenantAccessCount = ConcurrentHashMap<String, AtomicInteger>()

    // 租户最后访问时间
    private val tenantLastAccessTime = ConcurrentHashMap<String, Long>()

    // 租户创建时间
    private val tenantCreationTime = ConcurrentHashMap<String, Long>()

    // 互斥锁
    private val mutex = Mutex()

    /**
     * 获取或创建租户存储
     *
     * @param tenantId 租户ID
     * @return 代码向量存储
     */
    suspend fun getOrCreateTenantStore(tenantId: String): CodeVectorStore = withContext(Dispatchers.IO) {
        // 检查是否已存在
        val existingStore = tenantStores[tenantId]
        if (existingStore != null) {
            // 更新访问统计
            updateTenantAccess(tenantId)
            return@withContext existingStore
        }

        // 创建新存储
        mutex.withLock {
            // 双重检查锁定
            tenantStores[tenantId]?.let {
                updateTenantAccess(tenantId)
                return@withContext it
            }

            // 检查租户数量限制
            if (tenantStores.size >= config.maxTenantsInMemory) {
                // 如果超过限制，移除最不活跃的租户
                evictLeastActiveTenant()
            }

            // 创建新的向量存储
            val baseVectorStore = baseVectorStoreFactory(tenantId)
            val store = CodeVectorStore(
                baseVectorStore = baseVectorStore,
                indexName = "tenant-$tenantId",
                dimension = config.dimension,
                metric = config.metric
            )

            // 初始化存储
            store.initialize()

            // 添加到映射表
            tenantStores[tenantId] = store
            tenantAccessCount[tenantId] = AtomicInteger(0)
            tenantLastAccessTime[tenantId] = System.currentTimeMillis()
            tenantCreationTime[tenantId] = System.currentTimeMillis()

            logger.info { "创建租户存储: $tenantId" }
            return@withContext store
        }
    }

    /**
     * 移除最不活跃的租户
     */
    private suspend fun evictLeastActiveTenant() {
        if (tenantStores.isEmpty()) {
            return
        }

        val tenantToEvict = when (config.evictionStrategy) {
            EvictionStrategy.LRU -> {
                // 最近最少使用
                tenantLastAccessTime.entries
                    .minByOrNull { it.value }
                    ?.key
            }
            EvictionStrategy.LFU -> {
                // 最不经常使用
                tenantAccessCount.entries
                    .minByOrNull { it.value.get() }
                    ?.key
            }
            EvictionStrategy.FIFO -> {
                // 先进先出
                tenantCreationTime.entries
                    .minByOrNull { it.value }
                    ?.key
            }
        }

        if (tenantToEvict != null) {
            removeTenantStore(tenantToEvict)
            logger.info { "驱逐租户存储: $tenantToEvict (策略: ${config.evictionStrategy})" }
        }
    }

    /**
     * 更新租户访问统计
     *
     * @param tenantId 租户ID
     */
    private fun updateTenantAccess(tenantId: String) {
        tenantAccessCount[tenantId]?.incrementAndGet()
        tenantLastAccessTime[tenantId] = System.currentTimeMillis()
    }

    /**
     * 移除租户存储
     *
     * @param tenantId 租户ID
     * @return 是否成功移除
     */
    suspend fun removeTenantStore(tenantId: String): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val store = tenantStores.remove(tenantId)
            if (store != null) {
                // 清空存储
                store.clear()

                // 移除统计信息
                tenantAccessCount.remove(tenantId)
                tenantLastAccessTime.remove(tenantId)
                tenantCreationTime.remove(tenantId)

                logger.info { "移除租户存储: $tenantId" }
                return@withContext true
            }
            return@withContext false
        }
    }

    /**
     * 添加代码元素
     *
     * @param tenantId 租户ID
     * @param element 代码元素
     * @param vector 向量
     * @return 是否成功添加
     */
    suspend fun addElement(tenantId: String, element: CodeElement, vector: FloatArray): Boolean {
        val store = getOrCreateTenantStore(tenantId)
        return store.addElement(element, vector)
    }

    /**
     * 批量添加代码元素
     *
     * @param tenantId 租户ID
     * @param elements 代码元素列表
     * @param vectors 向量列表
     * @return 成功添加的元素数量
     */
    suspend fun addElements(tenantId: String, elements: List<CodeElement>, vectors: List<FloatArray>): Int {
        val store = getOrCreateTenantStore(tenantId)
        return store.addElements(elements, vectors)
    }

    /**
     * 删除代码元素
     *
     * @param tenantId 租户ID
     * @param id 元素ID
     * @return 是否成功删除
     */
    suspend fun deleteElement(tenantId: String, id: String): Boolean {
        val store = tenantStores[tenantId] ?: return false
        return store.deleteElement(id)
    }

    /**
     * 批量删除代码元素
     *
     * @param tenantId 租户ID
     * @param ids 元素ID列表
     * @return 是否成功删除
     */
    suspend fun deleteElements(tenantId: String, ids: List<String>): Boolean {
        val store = tenantStores[tenantId] ?: return false
        return store.deleteElements(ids)
    }

    /**
     * 相似度搜索
     *
     * @param tenantId 租户ID
     * @param vector 查询向量
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param filter 过滤函数
     * @return 搜索结果列表
     */
    suspend fun similaritySearch(
        tenantId: String,
        vector: List<Float>,
        limit: Int = 10,
        minScore: Float = 0.0f,
        filter: ((id: String, score: Double, element: CodeElement) -> Boolean)? = null
    ): List<CodeSearchResult> {
        val store = tenantStores[tenantId] ?: return emptyList()
        return store.similaritySearch(vector, limit, minScore, filter)
    }

    /**
     * 按类型进行相似度搜索
     *
     * @param tenantId 租户ID
     * @param vector 查询向量
     * @param types 类型列表
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    suspend fun similaritySearchByTypes(
        tenantId: String,
        vector: List<Float>,
        types: List<CodeElementType>,
        limit: Int = 10,
        minScore: Float = 0.0f
    ): List<CodeSearchResult> {
        val store = tenantStores[tenantId] ?: return emptyList()
        return store.similaritySearchByTypes(vector, types, limit, minScore)
    }

    /**
     * 跨租户相似度搜索
     *
     * @param vector 查询向量
     * @param tenantIds 租户ID列表，如果为空则搜索所有租户
     * @param limit 每个租户返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param filter 过滤函数
     * @return 搜索结果列表
     */
    suspend fun crossTenantSimilaritySearch(
        vector: List<Float>,
        tenantIds: List<String>? = null,
        limit: Int = 10,
        minScore: Float = 0.0f,
        filter: ((id: String, score: Double, element: CodeElement) -> Boolean)? = null
    ): List<CrossTenantSearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<CrossTenantSearchResult>()
        val targetTenantIds = tenantIds ?: tenantStores.keys().toList()

        for (tenantId in targetTenantIds) {
            val tenantResults = similaritySearch(tenantId, vector, limit, minScore, filter)
            
            // 转换为跨租户结果
            val crossTenantResults = tenantResults.map { result ->
                CrossTenantSearchResult(
                    tenantId = tenantId,
                    element = result.element,
                    score = result.score
                )
            }
            
            results.addAll(crossTenantResults)
        }

        // 按相似度降序排序并限制结果数量
        return@withContext results.sortedByDescending { it.score }.take(limit)
    }

    /**
     * 获取代码元素
     *
     * @param tenantId 租户ID
     * @param id 元素ID
     * @return 代码元素，如果不存在则返回null
     */
    fun getElement(tenantId: String, id: String): CodeElement? {
        val store = tenantStores[tenantId] ?: return null
        return store.getElement(id)
    }

    /**
     * 获取所有元素ID
     *
     * @param tenantId 租户ID
     * @return 元素ID列表
     */
    fun getAllIds(tenantId: String): List<String> {
        val store = tenantStores[tenantId] ?: return emptyList()
        return store.getAllIds()
    }

    /**
     * 获取元素数量
     *
     * @param tenantId 租户ID
     * @return 元素数量
     */
    suspend fun getElementCount(tenantId: String): Int {
        val store = tenantStores[tenantId] ?: return 0
        return store.getElementCount()
    }

    /**
     * 清空租户存储
     *
     * @param tenantId 租户ID
     * @return 是否成功清空
     */
    suspend fun clearTenantStore(tenantId: String): Boolean {
        val store = tenantStores[tenantId] ?: return false
        return store.clear()
    }

    /**
     * 清空所有租户存储
     */
    suspend fun clearAllTenantStores() = withContext(Dispatchers.IO) {
        mutex.withLock {
            val tenantIds = tenantStores.keys().toList()
            for (tenantId in tenantIds) {
                clearTenantStore(tenantId)
            }
            
            // 清空映射表
            tenantStores.clear()
            tenantAccessCount.clear()
            tenantLastAccessTime.clear()
            tenantCreationTime.clear()
            
            logger.info { "清空所有租户存储" }
        }
    }

    /**
     * 获取所有租户ID
     *
     * @return 租户ID列表
     */
    fun getAllTenantIds(): List<String> {
        return tenantStores.keys().toList()
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
     * 获取租户访问计数
     *
     * @param tenantId 租户ID
     * @return 访问计数
     */
    fun getTenantAccessCount(tenantId: String): Int {
        return tenantAccessCount[tenantId]?.get() ?: 0
    }

    /**
     * 获取租户最后访问时间
     *
     * @param tenantId 租户ID
     * @return 最后访问时间
     */
    fun getTenantLastAccessTime(tenantId: String): Long {
        return tenantLastAccessTime[tenantId] ?: 0
    }

    /**
     * 获取租户创建时间
     *
     * @param tenantId 租户ID
     * @return 创建时间
     */
    fun getTenantCreationTime(tenantId: String): Long {
        return tenantCreationTime[tenantId] ?: 0
    }
}

/**
 * 跨租户搜索结果
 *
 * @property tenantId 租户ID
 * @property element 代码元素
 * @property score 相似度分数
 */
data class CrossTenantSearchResult(
    val tenantId: String,
    val element: CodeElement,
    val score: Double
)
