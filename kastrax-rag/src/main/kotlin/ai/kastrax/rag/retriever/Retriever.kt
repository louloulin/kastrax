package ai.kastrax.rag.retriever

import ai.kastrax.rag.document.RagDocument

/**
 * 检索器接口。
 * 用于从数据源中检索文档。
 */
interface Retriever {

    /**
     * 检索文档。
     *
     * @param query 查询
     * @param limit 返回结果数量
     * @return 文档列表
     */
    suspend fun retrieve(query: String, limit: Int = 5): List<RagDocument>
}
