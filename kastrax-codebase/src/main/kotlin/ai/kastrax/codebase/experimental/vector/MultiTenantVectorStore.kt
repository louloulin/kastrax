package ai.kastrax.codebase.vector

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = KotlinLogging.logger {}

/**
 * 多租户向量存储
 *
 * 支持多租户的代码向量存储，每个租户有独立的向量存储
 *
 * @property dimension 向量维度
 * @property maxTenants 最大租户数量
 * @property tenantStores 租户存储映射表
 * @property lock 读写锁
 */
class MultiTenantVectorStore(
    private val dimension: Int,
    private val maxTenants: Int = 100,
    private val tenantStores: ConcurrentHashMap<String, CodeVectorStore> = ConcurrentHashMap(),
    private val lock: ReentrantReadWriteLock = ReentrantReadWriteLock()
) {
    /**
     * 获取或创建租户存储
     *
     * @param tenantId 租户ID
     * @return 代码向量存储
     */
    fun getOrCreateTenantStore(tenantId: String): CodeVectorStore {
        return lock.read {
            tenantStores[tenantId] ?: lock.write {
                // 双重检查锁定
                tenantStores[tenantId] ?: createTenantStore(tenantId)
            }
        }
    }

    /**
     * 创建租户存储
     *
     * @param tenantId 租户ID
     * @return 代码向量存储
     */
    private fun createTenantStore(tenantId: String): CodeVectorStore {
        // 检查租户数量限制
        if (tenantStores.size >= maxTenants) {
            // 如果超过限制，移除最不活跃的租户
            evictLeastActiveTenant()
        }

        val store = CodeVectorStore(dimension)
        tenantStores[tenantId] = store
        return store
    }

    /**
     * 移除最不活跃的租户
     */
    private fun evictLeastActiveTenant() {
        // 这里可以实现更复杂的淘汰策略，如LRU、LFU等
        // 简单起见，这里只移除第一个租户
        val firstTenant = tenantStores.keys.firstOrNull()
        if (firstTenant != null) {
            removeTenantStore(firstTenant)
        }
    }

    /**
     * 移除租户存储
     *
     * @param tenantId 租户ID
     * @return 是否成功移除
     */
    fun removeTenantStore(tenantId: String): Boolean {
        return lock.write {
            val removed = tenantStores.remove(tenantId) != null
            if (removed) {
                logger.info { "已移除租户存储: $tenantId" }
            }
            removed
        }
    }

    /**
     * 添加向量
     *
     * @param tenantId 租户ID
     * @param id 向量ID
     * @param vector 向量
     * @param element 代码元素
     * @param metadata 元数据
     * @return 是否成功添加
     */
    fun addVector(
        tenantId: String,
        id: String,
        vector: List<Float>,
        element: CodeElement,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean {
        val store = getOrCreateTenantStore(tenantId)
        return store.addVector(id, vector, element, metadata)
    }

    /**
     * 添加向量
     *
     * @param tenantId 租户ID
     * @param element 代码元素
     * @param vector 向量
     * @param metadata 元数据
     * @return 是否成功添加
     */
    fun addVector(
        tenantId: String,
        element: CodeElement,
        vector: List<Float>,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean {
        return addVector(tenantId, element.id, vector, element, metadata)
    }

    /**
     * 批量添加向量
     *
     * @param tenantId 租户ID
     * @param vectors 向量列表
     * @param metadata 元数据映射表
     * @return 成功添加的向量数量
     */
    fun addVectors(
        tenantId: String,
        vectors: List<Triple<String, List<Float>, CodeElement>>,
        metadata: Map<String, Map<String, Any>> = emptyMap()
    ): Int {
        val store = getOrCreateTenantStore(tenantId)
        return store.addVectors(vectors, metadata)
    }

    /**
     * 删除向量
     *
     * @param tenantId 租户ID
     * @param id 向量ID
     * @return 是否成功删除
     */
    fun removeVector(tenantId: String, id: String): Boolean {
        val store = tenantStores[tenantId] ?: return false
        return store.removeVector(id)
    }

    /**
     * 批量删除向量
     *
     * @param tenantId 租户ID
     * @param ids 向量ID列表
     * @return 成功删除的向量数量
     */
    fun removeVectors(tenantId: String, ids: List<String>): Int {
        val store = tenantStores[tenantId] ?: return 0
        return store.removeVectors(ids)
    }

    /**
     * 获取向量
     *
     * @param tenantId 租户ID
     * @param id 向量ID
     * @return 向量
     */
    fun getVector(tenantId: String, id: String): List<Float>? {
        val store = tenantStores[tenantId] ?: return null
        return store.getVector(id)
    }

    /**
     * 获取元数据
     *
     * @param tenantId 租户ID
     * @param id 向量ID
     * @return 元数据
     */
    fun getMetadata(tenantId: String, id: String): Map<String, Any>? {
        val store = tenantStores[tenantId] ?: return null
        return store.getMetadata(id)
    }

    /**
     * 获取代码元素
     *
     * @param tenantId 租户ID
     * @param id 向量ID
     * @return 代码元素
     */
    fun getElement(tenantId: String, id: String): CodeElement? {
        val store = tenantStores[tenantId] ?: return null
        return store.getElement(id)
    }

    /**
     * 获取所有向量ID
     *
     * @param tenantId 租户ID
     * @return 向量ID列表
     */
    fun getAllIds(tenantId: String): List<String> {
        val store = tenantStores[tenantId] ?: return emptyList()
        return store.getAllIds()
    }

    /**
     * 获取向量数量
     *
     * @param tenantId 租户ID
     * @return 向量数量
     */
    fun getVectorCount(tenantId: String): Int {
        val store = tenantStores[tenantId] ?: return 0
        return store.getVectorCount()
    }

    /**
     * 清空租户存储
     *
     * @param tenantId 租户ID
     * @return 是否成功清空
     */
    fun clearTenantStore(tenantId: String): Boolean {
        val store = tenantStores[tenantId] ?: return false
        store.clear()
        return true
    }

    /**
     * 清空所有租户存储
     */
    fun clearAllTenantStores() {
        lock.write {
            tenantStores.values.forEach { it.clear() }
        }
    }

    /**
     * 获取所有租户ID
     *
     * @return 租户ID列表
     */
    fun getAllTenantIds(): List<String> {
        return lock.read {
            tenantStores.keys.toList()
        }
    }

    /**
     * 获取租户数量
     *
     * @return 租户数量
     */
    fun getTenantCount(): Int {
        return lock.read {
            tenantStores.size
        }
    }

    /**
     * 相似度搜索
     *
     * @param tenantId 租户ID
     * @param vector 查询向量
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @param filter 过滤函数
     * @return 相似度结果列表
     */
    fun similaritySearch(
        tenantId: String,
        vector: List<Float>,
        limit: Int = 10,
        minScore: Float = 0.0f,
        filter: ((String, Map<String, Any>, CodeElement) -> Boolean)? = null
    ): List<CodeVectorStore.SimilarityResult> {
        val store = tenantStores[tenantId] ?: return emptyList()
        return store.similaritySearch(vector, limit, minScore, filter)
    }

    /**
     * 相似度搜索（按代码元素类型过滤）
     *
     * @param tenantId 租户ID
     * @param vector 查询向量
     * @param types 代码元素类型列表
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似度结果列表
     */
    fun similaritySearchByTypes(
        tenantId: String,
        vector: List<Float>,
        types: List<CodeElementType>,
        limit: Int = 10,
        minScore: Float = 0.0f
    ): List<CodeVectorStore.SimilarityResult> {
        val store = tenantStores[tenantId] ?: return emptyList()
        return store.similaritySearchByTypes(vector, types, limit, minScore)
    }

    /**
     * 相似度搜索（按元数据过滤）
     *
     * @param tenantId 租户ID
     * @param vector 查询向量
     * @param metadataFilter 元数据过滤条件
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似度结果列表
     */
    fun similaritySearchByMetadata(
        tenantId: String,
        vector: List<Float>,
        metadataFilter: Map<String, Any>,
        limit: Int = 10,
        minScore: Float = 0.0f
    ): List<CodeVectorStore.SimilarityResult> {
        val store = tenantStores[tenantId] ?: return emptyList()
        return store.similaritySearchByMetadata(vector, metadataFilter, limit, minScore)
    }

    /**
     * 相似度搜索（按文件路径过滤）
     *
     * @param tenantId 租户ID
     * @param vector 查询向量
     * @param filePath 文件路径
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似度结果列表
     */
    fun similaritySearchByFilePath(
        tenantId: String,
        vector: List<Float>,
        filePath: String,
        limit: Int = 10,
        minScore: Float = 0.0f
    ): List<CodeVectorStore.SimilarityResult> {
        val store = tenantStores[tenantId] ?: return emptyList()
        return store.similaritySearchByFilePath(vector, filePath, limit, minScore)
    }

    /**
     * 相似度搜索（按语言过滤）
     *
     * @param tenantId 租户ID
     * @param vector 查询向量
     * @param language 编程语言
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似度结果列表
     */
    fun similaritySearchByLanguage(
        tenantId: String,
        vector: List<Float>,
        language: String,
        limit: Int = 10,
        minScore: Float = 0.0f
    ): List<CodeVectorStore.SimilarityResult> {
        val store = tenantStores[tenantId] ?: return emptyList()
        return store.similaritySearchByLanguage(vector, language, limit, minScore)
    }

    /**
     * 相似度搜索（混合查询）
     *
     * @param tenantId 租户ID
     * @param vector 查询向量
     * @param types 代码元素类型列表
     * @param metadataFilter 元数据过滤条件
     * @param language 编程语言
     * @param limit 返回结果数量限制
     * @param minScore 最小相似度分数
     * @return 相似度结果列表
     */
    fun similaritySearchMixed(
        tenantId: String,
        vector: List<Float>,
        types: List<CodeElementType>? = null,
        metadataFilter: Map<String, Any>? = null,
        language: String? = null,
        limit: Int = 10,
        minScore: Float = 0.0f
    ): List<CodeVectorStore.SimilarityResult> {
        val store = tenantStores[tenantId] ?: return emptyList()
        return store.similaritySearchMixed(vector, types, metadataFilter, language, limit, minScore)
    }

    /**
     * 跨租户相似度搜索
     *
     * @param vector 查询向量
     * @param tenantIds 租户ID列表，如果为空则搜索所有租户
     * @param limit 每个租户返回结果数量限制
     * @param minScore 最小相似度分数
     * @param filter 过滤函数
     * @return 相似度结果列表（包含租户ID）
     */
    fun crossTenantSimilaritySearch(
        vector: List<Float>,
        tenantIds: List<String>? = null,
        limit: Int = 10,
        minScore: Float = 0.0f,
        filter: ((String, Map<String, Any>, CodeElement) -> Boolean)? = null
    ): List<CrossTenantSimilarityResult> {
        val results = mutableListOf<CrossTenantSimilarityResult>()
        val targetTenantIds = tenantIds ?: getAllTenantIds()

        targetTenantIds.forEach { tenantId ->
            val tenantResults = similaritySearch(tenantId, vector, limit, minScore, filter)
            tenantResults.forEach { result ->
                results.add(
                    CrossTenantSimilarityResult(
                        tenantId = tenantId,
                        id = result.id,
                        score = result.score,
                        metadata = result.metadata,
                        element = result.element
                    )
                )
            }
        }

        // 按相似度降序排序并限制结果数量
        return results.sortedByDescending { it.score }.take(limit)
    }

    /**
     * 跨租户相似度结果
     *
     * @property tenantId 租户ID
     * @property id 向量ID
     * @property score 相似度分数
     * @property metadata 元数据
     * @property element 代码元素
     */
    data class CrossTenantSimilarityResult(
        val tenantId: String,
        val id: String,
        val score: Float,
        val metadata: Map<String, Any>,
        val element: CodeElement
    )
}
