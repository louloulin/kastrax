package ai.kastrax.store.memory

import ai.kastrax.store.BaseVectorStore
import ai.kastrax.store.IndexStats
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.model.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * 基于内存的向量存储实现。
 * 参考 mastra 的 MastraVector 和 kastrax 的 InMemoryVectorStore 实现。
 */
class InMemoryVectorStore(val dimension: Int = 1536) : BaseVectorStore() {
    // 索引存储
    private val indexes = ConcurrentHashMap<String, MutableMap<String, Pair<FloatArray, Map<String, Any>>>>()

    // 索引信息存储
    private val indexStats = ConcurrentHashMap<String, IndexStats>()

    /**
     * 创建索引。
     *
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @param metric 相似度度量方式，默认为余弦相似度
     * @return 是否成功创建
     */
    override suspend fun createIndex(
        indexName: String,
        dimension: Int,
        metric: SimilarityMetric
    ): Boolean {
        if (indexes.containsKey(indexName)) {
            // 检查维度是否匹配
            val stats = indexStats[indexName]
            if (stats != null && stats.dimension != dimension) {
                throw IllegalArgumentException("Index $indexName already exists with dimension ${stats.dimension}, but requested dimension is $dimension")
            }
            if (stats != null && stats.metric != metric) {
                logger.warn { "Index $indexName already exists with metric ${stats.metric}, but requested metric is $metric. Using existing metric." }
            }
            return false
        }

        indexes[indexName] = ConcurrentHashMap()
        indexStats[indexName] = IndexStats(dimension, 0, metric)
        logger.debug { "Created index $indexName with dimension $dimension and metric $metric" }
        return true
    }

