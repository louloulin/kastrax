package ai.kastrax.store.lancedb

import ai.kastrax.store.BaseVectorStore
import ai.kastrax.store.IndexStats
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.model.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * LanceDB 向量存储实现。
 * 注意：这是一个简化的实现，实际上使用内存存储模拟 LanceDB 的功能。
 * 在生产环境中，应该使用 LanceDB 的官方 Java 客户端。
 *
 * @param uri LanceDB URI，可以是本地路径或远程 URI
 */
class LanceDBVectorStore(
    private val uri: String
) : BaseVectorStore() {

    // 存储索引信息
    private val indexes = ConcurrentHashMap<String, IndexInfo>()

    // 存储向量数据
    private val vectorData = ConcurrentHashMap<String, CopyOnWriteArrayList<VectorEntry>>()

    init {
        // 确保目录存在
        if (!uri.startsWith("http://") && !uri.startsWith("https://")) {
            val directory = File(uri)
            if (!directory.exists()) {
                directory.mkdirs()
            }
        }
        logger.debug { "Initialized LanceDB vector store with uri=$uri" }
    }

    /**
     * 创建索引。
     *
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @param metric 相似度度量方式
     * @return 是否成功创建
     */
    override suspend fun createIndex(
        indexName: String,
        dimension: Int,
        metric: SimilarityMetric
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 检查索引是否已存在
            if (indexes.containsKey(indexName)) {
                logger.debug { "Index $indexName already exists" }
                return@withContext true
            }

            // 创建索引
            indexes[indexName] = IndexInfo(
                name = indexName,
                dimension = dimension,
                metric = metric,
                count = 0L
            )

            // 创建向量数据存储
            vectorData[indexName] = CopyOnWriteArrayList<VectorEntry>()

            logger.debug { "Created index $indexName with dimension $dimension and metric $metric" }
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "Error creating index $indexName" }
            throw e
        }
    }

    /**
     * 删除索引。
     *
     * @param indexName 索引名称
     * @return 是否成功删除
     */
    override suspend fun deleteIndex(indexName: String): Boolean = withContext(Dispatchers.IO) {
        try {
            // 检查索引是否存在
            if (!indexes.containsKey(indexName)) {
                logger.debug { "Index $indexName does not exist" }
                return@withContext true
            }

            // 删除索引
            indexes.remove(indexName)
            vectorData.remove(indexName)

            logger.debug { "Deleted index $indexName" }
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "Error deleting index $indexName" }
            throw e
        }
    }

    /**
     * 列出所有索引。
     *
     * @return 索引名称列表
     */
    override suspend fun listIndexes(): List<String> = withContext(Dispatchers.IO) {
        try {
            val indexNames = indexes.keys.toList()
            logger.debug { "Listed ${indexNames.size} indexes" }
            return@withContext indexNames
        } catch (e: Exception) {
            logger.error(e) { "Error listing indexes" }
            throw e
        }
    }

    /**
     * 获取索引信息。
     *
     * @param indexName 索引名称
     * @return 索引信息
     */
    override suspend fun describeIndex(indexName: String): IndexStats = withContext(Dispatchers.IO) {
        try {
            // 获取索引信息
            val indexInfo = indexes[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")

            // 返回索引信息
            return@withContext IndexStats(
                name = indexName,
                dimension = indexInfo.dimension,
                count = indexInfo.count,
                metric = indexInfo.metric
            )
        } catch (e: Exception) {
            logger.error(e) { "Error describing index $indexName" }
            throw e
        }
    }

    /**
     * 添加向量。
     *
     * @param indexName 索引名称
     * @param vectors 向量列表
     * @param metadata 元数据列表
     * @param ids ID 列表
     * @return 向量 ID 列表
     */
    override suspend fun upsert(
        indexName: String,
        vectors: List<FloatArray>,
        metadata: List<Map<String, Any>>,
        ids: List<String>?
    ): List<String> = withContext(Dispatchers.IO) {
        if (vectors.isEmpty()) {
            return@withContext emptyList()
        }

        try {
            // 获取索引信息
            val indexInfo = indexes[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")

            // 检查向量维度
            for (vector in vectors) {
                if (vector.size != indexInfo.dimension) {
                    throw IllegalArgumentException("Vector dimension (${vector.size}) does not match index dimension (${indexInfo.dimension})")
                }
            }

            // 生成或使用提供的 ID
            val vectorIds = ids ?: List(vectors.size) { generateId() }

            // 确保元数据列表长度与向量列表长度相同
            val normalizedMetadata = if (metadata.size == vectors.size) {
                metadata
            } else {
                List(vectors.size) { i -> metadata.getOrElse(i) { emptyMap() } }
            }

            // 获取向量数据存储
            val vectorList = vectorData[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")

            // 添加向量
            for (i in vectors.indices) {
                // 检查 ID 是否已存在
                val existingIndex = vectorList.indexOfFirst { it.id == vectorIds[i] }
                if (existingIndex >= 0) {
                    // 更新现有向量
                    vectorList[existingIndex] = VectorEntry(
                        id = vectorIds[i],
                        vector = vectors[i],
                        metadata = normalizedMetadata[i]
                    )
                } else {
                    // 添加新向量
                    vectorList.add(
                        VectorEntry(
                            id = vectorIds[i],
                            vector = vectors[i],
                            metadata = normalizedMetadata[i]
                        )
                    )
                }
            }

            // 更新索引计数
            indexes[indexName] = indexInfo.copy(count = vectorList.size.toLong())

            logger.debug { "Added ${vectors.size} vectors to index $indexName" }
            return@withContext vectorIds
        } catch (e: Exception) {
            logger.error(e) { "Error adding vectors to index $indexName" }
            throw e
        }
    }

    /**
     * 更新向量。
     *
     * @param indexName 索引名称
     * @param id 向量 ID
     * @param vector 向量
     * @param metadata 元数据
     * @return 是否成功更新
     */
    override suspend fun updateVector(
        indexName: String,
        id: String,
        vector: FloatArray,
        metadata: Map<String, Any>
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 获取索引信息
            val indexInfo = indexes[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")

            // 检查向量维度
            if (vector.size != indexInfo.dimension) {
                throw IllegalArgumentException("Vector dimension (${vector.size}) does not match index dimension (${indexInfo.dimension})")
            }

            // 获取向量数据存储
            val vectorList = vectorData[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")

            // 查找向量
            val index = vectorList.indexOfFirst { it.id == id }
            if (index < 0) {
                logger.warn { "Vector $id not found in index $indexName" }
                return@withContext false
            }

            // 更新向量
            vectorList[index] = VectorEntry(
                id = id,
                vector = vector,
                metadata = metadata
            )

            logger.debug { "Updated vector $id in index $indexName" }
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "Error updating vector $id in index $indexName" }
            throw e
        }
    }

    /**
     * 删除向量。
     *
     * @param indexName 索引名称
     * @param ids 向量 ID 列表
     * @return 是否成功删除
     */
    override suspend fun deleteVectors(
        indexName: String,
        ids: List<String>
    ): Boolean = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) {
            return@withContext true
        }

        try {
            // 获取索引信息
            val indexInfo = indexes[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")

            // 获取向量数据存储
            val vectorList = vectorData[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")

            // 删除向量
            val initialSize = vectorList.size
            vectorList.removeIf { ids.contains(it.id) }

            // 更新索引计数
            indexes[indexName] = indexInfo.copy(count = vectorList.size.toLong())

            logger.debug { "Deleted ${initialSize - vectorList.size} vectors from index $indexName" }
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "Error deleting vectors from index $indexName" }
            throw e
        }
    }

    /**
     * 查询向量。
     *
     * @param indexName 索引名称
     * @param queryVector 查询向量
     * @param topK 返回结果的最大数量
     * @param filter 过滤条件
     * @return 搜索结果列表，按相似度降序排序
     */
    override suspend fun query(
        indexName: String,
        queryVector: FloatArray,
        topK: Int,
        filter: Map<String, Any>?
    ): List<SearchResult> = withContext(Dispatchers.IO) {
        try {
            // 获取索引信息
            val indexInfo = indexes[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")

            // 检查向量维度
            if (queryVector.size != indexInfo.dimension) {
                throw IllegalArgumentException("Query vector dimension (${queryVector.size}) does not match index dimension (${indexInfo.dimension})")
            }

            // 获取向量数据存储
            val vectorList = vectorData[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")

            // 过滤向量
            val filteredVectors = if (filter != null && filter.isNotEmpty()) {
                vectorList.filter { entry ->
                    filter.all { (key, value) ->
                        entry.metadata.containsKey(key) && entry.metadata[key] == value
                    }
                }
            } else {
                vectorList
            }

            // 计算相似度
            val results = filteredVectors.map { entry ->
                val score = when (indexInfo.metric) {
                    SimilarityMetric.COSINE -> cosineSimilarity(queryVector, entry.vector)
                    SimilarityMetric.EUCLIDEAN -> 1.0 / (1.0 + euclideanDistance(queryVector, entry.vector))
                    SimilarityMetric.DOT_PRODUCT -> dotProduct(queryVector, entry.vector)
                }

                SearchResult(
                    id = entry.id,
                    score = score,
                    metadata = entry.metadata,
                    vector = entry.vector
                )
            }

            // 排序并限制结果数量
            val sortedResults = results.sortedByDescending { it.score }.take(topK)

            logger.debug { "Query returned ${sortedResults.size} results from index $indexName" }
            return@withContext sortedResults
        } catch (e: Exception) {
            logger.error(e) { "Error querying index $indexName" }
            throw e
        }
    }

    /**
     * 创建 ANN 索引。
     *
     * @param indexName 索引名称
     * @param indexType 索引类型
     * @param params 索引参数
     * @return 是否成功创建
     */
    suspend fun createAnnIndex(
        indexName: String,
        indexType: String = "ivf_pq",
        params: Map<String, Any> = emptyMap()
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            // 检查索引是否存在
            if (!indexes.containsKey(indexName)) {
                logger.warn { "Index $indexName does not exist" }
                return@withContext false
            }

            // 模拟创建 ANN 索引
            logger.debug { "Created ANN index for index $indexName with type $indexType" }
            return@withContext true
        } catch (e: Exception) {
            logger.error(e) { "Error creating ANN index for index $indexName" }
            throw e
        }
    }

    /**
     * 关闭资源。
     */
    override fun close() {
        // 清空索引和向量数据
        indexes.clear()
        vectorData.clear()

        logger.debug { "Closed LanceDBVectorStore" }
    }

    /**
     * 索引信息。
     */
    private data class IndexInfo(
        val name: String,
        val dimension: Int,
        val metric: SimilarityMetric,
        val count: Long
    )

    /**
     * 向量条目。
     */
    private data class VectorEntry(
        val id: String,
        val vector: FloatArray,
        val metadata: Map<String, Any>
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as VectorEntry

            if (id != other.id) return false
            if (!vector.contentEquals(other.vector)) return false
            if (metadata != other.metadata) return false

            return true
        }

        override fun hashCode(): Int {
            var result = id.hashCode()
            result = 31 * result + vector.contentHashCode()
            result = 31 * result + metadata.hashCode()
            return result
        }
    }
}
