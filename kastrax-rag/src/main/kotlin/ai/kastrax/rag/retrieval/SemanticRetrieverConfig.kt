package ai.kastrax.rag.retrieval

/**
 * 语义检索器配置。
 *
 * @property expandQuery 是否扩展查询，默认为 true
 * @property queryExpansionFactor 查询扩展因子，默认为 1.5
 * @property diversityThreshold 多样性阈值，默认为 0.8
 * @property useSemanticClustering 是否使用语义聚类，默认为 true
 */
data class SemanticRetrieverConfig(
    val expandQuery: Boolean = true,
    val queryExpansionFactor: Double = 1.5,
    val diversityThreshold: Double = 0.8,
    val useSemanticClustering: Boolean = true
)
