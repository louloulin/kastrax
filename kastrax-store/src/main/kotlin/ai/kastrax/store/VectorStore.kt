package ai.kastrax.store

import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 向量存储接口，提供向量操作和文档操作的统一接口。
 * 参考 mastra 的 MastraVector 设计，结合 kastrax 的 RagVectorStore 接口。
 */
interface VectorStore {
    /**
     * 创建索引。
     *
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @param metric 相似度度量方式，默认为余弦相似度
     * @return 是否成功创建
     */
    suspend fun createIndex(
        indexName: String,
        dimension: Int,
        metric: SimilarityMetric = SimilarityMetric.COSINE
    ): Boolean

    /**
     * 向索引中添加向量。
     *
     * @param indexName 索引名称
     * @param vectors 向量列表
     * @param metadata 元数据列表
     * @param ids ID 列表，如果为 null 则自动生成
     * @return 向量 ID 列表
     */
    suspend fun upsert(
        indexName: String,
        vectors: List<FloatArray>,
        metadata: List<Map<String, Any>> = emptyList(),
        ids: List<String>? = null
    ): List<String>

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
    suspend fun query(
        indexName: String,
        queryVector: FloatArray,
        topK: Int = 10,
        filter: Map<String, Any>? = null,
        includeVectors: Boolean = false
    ): List<QueryResult>

    /**
     * 删除向量。
     *
     * @param indexName 索引名称
     * @param ids ID 列表
     * @return 是否成功删除
     */
    suspend fun deleteVectors(
        indexName: String,
        ids: List<String>
    ): Boolean

    /**
     * 删除索引。
     *
     * @param indexName 索引名称
     * @return 是否成功删除
     */
    suspend fun deleteIndex(
        indexName: String
    ): Boolean

    /**
     * 获取索引信息。
     *
     * @param indexName 索引名称
     * @return 索引信息
     */
    suspend fun describeIndex(
        indexName: String
    ): IndexStats

    /**
     * 列出所有索引。
     *
     * @return 索引名称列表
     */
    suspend fun listIndexes(): List<String>

    /**
     * 使用查询文本进行相似度搜索。
     *
     * @param indexName 索引名称
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param topK 返回结果的最大数量
     * @param filter 过滤条件
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，按相似度降序排序
     */
    suspend fun similaritySearch(
        indexName: String,
        query: String,
        embeddingService: EmbeddingService,
        topK: Int = 5,
        filter: Map<String, Any>? = null,
        minScore: Double = 0.0
    ): List<QueryResult>

    /**
     * 批量添加向量。
     *
     * @param indexName 索引名称
     * @param vectors 向量列表
     * @param metadata 元数据列表
     * @param ids ID 列表
     * @param batchSize 批处理大小
     * @return 向量 ID 列表
     */
    suspend fun batchUpsert(
        indexName: String,
        vectors: List<FloatArray>,
        metadata: List<Map<String, Any>> = emptyList(),
        ids: List<String>? = null,
        batchSize: Int = 100
    ): List<String>

    /**
     * 更新向量。
     *
     * @param indexName 索引名称
     * @param id 向量 ID
     * @param vector 新向量
     * @param metadata 新元数据
     * @return 是否成功更新
     */
    suspend fun updateVector(
        indexName: String,
        id: String,
        vector: FloatArray? = null,
        metadata: Map<String, Any>? = null
    ): Boolean
}

/**
 * 相似度度量方式。
 */
enum class SimilarityMetric {
    /**
     * 余弦相似度。
     */
    COSINE,

    /**
     * 欧几里得距离。
     */
    EUCLIDEAN,

    /**
     * 点积。
     */
    DOT_PRODUCT
}

/**
 * 查询结果。
 *
 * @property id 向量 ID
 * @property score 相似度分数
 * @property metadata 元数据
 * @property vector 向量，如果 includeVectors 为 true 则包含
 */
data class QueryResult(
    val id: String,
    val score: Double,
    val metadata: Map<String, Any>? = null,
    val vector: FloatArray? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as QueryResult

        if (id != other.id) return false
        if (score != other.score) return false
        if (metadata != other.metadata) return false
        if (vector != null) {
            if (other.vector == null) return false
            if (!vector.contentEquals(other.vector)) return false
        } else if (other.vector != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + score.hashCode()
        result = 31 * result + (metadata?.hashCode() ?: 0)
        result = 31 * result + (vector?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * 索引信息。
 *
 * @property dimension 向量维度
 * @property count 向量数量
 * @property metric 相似度度量方式
 */
data class IndexStats(
    val dimension: Int,
    val count: Int,
    val metric: SimilarityMetric? = SimilarityMetric.COSINE
)
