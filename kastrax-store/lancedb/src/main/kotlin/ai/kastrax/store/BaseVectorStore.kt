package ai.kastrax.store

import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.UUID
import kotlin.math.sqrt

/**
 * 向量存储的抽象基类，提供了一些通用实现。
 */
abstract class BaseVectorStore : VectorStore {

    /**
     * 批量添加向量的默认实现。
     * 将向量分批处理，并并行执行 upsert 操作。
     *
     * @param indexName 索引名称
     * @param vectors 向量列表
     * @param metadata 元数据列表
     * @param ids ID 列表
     * @param batchSize 批处理大小
     * @return 向量 ID 列表
     */
    override suspend fun batchUpsert(
        indexName: String,
        vectors: List<FloatArray>,
        metadata: List<Map<String, Any>>,
        ids: List<String>?,
        batchSize: Int
    ): List<String> = coroutineScope {
        if (vectors.isEmpty()) {
            return@coroutineScope emptyList()
        }

        // 生成或使用提供的 ID
        val vectorIds = ids ?: List(vectors.size) { generateId() }

        // 确保元数据列表长度与向量列表长度相同
        val normalizedMetadata = if (metadata.size == vectors.size) {
            metadata
        } else {
            List(vectors.size) { i -> metadata.getOrElse(i) { emptyMap() } }
        }

        // 将向量分批处理
        val batches = vectors.indices.chunked(batchSize)

        // 并行处理每个批次
        val results = batches.map { batchIndices ->
            async {
                val batchVectors = batchIndices.map { vectors[it] }
                val batchMetadata = batchIndices.map { normalizedMetadata[it] }
                val batchIds = batchIndices.map { vectorIds[it] }

                upsert(indexName, batchVectors, batchMetadata, batchIds)
            }
        }.awaitAll()

        // 合并结果
        results.flatten()
    }

    /**
     * 使用查询文本进行相似度搜索的默认实现。
     *
     * @param indexName 索引名称
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param topK 返回结果的最大数量
     * @param filter 过滤条件
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，按相似度降序排序
     */
    override suspend fun similaritySearch(
        indexName: String,
        query: String,
        embeddingService: EmbeddingService,
        topK: Int,
        filter: Map<String, Any>?,
        minScore: Double
    ): List<SearchResult> {
        // 计算查询的嵌入向量
        val queryEmbedding = embeddingService.embed(query)
        
        // 查询向量
        val results = query(indexName, queryEmbedding, topK, filter)
        
        // 过滤低于最小分数的结果
        return results.filter { it.score >= minScore }
    }

    /**
     * 生成唯一 ID。
     *
     * @return 唯一 ID
     */
    protected fun generateId(): String {
        return UUID.randomUUID().toString()
    }

    /**
     * 验证向量维度。
     *
     * @param vectors 向量列表
     * @param dimension 期望的维度
     * @throws IllegalArgumentException 如果向量维度不匹配
     */
    protected fun validateVectorDimensions(vectors: List<FloatArray>, dimension: Int) {
        for (vector in vectors) {
            if (vector.size != dimension) {
                throw IllegalArgumentException("Vector dimension (${vector.size}) does not match index dimension ($dimension)")
            }
        }
    }

    /**
     * 计算余弦相似度。
     *
     * @param a 向量 a
     * @param b 向量 b
     * @return 余弦相似度
     */
    protected fun cosineSimilarity(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }

        var dotProduct = 0.0
        var normA = 0.0
        var normB = 0.0

        for (i in a.indices) {
            dotProduct += a[i] * b[i]
            normA += a[i] * a[i]
            normB += b[i] * b[i]
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0
        }

        return dotProduct / (sqrt(normA) * sqrt(normB))
    }

    /**
     * 计算欧几里得距离。
     *
     * @param a 向量 a
     * @param b 向量 b
     * @return 欧几里得距离
     */
    protected fun euclideanDistance(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }

        var sum = 0.0
        for (i in a.indices) {
            val diff = a[i] - b[i]
            sum += diff * diff
        }

        return sqrt(sum)
    }

    /**
     * 计算点积。
     *
     * @param a 向量 a
     * @param b 向量 b
     * @return 点积
     */
    protected fun dotProduct(a: FloatArray, b: FloatArray): Double {
        if (a.size != b.size) {
            throw IllegalArgumentException("Vectors must have the same dimension")
        }

        var sum = 0.0
        for (i in a.indices) {
            sum += a[i] * b[i]
        }

        return sum
    }
}
