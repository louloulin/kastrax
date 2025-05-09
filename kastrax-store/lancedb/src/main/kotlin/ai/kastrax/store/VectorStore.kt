package ai.kastrax.store

import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.Closeable

/**
 * 向量存储接口，定义了向量存储的基本操作。
 */
interface VectorStore : Closeable {

    /**
     * 创建索引。
     *
     * @param indexName 索引名称
     * @param dimension 向量维度
     * @param metric 相似度度量方式
     * @return 是否成功创建
     */
    suspend fun createIndex(
        indexName: String,
        dimension: Int,
        metric: SimilarityMetric = SimilarityMetric.COSINE
    ): Boolean

    /**
     * 删除索引。
     *
     * @param indexName 索引名称
     * @return 是否成功删除
     */
    suspend fun deleteIndex(indexName: String): Boolean

    /**
     * 列出所有索引。
     *
     * @return 索引名称列表
     */
    suspend fun listIndexes(): List<String>

    /**
     * 获取索引信息。
     *
     * @param indexName 索引名称
     * @return 索引信息
     */
    suspend fun describeIndex(indexName: String): IndexStats

    /**
     * 添加向量。
     *
     * @param indexName 索引名称
     * @param vectors 向量列表
     * @param metadata 元数据列表
     * @param ids ID 列表
     * @return 向量 ID 列表
     */
    suspend fun upsert(
        indexName: String,
        vectors: List<FloatArray>,
        metadata: List<Map<String, Any>> = emptyList(),
        ids: List<String>? = null
    ): List<String>

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
     * @param vector 向量
     * @param metadata 元数据
     * @return 是否成功更新
     */
    suspend fun updateVector(
        indexName: String,
        id: String,
        vector: FloatArray,
        metadata: Map<String, Any> = emptyMap()
    ): Boolean

    /**
     * 删除向量。
     *
     * @param indexName 索引名称
     * @param ids 向量 ID 列表
     * @return 是否成功删除
     */
    suspend fun deleteVectors(
        indexName: String,
        ids: List<String>
    ): Boolean

    /**
     * 查询向量。
     *
     * @param indexName 索引名称
     * @param queryVector 查询向量
     * @param topK 返回结果的最大数量
     * @param filter 过滤条件
     * @return 搜索结果列表，按相似度降序排序
     */
    suspend fun query(
        indexName: String,
        queryVector: FloatArray,
        topK: Int = 10,
        filter: Map<String, Any>? = null
    ): List<SearchResult>

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
        topK: Int = 10,
        filter: Map<String, Any>? = null,
        minScore: Double = 0.0
    ): List<SearchResult>
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
 * 索引统计信息。
 *
 * @property name 索引名称
 * @property dimension 向量维度
 * @property count 向量数量
 * @property metric 相似度度量方式
 */
data class IndexStats(
    val name: String,
    val dimension: Int,
    val count: Long,
    val metric: SimilarityMetric
)
