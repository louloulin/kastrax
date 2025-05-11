package ai.kastrax.rag.realtime

import ai.kastrax.rag.context.ContextBuilderConfig

/**
 * 实时 RAG 配置选项
 *
 * @property streamingEnabled 是否启用流式处理
 * @property updateInterval 更新间隔（毫秒），仅在非流式模式下使用
 * @property maxBatchSize 最大批处理大小
 * @property maxLatency 最大延迟（毫秒）
 * @property useAsyncEmbedding 是否使用异步嵌入生成
 * @property useIncrementalIndexing 是否使用增量索引
 * @property useChangeDetection 是否使用变更检测
 * @property changeDetectionThreshold 变更检测阈值
 * @property retrievalOptions 检索选项
 * @property contextOptions 上下文构建选项
 */
data class RealTimeRagConfig(
    val streamingEnabled: Boolean = true,
    val updateInterval: Long = 5000, // 5 seconds
    val maxBatchSize: Int = 100,
    val maxLatency: Long = 1000, // 1 second
    val useAsyncEmbedding: Boolean = true,
    val useIncrementalIndexing: Boolean = true,
    val useChangeDetection: Boolean = true,
    val changeDetectionThreshold: Double = 0.1,
    val retrievalOptions: RealTimeRetrievalOptions = RealTimeRetrievalOptions(),
    val contextOptions: ContextBuilderConfig = ContextBuilderConfig()
)

/**
 * 实时检索选项
 *
 * @property useHybridSearch 是否使用混合搜索
 * @property useSemanticRetrieval 是否使用语义检索
 * @property useReranking 是否使用重排序
 * @property useQueryEnhancement 是否使用查询增强
 * @property hybridOptions 混合搜索选项
 * @property semanticOptions 语义检索选项
 * @property rerankingOptions 重排序选项
 * @property queryEnhancementOptions 查询增强选项
 */
data class RealTimeRetrievalOptions(
    val useHybridSearch: Boolean = false,
    val useSemanticRetrieval: Boolean = false,
    val useReranking: Boolean = true,
    val useQueryEnhancement: Boolean = true,
    val hybridOptions: HybridOptions = HybridOptions(),
    val semanticOptions: SemanticOptions = SemanticOptions(),
    val rerankingOptions: RerankingOptions = RerankingOptions(),
    val queryEnhancementOptions: QueryEnhancementOptions = QueryEnhancementOptions()
)

/**
 * 混合搜索选项
 *
 * @property vectorWeight 向量权重
 * @property keywordWeight 关键词权重
 */
data class HybridOptions(
    val vectorWeight: Double = 0.7,
    val keywordWeight: Double = 0.3
)

/**
 * 语义检索选项
 *
 * @property useChunking 是否使用分块
 * @property chunkSize 分块大小
 * @property chunkOverlap 分块重叠大小
 */
data class SemanticOptions(
    val useChunking: Boolean = true,
    val chunkSize: Int = 1000,
    val chunkOverlap: Int = 200
)

/**
 * 重排序选项
 *
 * @property useDiversity 是否使用多样性重排序
 * @property diversityWeight 多样性权重
 * @property useMetadata 是否使用元数据重排序
 * @property metadataFields 元数据字段
 * @property metadataWeights 元数据权重
 */
data class RerankingOptions(
    val useDiversity: Boolean = false,
    val diversityWeight: Double = 0.3,
    val useMetadata: Boolean = false,
    val metadataFields: List<String> = emptyList(),
    val metadataWeights: Map<String, Double> = emptyMap()
)

/**
 * 查询增强选项
 *
 * @property useSynonyms 是否使用同义词
 * @property useDecomposition 是否使用分解
 * @property useNormalization 是否使用归一化
 */
data class QueryEnhancementOptions(
    val useSynonyms: Boolean = true,
    val useDecomposition: Boolean = false,
    val useNormalization: Boolean = true
)
