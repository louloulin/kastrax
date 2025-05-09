package ai.kastrax.rag.vectorstore

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 增强向量存储接口，提供高级索引和查询功能。
 */
interface EnhancedVectorStore : RagVectorStore {
    /**
     * 使用高级查询选项进行相似度搜索。
     *
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param options 查询选项
     * @return 搜索结果列表
     */
    suspend fun advancedSearch(
        query: String,
        embeddingService: EmbeddingService,
        options: QueryOptions = QueryOptions()
    ): List<SearchResult>

    /**
     * 使用高级查询选项进行向量搜索。
     *
     * @param embedding 查询向量
     * @param options 查询选项
     * @return 搜索结果列表
     */
    suspend fun advancedSearch(
        embedding: FloatArray,
        options: QueryOptions = QueryOptions()
    ): List<SearchResult>

    /**
     * 使用高级查询选项进行混合搜索。
     *
     * @param query 查询文本
     * @param embeddingService 嵌入服务
     * @param keywords 关键词列表
     * @param options 查询选项
     * @return 搜索结果列表
     */
    suspend fun hybridSearch(
        query: String,
        embeddingService: EmbeddingService,
        keywords: List<String>,
        options: QueryOptions = QueryOptions()
    ): List<SearchResult>

    /**
     * 创建索引。
     *
     * @param options 索引选项
     * @return 是否成功
     */
    suspend fun createIndex(options: IndexOptions = IndexOptions()): Boolean

    /**
     * 删除索引。
     *
     * @return 是否成功
     */
    suspend fun deleteIndex(): Boolean

    /**
     * 优化索引。
     *
     * @param options 索引选项
     * @return 是否成功
     */
    suspend fun optimizeIndex(options: IndexOptions = IndexOptions()): Boolean

    /**
     * 获取索引统计信息。
     *
     * @return 索引统计信息
     */
    suspend fun getIndexStats(): IndexStats

    /**
     * 获取文档数量。
     *
     * @return 文档数量
     */
    suspend fun getDocumentCount(): Int

    /**
     * 获取索引大小（字节）。
     *
     * @return 索引大小
     */
    suspend fun getIndexSize(): Long

    /**
     * 获取索引名称。
     *
     * @return 索引名称
     */
    fun getIndexName(): String

    /**
     * 获取索引类型。
     *
     * @return 索引类型
     */
    fun getIndexType(): String

    /**
     * 获取索引版本。
     *
     * @return 索引版本
     */
    fun getIndexVersion(): String

    /**
     * 获取索引状态。
     *
     * @return 索引状态
     */
    suspend fun getIndexStatus(): IndexStatus

    /**
     * 索引选项。
     *
     * @property indexName 索引名称
     * @property dimensions 向量维度
     * @property metadataIndexed 要索引的元数据字段列表
     * @property metadataStored 要存储的元数据字段列表
     * @property chunkSize 分块大小
     * @property similarity 相似度度量
     * @property normalize 是否归一化向量
     * @property async 是否异步索引
     * @property batchSize 批处理大小
     * @property refreshInterval 刷新间隔（毫秒）
     * @property replicas 副本数
     * @property shards 分片数
     */
    data class IndexOptions(
        val indexName: String = "default",
        val dimensions: Int = 1536,
        val metadataIndexed: List<String> = emptyList(),
        val metadataStored: List<String> = emptyList(),
        val chunkSize: Int = 1000,
        val similarity: SimilarityMetric = SimilarityMetric.COSINE,
        val normalize: Boolean = true,
        val async: Boolean = false,
        val batchSize: Int = 100,
        val refreshInterval: Long = 1000,
        val replicas: Int = 1,
        val shards: Int = 1
    )

    /**
     * 查询选项。
     *
     * @property limit 返回结果的最大数量
     * @property offset 偏移量
     * @property minScore 最小相似度分数
     * @property includeMetadata 是否包含元数据
     * @property includeVectors 是否包含向量
     * @property filter 元数据过滤器
     * @property rerank 是否重排序
     * @property rerankSize 重排序大小
     * @property hybridAlpha 混合搜索权重
     * @property timeout 超时时间（毫秒）
     */
    data class QueryOptions(
        val limit: Int = 10,
        val offset: Int = 0,
        val minScore: Double = 0.0,
        val includeMetadata: Boolean = true,
        val includeVectors: Boolean = false,
        val filter: Map<String, Any> = emptyMap(),
        val rerank: Boolean = false,
        val rerankSize: Int = 50,
        val hybridAlpha: Double = 0.5,
        val timeout: Long = 30000
    )

    /**
     * 相似度度量。
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
        DOT_PRODUCT,
        
        /**
         * 曼哈顿距离。
         */
        MANHATTAN
    }

    /**
     * 索引状态。
     */
    enum class IndexStatus {
        /**
         * 创建中。
         */
        CREATING,
        
        /**
         * 活跃。
         */
        ACTIVE,
        
        /**
         * 优化中。
         */
        OPTIMIZING,
        
        /**
         * 删除中。
         */
        DELETING,
        
        /**
         * 不可用。
         */
        UNAVAILABLE
    }

    /**
     * 索引统计信息。
     *
     * @property documentCount 文档数量
     * @property indexSize 索引大小（字节）
     * @property dimensions 向量维度
     * @property createdAt 创建时间
     * @property updatedAt 更新时间
     * @property indexName 索引名称
     * @property indexType 索引类型
     * @property indexVersion 索引版本
     * @property status 索引状态
     * @property memoryUsage 内存使用量（字节）
     * @property diskUsage 磁盘使用量（字节）
     */
    data class IndexStats(
        val documentCount: Int,
        val indexSize: Long,
        val dimensions: Int,
        val createdAt: Long,
        val updatedAt: Long,
        val indexName: String,
        val indexType: String,
        val indexVersion: String,
        val status: IndexStatus,
        val memoryUsage: Long,
        val diskUsage: Long
    )
}
