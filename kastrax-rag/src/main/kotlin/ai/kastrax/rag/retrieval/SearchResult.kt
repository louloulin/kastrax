package ai.kastrax.rag.retrieval

import ai.kastrax.store.document.Document

/**
 * 搜索结果类，表示一个搜索的结果。
 *
 * @property document 文档
 * @property score 相似度分数
 */
data class SearchResult(
    val document: Document,
    val score: Double
)
