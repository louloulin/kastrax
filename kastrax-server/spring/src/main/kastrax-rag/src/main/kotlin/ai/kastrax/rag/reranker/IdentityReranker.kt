package ai.kastrax.rag.reranker

import ai.kastrax.store.document.DocumentSearchResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 恒等重排序器，不对检索结果进行重排序。
 */
class IdentityReranker : Reranker {
    /**
     * 对检索结果进行重排序。
     *
     * @param query 查询文本
     * @param results 检索结果列表
     * @return 重排序后的结果列表
     */
    override suspend fun rerank(
        query: String,
        results: List<DocumentSearchResult>
    ): List<DocumentSearchResult> {
        logger.debug { "Identity reranker: returning ${results.size} results as-is" }
        return results
    }
}
