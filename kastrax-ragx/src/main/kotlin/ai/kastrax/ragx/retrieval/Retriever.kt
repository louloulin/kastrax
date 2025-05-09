package ai.kastrax.ragx.retrieval

import ai.kastrax.store.document.DocumentSearchResult

/**
 * 检索器接口，定义了检索文档的方法。
 */
interface Retriever {
    /**
     * 检索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 检索结果列表
     */
    suspend fun retrieve(
        query: String,
        limit: Int = 5,
        minScore: Double = 0.0
    ): List<DocumentSearchResult>
}
