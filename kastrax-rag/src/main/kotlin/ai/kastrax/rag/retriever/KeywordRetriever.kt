package ai.kastrax.rag.retriever

import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.document.DocumentVectorStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * 关键词检索器，基于关键词匹配检索文档。
 *
 * @property documentStore 文档向量存储
 * @property keywordExtractor 关键词提取器
 */
class KeywordRetriever(
    private val documentStore: DocumentVectorStore,
    private val keywordExtractor: KeywordExtractor = SimpleKeywordExtractor()
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
            val keywords = keywordExtractor.extract(query)
            
            if (keywords.isEmpty()) {
                logger.warn { "No keywords extracted from query: $query" }
                return@withContext emptyList()
            }
            
            // 使用文档向量存储进行关键词搜索
            val results = documentStore.keywordSearch(keywords, limit)
            
            // 过滤低分结果
            return@withContext results.filter { it.score >= minScore }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving documents with keywords" }
            return@withContext emptyList()
        }
    }
}
