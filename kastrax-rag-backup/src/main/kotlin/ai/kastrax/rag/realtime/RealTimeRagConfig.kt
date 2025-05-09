package ai.kastrax.rag.realtime

import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.reranker.ContextAwareRerankerConfig
import ai.kastrax.rag.retrieval.EnhancedHybridRetrieverConfig
import ai.kastrax.rag.retrieval.HybridRetrieverConfig
import ai.kastrax.rag.retrieval.QueryEnhancedRetrieverConfig
import ai.kastrax.rag.retrieval.SemanticRetrieverConfig
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

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
@Serializable
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
    @Contextual val contextOptions: ContextBuilderConfig = ContextBuilderConfig()
)

/**
 * 实时检索选项
 *
 * @property useHybridSearch 是否使用混合搜索
 * @property useEnhancedHybridSearch 是否使用增强混合搜索
 * @property useSemanticRetrieval 是否使用语义检索
 * @property useReranking 是否使用重排序
 * @property useContextAwareReranking 是否使用上下文感知重排序
 * @property useQueryEnhancement 是否使用查询增强
 * @property hybridOptions 混合搜索选项
 * @property enhancedHybridOptions 增强混合搜索选项
 * @property semanticOptions 语义检索选项
 * @property contextAwareRerankingOptions 上下文感知重排序选项
 * @property queryEnhancementOptions 查询增强选项
 */
@Serializable
data class RealTimeRetrievalOptions(
    val useHybridSearch: Boolean = false,
    val useEnhancedHybridSearch: Boolean = true,
    val useSemanticRetrieval: Boolean = false,
    val useReranking: Boolean = true,
    val useContextAwareReranking: Boolean = false,
    val useQueryEnhancement: Boolean = true,
    @Contextual val hybridOptions: HybridRetrieverConfig = HybridRetrieverConfig(),
    @Contextual val enhancedHybridOptions: EnhancedHybridRetrieverConfig = EnhancedHybridRetrieverConfig(),
    @Contextual val semanticOptions: SemanticRetrieverConfig = SemanticRetrieverConfig(),
    @Contextual val contextAwareRerankingOptions: ContextAwareRerankerConfig = ContextAwareRerankerConfig(),
    @Contextual val queryEnhancementOptions: QueryEnhancedRetrieverConfig = QueryEnhancedRetrieverConfig()
)
