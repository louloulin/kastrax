package ai.kastrax.rag.model

import ai.kastrax.store.document.DocumentSearchResult

/**
 * 检索上下文结果，包含检索结果和生成的上下文。
 *
 * @property results 检索结果列表
 * @property context 生成的上下文
 */
data class RetrieveContextResult(
    val results: List<DocumentSearchResult>,
    val context: String
)