    /**
     * 向索引中添加向量。
     *
     * @param indexName 索引名称
     * @param vectors 向量列表
     * @param metadata 元数据列表
     * @param ids ID 列表，如果为 null 则自动生成
     * @return 向量 ID 列表
     */
    override suspend fun upsert(
        indexName: String,
        vectors: List<FloatArray>,
        metadata: List<Map<String, Any>>,
        ids: List<String>?
    ): List<String> {
        val index = indexes[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")
        val stats = indexStats[indexName] ?: throw IllegalArgumentException("Index stats for $indexName not found")

        // 验证向量维度
        validateVectorDimensions(vectors, stats.dimension)

        // 生成或使用提供的 ID
        val vectorIds = ids ?: List(vectors.size) { generateId() }

        // 确保元数据列表长度与向量列表长度相同
        val normalizedMetadata = if (metadata.size == vectors.size) {
            metadata
        } else {
            List(vectors.size) { i -> metadata.getOrElse(i) { emptyMap() } }
        }

        // 添加向量到索引
        for (i in vectors.indices) {
            index[vectorIds[i]] = Pair(vectors[i], normalizedMetadata[i])
        }

        // 更新索引统计信息
        indexStats[indexName] = stats.copy(count = index.size)

        logger.debug { "Upserted ${vectors.size} vectors to index $indexName" }
        return vectorIds
    }

    /**
     * 查询向量。
     *
     * @param indexName 索引名称
     * @param queryVector 查询向量
     * @param topK 返回结果数量
     * @param filter 过滤条件
     * @param includeVectors 是否包含向量
     * @return 查询结果列表
     */
    override suspend fun query(
        indexName: String,
        queryVector: FloatArray,
        topK: Int,
        filter: Map<String, Any>?,
        includeVectors: Boolean
    ): List<SearchResult> {
        val index = indexes[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")
        val stats = indexStats[indexName] ?: throw IllegalArgumentException("Index stats for $indexName not found")

        // 验证查询向量维度
        if (queryVector.size != stats.dimension) {
            throw IllegalArgumentException("Query vector dimension (${queryVector.size}) does not match index dimension (${stats.dimension})")
        }

        // 计算相似度并过滤
        val results = index.entries
            .filter { (_, value) ->
                filter == null || matchesFilter(value.second, filter)
            }
            .map { (id, value) ->
                val similarity = when (stats.metric) {
                    SimilarityMetric.COSINE -> cosineSimilarity(queryVector, value.first)
                    SimilarityMetric.EUCLIDEAN -> 1.0 / (1.0 + euclideanDistance(queryVector, value.first))
                    SimilarityMetric.DOT_PRODUCT -> dotProduct(queryVector, value.first)
                    else -> cosineSimilarity(queryVector, value.first)
                }
                SearchResult(
                    id = id,
                    score = similarity,
                    metadata = value.second,
                    vector = if (includeVectors) value.first else null
                )
            }
            .sortedByDescending { it.score }
            .take(topK)

        logger.debug { "Query returned ${results.size} results from index $indexName" }
        return results
    }

    /**
     * 删除向量。
     *
     * @param indexName 索引名称
     * @param ids ID 列表
     * @return 是否成功删除
     */
    override suspend fun deleteVectors(indexName: String, ids: List<String>): Boolean {
        val index = indexes[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")
        val stats = indexStats[indexName] ?: throw IllegalArgumentException("Index stats for $indexName not found")

        var deleted = false
        for (id in ids) {
            if (index.remove(id) != null) {
                deleted = true
            }
        }

        // 更新索引统计信息
        if (deleted) {
            indexStats[indexName] = stats.copy(count = index.size)
            logger.debug { "Deleted ${ids.size} vectors from index $indexName" }
        }

        return deleted
    }

    /**
     * 删除索引。
     *
     * @param indexName 索引名称
     * @return 是否成功删除
     */
    override suspend fun deleteIndex(indexName: String): Boolean {
        val removed = indexes.remove(indexName) != null
        if (removed) {
            indexStats.remove(indexName)
            logger.debug { "Deleted index $indexName" }
        }
        return removed
    }

    /**
     * 获取索引信息。
     *
     * @param indexName 索引名称
     * @return 索引信息
     */
    override suspend fun describeIndex(indexName: String): IndexStats {
        return indexStats[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")
    }

    /**
     * 相似度搜索。
     *
     * @param indexName 索引名称
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param limit 返回结果数量限制
     * @param filter 过滤条件
     * @param minScore 最小分数
     * @return 搜索结果列表
     */
    override suspend fun similaritySearch(
        indexName: String,
        query: String,
        embeddingService: EmbeddingService,
        topK: Int,
        filter: Map<String, Any>?,
        minScore: Double
    ): List<SearchResult> {
        val queryVector = embeddingService.embed(query)
        return query(indexName, queryVector, topK, filter, false)
            .filter { it.score >= minScore }
    }

    /**
     * 列出所有索引。
     *
     * @return 索引名称列表
     */
    override suspend fun listIndexes(): List<String> {
        return indexes.keys.toList()
    }

    /**
     * 更新向量。
     *
     * @param indexName 索引名称
     * @param id 向量 ID
     * @param vector 新向量
     * @param metadata 新元数据
     * @return 是否成功更新
     */
    override suspend fun updateVector(
        indexName: String,
        id: String,
        vector: FloatArray?,
        metadata: Map<String, Any>?
    ): Boolean {
        val index = indexes[indexName] ?: throw IllegalArgumentException("Index $indexName does not exist")
        val stats = indexStats[indexName] ?: throw IllegalArgumentException("Index stats for $indexName not found")

        // 获取现有向量和元数据
        val existing = index[id] ?: return false
        val existingVector = existing.first
        val existingMetadata = existing.second

        // 如果没有提供新向量和元数据，则不需要更新
        if (vector == null && metadata == null) {
            return true
        }

        // 验证新向量维度
        if (vector != null && vector.size != stats.dimension) {
            throw IllegalArgumentException("Vector dimension (${vector.size}) does not match index dimension (${stats.dimension})")
        }

        // 更新向量和元数据
        val updatedVector = vector ?: existingVector
        val updatedMetadata = if (metadata != null) {
            existingMetadata + metadata
        } else {
            existingMetadata
        }

        // 存储更新后的向量和元数据
        index[id] = Pair(updatedVector, updatedMetadata)
        logger.debug { "Updated vector $id in index $indexName" }

        return true
    }

    /**
     * 检查元数据是否匹配过滤器。
     *
     * @param metadata 元数据
     * @param filter 过滤器
     * @return 是否匹配
     */
    private fun matchesFilter(metadata: Map<String, Any>, filter: Map<String, Any>): Boolean {
        // 所有过滤条件都必须匹配
        return filter.all { (key, value) ->
            when (value) {
                is String -> metadata[key]?.toString()?.equals(value, ignoreCase = true) ?: false
                is Number -> metadata[key]?.toString()?.toDoubleOrNull() == value.toDouble()
                is Boolean -> metadata[key]?.toString()?.toBoolean() == value
                is List<*> -> {
                    val metadataValue = metadata[key]?.toString() ?: return@all false
                    value.any { it.toString().equals(metadataValue, ignoreCase = true) }
                }
                else -> false
            }
        }
    }
}
