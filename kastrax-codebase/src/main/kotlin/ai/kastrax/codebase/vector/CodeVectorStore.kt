package ai.kastrax.codebase.vector

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.VectorStore
import ai.kastrax.store.model.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 代码向量存储
 *
 * 专为代码元素优化的向量存储实现
 *
 * @property baseVectorStore 基础向量存储
 * @property indexName 索引名称
 * @property dimension 向量维度
 * @property metric 相似度度量方式
 */
class CodeVectorStore(
    private val baseVectorStore: VectorStore,
    private val indexName: String = "code-elements",
    private val dimension: Int = 1536,
    private val metric: SimilarityMetric = SimilarityMetric.COSINE
) {
    // 元素缓存
    private val elementCache = ConcurrentHashMap<String, CodeElement>()

    // 是否已初始化
    private var initialized = false

    /**
     * 初始化向量存储
     */
    suspend fun initialize() {
        if (initialized) {
            logger.debug { "代码向量存储已经初始化" }
            return
        }

        try {
            // 创建索引
            baseVectorStore.createIndex(indexName, dimension, metric)
            initialized = true
            logger.info { "代码向量存储初始化成功: $indexName" }
        } catch (e: Exception) {
            logger.error(e) { "初始化代码向量存储失败: ${e.message}" }
            throw e
        }
    }

    /**
     * 添加代码元素
     *
     * @param element 代码元素
     * @param vector 向量
     * @return 是否成功添加
     */
    suspend fun addElement(element: CodeElement, vector: FloatArray): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!initialized) {
                initialize()
            }

            // 创建元数据
            val metadata = createMetadata(element)

            // 添加向量
            val ids = baseVectorStore.upsert(
                indexName = indexName,
                vectors = listOf(vector),
                metadata = listOf(metadata),
                ids = listOf(element.id)
            )

            if (ids.isNotEmpty()) {
                // 缓存元素
                elementCache[element.id] = element
                return@withContext true
            }

            return@withContext false
        } catch (e: Exception) {
            logger.error(e) { "添加代码元素失败: ${element.id}, ${e.message}" }
            return@withContext false
        }
    }

    /**
     * 批量添加代码元素
     *
     * @param elements 代码元素列表
     * @param vectors 向量列表
     * @return 成功添加的元素数量
     */
    suspend fun addElements(elements: List<CodeElement>, vectors: List<FloatArray>): Int = withContext(Dispatchers.IO) {
        try {
            if (elements.isEmpty() || vectors.isEmpty() || elements.size != vectors.size) {
                logger.warn { "批量添加代码元素参数无效: elements=${elements.size}, vectors=${vectors.size}" }
                return@withContext 0
            }

            if (!initialized) {
                initialize()
            }

            // 创建元数据
            val metadata = elements.map { createMetadata(it) }
            val ids = elements.map { it.id }

            // 批量添加向量
            val resultIds = baseVectorStore.upsert(
                indexName = indexName,
                vectors = vectors,
                metadata = metadata,
                ids = ids
            )

            // 缓存元素
            elements.forEach { elementCache[it.id] = it }

            return@withContext resultIds.size
        } catch (e: Exception) {
            logger.error(e) { "批量添加代码元素失败: ${e.message}" }
            return@withContext 0
        }
    }

    /**
     * 删除代码元素
     *
     * @param id 元素ID
     * @return 是否成功删除
     */
    suspend fun deleteElement(id: String): Boolean = withContext(Dispatchers.IO) {
        try {
            if (!initialized) {
                initialize()
            }

            // 删除向量
            val result = baseVectorStore.deleteVectors(indexName, listOf(id))

            // 从缓存中移除
            if (result) {
                elementCache.remove(id)
            }

            return@withContext result
        } catch (e: Exception) {
            logger.error(e) { "删除代码元素失败: $id, ${e.message}" }
            return@withContext false
        }
    }

    /**
     * 批量删除代码元素
     *
     * @param ids 元素ID列表
     * @return 是否成功删除
     */
    suspend fun deleteElements(ids: List<String>): Boolean = withContext(Dispatchers.IO) {
        try {
            if (ids.isEmpty()) {
                logger.warn { "批量删除代码元素参数无效: ids=${ids.size}" }
                return@withContext false
            }

            if (!initialized) {
                initialize()
            }

            // 批量删除向量
            val result = baseVectorStore.deleteVectors(indexName, ids)

            // 从缓存中移除
            if (result) {
                ids.forEach { elementCache.remove(it) }
            }

            return@withContext result
        } catch (e: Exception) {
            logger.error(e) { "批量删除代码元素失败: ${e.message}" }
            return@withContext false
        }
    }

    /**
     * 相似度搜索
     *
     * @param vector 查询向量
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @param filter 过滤函数
     * @return 搜索结果列表
     */
    suspend fun similaritySearch(
        vector: List<Float>,
        limit: Int = 10,
        minScore: Float = 0.0f,
        filter: ((id: String, score: Double, element: CodeElement) -> Boolean)? = null
    ): List<CodeSearchResult> = withContext(Dispatchers.IO) {
        try {
            if (!initialized) {
                initialize()
            }

            // 执行向量搜索
            val results = baseVectorStore.query(
                indexName = indexName,
                queryVector = vector.toFloatArray(),
                topK = limit * 2, // 获取更多结果，以便后续过滤
                filter = null
            )

            // 过滤结果
            val filteredResults = results
                .filter { it.score >= minScore }
                .mapNotNull { result ->
                    val element = getElement(result.id)
                    if (element != null && (filter == null || filter(result.id, result.score, element))) {
                        CodeSearchResult(element, result.score)
                    } else {
                        null
                    }
                }
                .sortedByDescending { it.score }
                .take(limit)

            return@withContext filteredResults
        } catch (e: Exception) {
            logger.error(e) { "相似度搜索失败: ${e.message}" }
            return@withContext emptyList()
        }
    }

    /**
     * 按类型进行相似度搜索
     *
     * @param vector 查询向量
     * @param types 类型列表
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表
     */
    suspend fun similaritySearchByTypes(
        vector: List<Float>,
        types: List<CodeElementType>,
        limit: Int = 10,
        minScore: Float = 0.0f
    ): List<CodeSearchResult> {
        return similaritySearch(
            vector = vector,
            limit = limit,
            minScore = minScore,
            filter = { _, _, element -> element.type in types }
        )
    }

    /**
     * 获取代码元素
     *
     * @param id 元素ID
     * @return 代码元素，如果不存在则返回null
     */
    fun getElement(id: String): CodeElement? {
        return elementCache[id]
    }

    /**
     * 获取所有元素ID
     *
     * @return 元素ID列表
     */
    fun getAllIds(): List<String> {
        return elementCache.keys().toList()
    }

    /**
     * 获取元素数量
     *
     * @return 元素数量
     */
    suspend fun getElementCount(): Int {
        try {
            if (!initialized) {
                initialize()
            }

            val stats = baseVectorStore.describeIndex(indexName)
            return stats.count.toInt()
        } catch (e: Exception) {
            logger.error(e) { "获取元素数量失败: ${e.message}" }
            return 0
        }
    }

    /**
     * 清空向量存储
     *
     * @return 是否成功清空
     */
    suspend fun clear(): Boolean {
        try {
            if (!initialized) {
                return true
            }

            // 删除索引
            val result = baseVectorStore.deleteIndex(indexName)

            // 清空缓存
            if (result) {
                elementCache.clear()
                initialized = false
            }

            return result
        } catch (e: Exception) {
            logger.error(e) { "清空向量存储失败: ${e.message}" }
            return false
        }
    }

    /**
     * 创建元数据
     *
     * @param element 代码元素
     * @return 元数据
     */
    private fun createMetadata(element: CodeElement): Map<String, Any> {
        val metadata = mutableMapOf<String, Any>()

        // 基本信息
        metadata["id"] = element.id
        metadata["name"] = element.name
        metadata["qualifiedName"] = element.qualifiedName
        metadata["type"] = element.type.name
        metadata["language"] = element.language

        // 位置信息
        metadata["filePath"] = element.location.filePath.toString()
        metadata["startLine"] = element.location.startLine
        metadata["startColumn"] = element.location.startColumn
        metadata["endLine"] = element.location.endLine
        metadata["endColumn"] = element.location.endColumn

        // 可见性和修饰符
        metadata["visibility"] = element.visibility.name
        metadata["modifiers"] = element.modifiers.joinToString(",") { it.name }

        // 父元素信息
        element.parent?.let {
            metadata["parentId"] = it.id
            metadata["parentName"] = it.name
            metadata["parentType"] = it.type.name
        }

        // 添加元素自身的元数据
        metadata.putAll(element.metadata.mapValues { it.value.toString() })

        return metadata
    }

    /**
     * 从元数据中提取代码元素
     *
     * @param metadata 元数据
     * @return 代码元素，如果无法提取则返回null
     */
    private fun extractElementFromMetadata(metadata: Map<String, Any>?): CodeElement? {
        if (metadata == null || !metadata.containsKey("id")) {
            return null
        }

        try {
            val id = metadata["id"] as String

            // 如果缓存中存在，直接返回
            val cachedElement = elementCache[id]
            if (cachedElement != null) {
                return cachedElement
            }

            // 否则，尝试从元数据中重建元素
            // 注意：这里只能重建基本信息，无法重建完整的元素结构
            logger.warn { "从元数据中重建代码元素: $id" }
            return null
        } catch (e: Exception) {
            logger.error(e) { "从元数据中提取代码元素失败: ${e.message}" }
            return null
        }
    }

    companion object {
        /**
         * 创建代码向量存储
         *
         * @param vectorStore 基础向量存储
         * @param indexName 索引名称
         * @param dimension 向量维度
         * @param metric 相似度度量方式
         * @return 代码向量存储
         */
        fun create(
            vectorStore: VectorStore,
            indexName: String = "code-elements",
            dimension: Int = 1536,
            metric: SimilarityMetric = SimilarityMetric.COSINE
        ): CodeVectorStore {
            return CodeVectorStore(
                baseVectorStore = vectorStore,
                indexName = indexName,
                dimension = dimension,
                metric = metric
            )
        }
    }
}

/**
 * 代码搜索结果
 *
 * @property element 代码元素
 * @property score 相似度分数
 */
data class CodeSearchResult(
    val element: CodeElement,
    val score: Double
)
