package ai.kastrax.rag.model

import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult

/**
 * 检索上下文结果，包含生成的上下文和检索到的文档。
 *
 * @property context 生成的上下文
 * @property documents 检索到的文档列表
 */
data class RetrieveContextResult(
    val context: String,
    val documents: List<Document> = emptyList()
) {
    companion object {
        /**
         * 使用文档搜索结果创建检索上下文结果。
         *
         * @param context 生成的上下文
         * @param searchResults 文档搜索结果列表
         * @return 检索上下文结果
         */
        fun fromSearchResults(context: String, searchResults: List<DocumentSearchResult>): RetrieveContextResult {
            return RetrieveContextResult(
                context = context,
                documents = searchResults.map { it.document }
            )
        }
    }
}
