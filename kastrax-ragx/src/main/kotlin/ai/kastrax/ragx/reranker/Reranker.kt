package ai.kastrax.ragx.reranker

import ai.kastrax.store.document.DocumentSearchResult

/**
 * 重排序器接口，定义了重排序文档的方法。
 */
interface Reranker {
    /**
     * 重排序文档。
     *
     * @param query 查询文本
     * @param results 检索结果列表
     * @return 重排序后的结果列表
     */
    suspend fun rerank(
        query: String,
        results: List<DocumentSearchResult>
    ): List<DocumentSearchResult>
}
