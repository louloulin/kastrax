package ai.kastrax.ragx.retrieval

import ai.kastrax.store.VectorStore
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * TopK 检索器，基于向量相似度检索文档。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property indexName 索引名称
 */
class TopKRetriever(
    private val vectorStore: VectorStore,
    private val embeddingService: EmbeddingService,
    private val indexName: String = "default"
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
            // 计算查询嵌入向量
            val embedding = embeddingService.embed(query)
            
            // 执行向量查询
            val results = vectorStore.query(
                indexName = indexName,
                queryVector = embedding,
                topK = limit,
                filter = null
            ).filter { it.score >= minScore }
            
            // 转换为文档搜索结果
            return@withContext results.map { result ->
                val content = result.metadata?.get("content") as? String ?: ""
                val metadata = result.metadata?.filterKeys { it != "content" } ?: emptyMap()
                val document = Document(result.id, content, metadata)
                
                DocumentSearchResult(document, result.score)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving documents" }
            throw e
        }
    }
}
