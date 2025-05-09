package ai.kastrax.store.document

/**
 * 文档搜索结果类，表示一个文档搜索的结果。
 *
 * @property document 文档
 * @property score 相似度分数
 */
data class DocumentSearchResult(
    val document: Document,
    val score: Double
)
