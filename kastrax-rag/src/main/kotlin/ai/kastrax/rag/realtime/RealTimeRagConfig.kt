package ai.kastrax.rag.realtime

import ai.kastrax.rag.HybridOptions
import ai.kastrax.rag.QueryEnhancementOptions
import ai.kastrax.rag.RerankingOptions
import ai.kastrax.rag.SemanticOptions
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
 * @property maxDocuments 最大文档数量
 * @property maxDocumentAge 最大文档年龄（毫秒）
 * @property useTimeDecay 是否使用时间衰减
 * @property timeDecayFactor 时间衰减因子
 * @property characterLevelStreaming 是否启用字符级流式处理
 * @property streamingDelay 流式处理延迟（毫秒）
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
    val maxDocuments: Int = 1000,
    val maxDocumentAge: Long = 24 * 60 * 60 * 1000, // 24 小时
    val useTimeDecay: Boolean = true,
    val timeDecayFactor: Double = 0.5,
    val characterLevelStreaming: Boolean = false,
    val streamingDelay: Long = 10, // 10 毫秒，字符级流式处理的延迟
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
    val useQueryEnhancement: Boolean = false,
    val hybridOptions: HybridOptions = HybridOptions(),
    val semanticOptions: SemanticOptions = SemanticOptions(),
    val rerankingOptions: RerankingOptions = RerankingOptions(),
    val queryEnhancementOptions: QueryEnhancementOptions = QueryEnhancementOptions()
)
