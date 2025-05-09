package ai.kastrax.rag.model

/**
 * 搜索结果类，表示一个文档搜索的结果。
 *
 * @property id 文档 ID
 * @property content 文档内容
 * @property score 相似度分数
 * @property metadata 元数据
 */
data class SearchResult(
    val id: String,
    val content: String,
    val score: Double,
    val metadata: Map<String, Any> = emptyMap()
)
