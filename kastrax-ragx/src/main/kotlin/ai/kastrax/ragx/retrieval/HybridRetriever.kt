package ai.kastrax.ragx.retrieval

import ai.kastrax.store.VectorStore
import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.hybrid.HybridSearch
import ai.kastrax.store.hybrid.HybridSearchOptions
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * 混合检索器，结合向量相似度和关键词匹配检索文档。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property indexName 索引名称
 * @property vectorWeight 向量权重
 * @property keywordWeight 关键词权重
 */
class HybridRetriever(
    private val vectorStore: VectorStore,
    private val embeddingService: EmbeddingService,
    private val indexName: String = "default",
    private val vectorWeight: Double = 0.7,
    private val keywordWeight: Double = 0.3
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
            // 提取关键词
            val keywords = extractKeywords(query)
            
            // 执行混合搜索
            val options = HybridSearchOptions(
                vectorWeight = vectorWeight,
                keywordWeight = keywordWeight,
                useReranking = true
            )
            
            // 使用 HybridSearch 执行搜索
            val results = HybridSearch.search(
                query = query,
                vectorStore = vectorStore,
                embeddingService = embeddingService,
                keywords = keywords,
                limit = limit,
                options = options,
                indexName = indexName
            )
            
            // 过滤低分结果
            val filteredResults = results.filter { it.score >= minScore }
            
            // 转换为文档搜索结果
            return@withContext filteredResults.map { result ->
                DocumentSearchResult(result.document, result.score)
            }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving documents with hybrid search" }
            throw e
        }
    }
    
    /**
     * 提取关键词。
     *
     * @param query 查询文本
     * @return 关键词列表
     */
    private fun extractKeywords(query: String): List<String> {
        return HybridSearch.extractKeywords(query)
    }
}
