package ai.kastrax.rag.model

import ai.kastrax.store.document.Document

/**
 * 检索上下文结果，包含生成的上下文和检索到的文档。
 *
 * @property context 生成的上下文
 * @property documents 检索到的文档列表
 */
data class RetrieveContextResult(
    val context: String,
    val documents: List<Document>
)
