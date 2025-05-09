package ai.kastrax.rag.model

import ai.kastrax.rag.document.RagDocument

/**
 * 搜索结果模型。
 *
 * @property id 文档 ID
 * @property content 文档内容
 * @property score 相似度分数
 * @property metadata 文档元数据
 */
data class SearchResult(
    val id: String,
    val content: String,
    val score: Double,
    val metadata: Map<String, Any> = emptyMap()
) {
    /**
     * 从 RagDocument 创建 SearchResult。
     *
     * @param document RAG 文档
     * @param score 相似度分数
     */
    constructor(document: RagDocument, score: Double) : this(
        id = document.id,
        content = document.content,
        score = score,
        metadata = document.metadata
    )

    /**
     * 获取文档。
     *
     * @return RAG 文档
     */
    val document: RagDocument
        get() = RagDocument(
            id = id,
            content = content,
            metadata = metadata,
            embedding = null
        )
}
