package ai.kastrax.rag

import ai.kastrax.store.document.Document

/**
 * RAG检索结果，包含检索到的文档、查询和元数据。
 *
 * @property documents 检索到的文档列表
 * @property query 原始查询
 * @property metadata 元数据
 */
data class RagResult(
    val documents: List<Document> = emptyList(),
    val query: String = "",
    val metadata: Map<String, Any> = emptyMap()
)