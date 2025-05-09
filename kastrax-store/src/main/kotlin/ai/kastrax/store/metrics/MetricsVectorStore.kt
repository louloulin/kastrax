package ai.kastrax.store.metrics

import ai.kastrax.store.IndexStats
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.VectorStore
import ai.kastrax.store.model.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 带有指标收集功能的向量存储装饰器。
 *
 * @property delegate 被装饰的向量存储
 */
class MetricsVectorStore(private val delegate: VectorStore) : VectorStore {

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
        val context = VectorStoreMetrics.recordOperationStart(MetricType.CREATE_INDEX, indexName)
        return try {
            val result = delegate.createIndex(indexName, dimension, metric)
            VectorStoreMetrics.recordOperationEnd(context, true)
            result
        } catch (e: Exception) {
            VectorStoreMetrics.recordOperationEnd(context, false)
            throw e
        }
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
        val context = VectorStoreMetrics.recordOperationStart(MetricType.UPSERT, indexName)
        return try {
            val result = delegate.upsert(indexName, vectors, metadata, ids)
            VectorStoreMetrics.recordOperationEnd(context, true)
            result
        } catch (e: Exception) {
            VectorStoreMetrics.recordOperationEnd(context, false)
            throw e
        }
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
        val context = VectorStoreMetrics.recordOperationStart(MetricType.QUERY, indexName)
        return try {
            val result = delegate.query(indexName, queryVector, topK, filter, includeVectors)
            VectorStoreMetrics.recordOperationEnd(context, true)
            result
        } catch (e: Exception) {
            VectorStoreMetrics.recordOperationEnd(context, false)
            throw e
        }
    }

    /**
     * 删除向量。
     *
     * @param indexName 索引名称
     * @param ids ID 列表
     * @return 是否成功删除
     */
    override suspend fun deleteVectors(indexName: String, ids: List<String>): Boolean {
        val context = VectorStoreMetrics.recordOperationStart(MetricType.DELETE, indexName)
        return try {
            val result = delegate.deleteVectors(indexName, ids)
            VectorStoreMetrics.recordOperationEnd(context, true)
            result
        } catch (e: Exception) {
            VectorStoreMetrics.recordOperationEnd(context, false)
            throw e
        }
    }

    /**
     * 删除索引。
     *
     * @param indexName 索引名称
     * @return 是否成功删除
     */
    override suspend fun deleteIndex(indexName: String): Boolean {
        val context = VectorStoreMetrics.recordOperationStart(MetricType.DELETE_INDEX, indexName)
        return try {
            val result = delegate.deleteIndex(indexName)
            VectorStoreMetrics.recordOperationEnd(context, true)
            result
        } catch (e: Exception) {
            VectorStoreMetrics.recordOperationEnd(context, false)
            throw e
        }
    }

    /**
     * 获取索引信息。
     *
     * @param indexName 索引名称
     * @return 索引信息
     */
    override suspend fun describeIndex(indexName: String): IndexStats {
        val context = VectorStoreMetrics.recordOperationStart(MetricType.DESCRIBE_INDEX, indexName)
        return try {
            val result = delegate.describeIndex(indexName)
            VectorStoreMetrics.recordOperationEnd(context, true)
            result
        } catch (e: Exception) {
            VectorStoreMetrics.recordOperationEnd(context, false)
            throw e
        }
    }

    /**
     * 列出所有索引。
     *
     * @return 索引名称列表
     */
    override suspend fun listIndexes(): List<String> {
        val context = VectorStoreMetrics.recordOperationStart(MetricType.LIST_INDEXES)
        return try {
            val result = delegate.listIndexes()
            VectorStoreMetrics.recordOperationEnd(context, true)
            result
        } catch (e: Exception) {
            VectorStoreMetrics.recordOperationEnd(context, false)
            throw e
        }
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
        val context = VectorStoreMetrics.recordOperationStart(MetricType.UPDATE_VECTOR, indexName)
        return try {
            val result = delegate.updateVector(indexName, id, vector, metadata)
            VectorStoreMetrics.recordOperationEnd(context, true)
            result
        } catch (e: Exception) {
            VectorStoreMetrics.recordOperationEnd(context, false)
            throw e
        }
    }

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
    override suspend fun batchUpsert(
        indexName: String,
        vectors: List<FloatArray>,
        metadata: List<Map<String, Any>>,
        ids: List<String>?,
        batchSize: Int
    ): List<String> {
        val context = VectorStoreMetrics.recordOperationStart(MetricType.BATCH_UPSERT, indexName)
        return try {
            val result = delegate.batchUpsert(indexName, vectors, metadata, ids, batchSize)
            VectorStoreMetrics.recordOperationEnd(context, true)
            result
        } catch (e: Exception) {
            VectorStoreMetrics.recordOperationEnd(context, false)
            throw e
        }
    }

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
    override suspend fun similaritySearch(
        indexName: String,
        query: String,
        embeddingService: ai.kastrax.store.embedding.EmbeddingService,
        topK: Int,
        filter: Map<String, Any>?,
        minScore: Double
    ): List<SearchResult> {
        val context = VectorStoreMetrics.recordOperationStart(MetricType.SIMILARITY_SEARCH, indexName)
        return try {
            val result = delegate.similaritySearch(indexName, query, embeddingService, topK, filter, minScore)
            VectorStoreMetrics.recordOperationEnd(context, true)
            result
        } catch (e: Exception) {
            VectorStoreMetrics.recordOperationEnd(context, false)
            throw e
        }
    }
}
