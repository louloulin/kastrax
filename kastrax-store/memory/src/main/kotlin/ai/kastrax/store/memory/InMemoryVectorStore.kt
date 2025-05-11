package ai.kastrax.store.memory

import ai.kastrax.store.BaseVectorStore
import ai.kastrax.store.IndexStats
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.model.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.math.sqrt

private val logger = KotlinLogging.logger {}

/**
 * 内存向量存储实现
 */
class InMemoryVectorStore : BaseVectorStore() {
    // 索引映射
    private val indexes = mutableMapOf<String, Index>()
    
    // 互斥锁
    private val mutex = Mutex()
    
    /**
     * 创建索引
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
    ): Boolean = mutex.withLock {
        if (indexes.containsKey(indexName)) {
            logger.warn { "Index $indexName already exists" }
            return@withLock false
        }
        
        indexes[indexName] = Index(dimension, metric)
        logger.info { "Created index $indexName with dimension $dimension and metric $metric" }
        return@withLock true
    }
    
    /**
     * 向索引中添加向量
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
    ): List<String> = mutex.withLock {
        val index = getIndex(indexName) ?: return@withLock emptyList()
        
        if (vectors.isEmpty()) {
            logger.warn { "No vectors to upsert" }
            return@withLock emptyList()
        }
        
        // 检查向量维度
        if (vectors.any { it.size != index.dimension }) {
            logger.error { "Vector dimension mismatch. Expected ${index.dimension}" }
            return@withLock emptyList()
        }
        
        // 准备 ID 和元数据
        val actualIds = ids ?: List(vectors.size) { generateId() }
        val actualMetadata = if (metadata.isEmpty()) List(vectors.size) { emptyMap() } else metadata
        
        if (actualIds.size != vectors.size || actualMetadata.size != vectors.size) {
            logger.error { "Size mismatch: vectors=${vectors.size}, ids=${actualIds.size}, metadata=${actualMetadata.size}" }
            return@withLock emptyList()
        }
        
        // 添加向量
        for (i in vectors.indices) {
            index.vectors[actualIds[i]] = vectors[i]
            index.metadata[actualIds[i]] = actualMetadata[i]
        }
        
        logger.debug { "Upserted ${vectors.size} vectors to index $indexName" }
        return@withLock actualIds
    }
    
    /**
     * 查询向量
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
    ): List<SearchResult> = mutex.withLock {
        val index = getIndex(indexName) ?: return@withLock emptyList()
        
        if (queryVector.isEmpty()) {
            logger.warn { "Empty query vector" }
            return@withLock emptyList()
        }
        
        if (queryVector.size != index.dimension && queryVector.isNotEmpty()) {
            logger.error { "Query vector dimension mismatch. Expected ${index.dimension}, got ${queryVector.size}" }
            return@withLock emptyList()
        }
        
        // 计算相似度
        val scores = mutableMapOf<String, Double>()
        
        for ((id, vector) in index.vectors) {
            // 应用过滤条件
            if (filter != null && !matchesFilter(index.metadata[id] ?: emptyMap(), filter)) {
                continue
            }
            
            // 计算相似度
            val similarity = when (index.metric) {
                SimilarityMetric.COSINE -> cosineSimilarity(queryVector, vector)
                SimilarityMetric.EUCLIDEAN -> euclideanSimilarity(queryVector, vector)
                SimilarityMetric.DOT_PRODUCT -> dotProduct(queryVector, vector)
            }
            
            scores[id] = similarity
        }
        
        // 排序并返回结果
        return@withLock scores.entries
            .sortedByDescending { it.value }
            .take(topK)
            .map { (id, score) ->
                SearchResult(
                    id = id,
                    score = score,
                    metadata = index.metadata[id] ?: emptyMap(),
                    vector = if (includeVectors) index.vectors[id] else null
                )
            }
    }
    
    /**
     * 删除向量
     *
     * @param indexName 索引名称
     * @param ids ID 列表
     * @return 是否成功删除
     */
    override suspend fun deleteVectors(
        indexName: String,
        ids: List<String>
    ): Boolean = mutex.withLock {
        val index = getIndex(indexName) ?: return@withLock false
        
        if (ids.isEmpty()) {
            logger.warn { "No IDs to delete" }
            return@withLock false
        }
        
        var deleted = false
        
        for (id in ids) {
            if (index.vectors.remove(id) != null) {
                index.metadata.remove(id)
                deleted = true
            }
        }
        
        logger.debug { "Deleted ${ids.size} vectors from index $indexName" }
        return@withLock deleted
    }
    
