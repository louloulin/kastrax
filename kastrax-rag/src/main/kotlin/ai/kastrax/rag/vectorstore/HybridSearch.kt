package ai.kastrax.rag.vectorstore

import ai.kastrax.rag.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * 混合搜索过滤器，用于根据元数据过滤搜索结果。
 */
typealias MetadataFilter = (Map<String, Any>) -> Boolean

/**
 * 混合搜索结果，包含向量相似度和关键词匹配信息。
 *
 * @property document 文档
 * @property vectorScore 向量相似度分数
 * @property keywordScore 关键词匹配分数
 * @property combinedScore 综合分数
 */
data class HybridSearchResult(
    val document: RagDocument,
    val vectorScore: Double,
    val keywordScore: Double,
    val combinedScore: Double
)

/**
 * 混合搜索扩展，为 VectorStore 添加混合搜索功能。
 */
object HybridSearch {

    /**
     * 使用混合搜索（向量相似度 + 元数据过滤）搜索文档。
     *
     * @param vectorStore 向量存储
     * @param embedding 查询嵌入向量
     * @param filter 元数据过滤器
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 搜索结果列表，按相似度降序排序
     */
    suspend fun hybridSearch(
        vectorStore: RagVectorStore,
        query: String,
        embeddingService: EmbeddingService,
        filter: MetadataFilter? = null,
        limit: Int = 5,
        minScore: Double = 0.0
    ): List<SearchResult> {
        // 首先进行向量相似度搜索
        val vectorResults = vectorStore.similaritySearch(query, embeddingService, limit * 2, minScore)

        // 如果没有过滤器，直接返回向量搜索结果
        if (filter == null) {
            return vectorResults.take(limit)
        }

        // 应用元数据过滤器
        val filteredResults = vectorResults.filter { result ->
            try {
                filter(result.document.metadata)
            } catch (e: Exception) {
                logger.warn(e) { "Error applying metadata filter" }
                false
            }
        }

        return filteredResults.take(limit)
    }

    /**
     * 使用混合搜索（向量相似度 + 关键词匹配）搜索文档。
     *
     * @param vectorStore 向量存储
     * @param text 查询文本
     * @param embeddingService 嵌入服务
     * @param keywords 关键词列表
     * @param vectorWeight 向量相似度权重
     * @param keywordWeight 关键词匹配权重
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 混合搜索结果列表，按综合分数降序排序
     */
    suspend fun hybridKeywordSearch(
        vectorStore: RagVectorStore,
        text: String,
        embeddingService: EmbeddingService,
        keywords: List<String>,
        vectorWeight: Double = 0.7,
        keywordWeight: Double = 0.3,
        limit: Int = 5,
        minScore: Double = 0.0
    ): List<HybridSearchResult> = coroutineScope {
        // 进行向量相似度搜索
        val vectorResults = vectorStore.similaritySearch(text, embeddingService, limit * 2, 0.0)

        // 计算关键词匹配分数并组合结果
        val hybridResults = vectorResults.map { result ->
            val keywordScore = calculateKeywordScore(result.document.content, keywords)
            val combinedScore = (result.score * vectorWeight) + (keywordScore * keywordWeight)

            HybridSearchResult(
                document = result.document,
                vectorScore = result.score,
                keywordScore = keywordScore,
                combinedScore = combinedScore
            )
        }

        // 按综合分数排序并过滤低于最小分数的结果
        hybridResults
            .filter { it.combinedScore >= minScore }
            .sortedByDescending { it.combinedScore }
            .take(limit)
    }

    /**
     * 计算文本中关键词的匹配分数。
     *
     * @param text 要检查的文本
     * @param keywords 关键词列表
     * @return 关键词匹配分数，范围为 [0, 1]
     */
    private fun calculateKeywordScore(text: String, keywords: List<String>): Double {
        if (keywords.isEmpty()) {
            return 0.0
        }

        val lowerText = text.lowercase()
        var matchCount = 0

        for (keyword in keywords) {
            val lowerKeyword = keyword.lowercase()
            if (lowerText.contains(lowerKeyword)) {
                matchCount++
            }
        }

        return matchCount.toDouble() / keywords.size
    }

}

/**
 * 将混合搜索结果转换为标准搜索结果。
 *
 * @return 标准搜索结果列表
 */
fun List<HybridSearchResult>.toSearchResults(): List<SearchResult> {
    return map { SearchResult(it.document, it.combinedScore) }
}