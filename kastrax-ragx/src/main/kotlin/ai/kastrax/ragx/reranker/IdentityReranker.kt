package ai.kastrax.ragx.reranker

import ai.kastrax.store.document.DocumentSearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 恒等重排序器，不改变检索结果的顺序。
 */
class IdentityReranker : Reranker {
    /**
     * 重排序文档。
     *
     * @param query 查询文本
     * @param results 检索结果列表
     * @return 重排序后的结果列表
     */
    override suspend fun rerank(
        query: String,
        results: List<DocumentSearchResult>
    ): List<DocumentSearchResult> = withContext(Dispatchers.IO) {
        // 直接返回原始结果
        return@withContext results
    }
}
