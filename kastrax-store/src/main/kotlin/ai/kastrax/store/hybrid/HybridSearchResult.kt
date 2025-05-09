package ai.kastrax.store.hybrid

import ai.kastrax.store.document.Document

/**
 * 混合搜索结果。
 *
 * @property document 文档
 * @property score 混合分数
 * @property vectorScore 向量分数
 * @property keywordScore 关键词分数
 */
data class HybridSearchResult(
    val document: Document,
    val score: Double,
    val vectorScore: Double,
    val keywordScore: Double
)