    /**
     * 删除索引
     *
     * @param indexName 索引名称
     * @return 是否成功删除
     */
    override suspend fun deleteIndex(
        indexName: String
    ): Boolean = mutex.withLock {
        val removed = indexes.remove(indexName) != null
        
        if (removed) {
            logger.info { "Deleted index $indexName" }
        } else {
            logger.warn { "Index $indexName not found" }
        }
        
        return@withLock removed
    }
    
    /**
     * 获取索引信息
     *
     * @param indexName 索引名称
     * @return 索引信息
     */
    override suspend fun describeIndex(
        indexName: String
    ): IndexStats = mutex.withLock {
        val index = getIndex(indexName)
        
        return@withLock if (index != null) {
            IndexStats(
                dimension = index.dimension,
                count = index.vectors.size,
                metric = index.metric
            )
        } else {
            IndexStats(0, 0)
        }
    }
    
    /**
     * 列出所有索引
     *
     * @return 索引名称列表
     */
    override suspend fun listIndexes(): List<String> = mutex.withLock {
        return@withLock indexes.keys.toList()
    }
    
    /**
     * 获取索引
     *
     * @param indexName 索引名称
     * @return 索引
     */
    private fun getIndex(indexName: String): Index? {
        val index = indexes[indexName]
        
        if (index == null) {
            logger.warn { "Index $indexName not found" }
        }
        
        return index
    }
    
    /**
     * 检查元数据是否匹配过滤条件
     *
     * @param metadata 元数据
     * @param filter 过滤条件
     * @return 是否匹配
     */
    private fun matchesFilter(metadata: Map<String, Any>, filter: Map<String, Any>): Boolean {
        for ((key, value) in filter) {
            val metadataValue = metadata[key]
            
            if (metadataValue == null || metadataValue != value) {
                return false
            }
        }
        
        return true
    }
    
    /**
     * 计算余弦相似度
     *
     * @param vec1 向量 1
     * @param vec2 向量 2
     * @return 余弦相似度
     */
    private fun cosineSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        var dotProduct = 0.0
        var norm1 = 0.0
        var norm2 = 0.0
        
        for (i in vec1.indices) {
            dotProduct += vec1[i] * vec2[i]
            norm1 += vec1[i] * vec1[i]
            norm2 += vec2[i] * vec2[i]
        }
        
        return if (norm1 > 0 && norm2 > 0) {
            dotProduct / (sqrt(norm1) * sqrt(norm2))
        } else {
            0.0
        }
    }
    
    /**
     * 计算欧几里得相似度
     *
     * @param vec1 向量 1
     * @param vec2 向量 2
     * @return 欧几里得相似度
     */
    private fun euclideanSimilarity(vec1: FloatArray, vec2: FloatArray): Double {
        var sum = 0.0
        
        for (i in vec1.indices) {
            val diff = vec1[i] - vec2[i]
            sum += diff * diff
        }
        
        val distance = sqrt(sum)
        
        // 转换为相似度（越小越相似）
        return 1.0 / (1.0 + distance)
    }
    
    /**
     * 计算点积
     *
     * @param vec1 向量 1
     * @param vec2 向量 2
     * @return 点积
     */
    private fun dotProduct(vec1: FloatArray, vec2: FloatArray): Double {
        var sum = 0.0
        
        for (i in vec1.indices) {
            sum += vec1[i] * vec2[i]
        }
        
        return sum
    }
    
    /**
     * 索引
     *
     * @property dimension 向量维度
     * @property metric 相似度度量方式
     * @property vectors 向量映射
     * @property metadata 元数据映射
     */
    private data class Index(
        val dimension: Int,
        val metric: SimilarityMetric,
        val vectors: MutableMap<String, FloatArray> = mutableMapOf(),
        val metadata: MutableMap<String, Map<String, Any>> = mutableMapOf()
    )
}
