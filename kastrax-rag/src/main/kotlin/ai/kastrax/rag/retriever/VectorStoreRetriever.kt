package ai.kastrax.rag.retriever

import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * 向量存储检索器，基于向量相似度检索文档。
 *
 * @property documentStore 文档向量存储
 * @property embeddingService 嵌入服务
 */
class VectorStoreRetriever(
    private val documentStore: DocumentVectorStore,
    private val embeddingService: EmbeddingService
) : Retriever {
    /**
     * 检索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 检索结果列表
     */
    override suspend fun retrieve(
        query: String,
        limit: Int,
        minScore: Double
    ): List<DocumentSearchResult> = withContext(Dispatchers.IO) {
        try {
            // 使用文档向量存储进行相似度搜索
            val results = documentStore.similaritySearch(query, embeddingService, limit)
            
            // 过滤低分结果
            return@withContext results.filter { it.score >= minScore }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving documents" }
            return@withContext emptyList()
        }
    }
}
