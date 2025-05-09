package ai.kastrax.rag.retrieval

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.vectorstore.RagDocument
import ai.kastrax.rag.vectorstore.RagVectorStore
import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 混合检索器配置。
 *
 * @property vectorWeight 向量搜索的权重，默认为 0.7
 * @property keywordWeight 关键词搜索的权重，默认为 0.3
 * @property expandLimit 扩展检索限制的因子，默认为 2.0
 * @property minKeywordMatches 最小关键词匹配数，默认为 1
 * @property maxKeywords 最大关键词数，默认为 5
 * @property keywordBoostFactor 关键词提升因子，默认为 1.2
 */
data class HybridRetrieverConfig(
    val vectorWeight: Double = 0.7,
    val keywordWeight: Double = 0.3,
    val expandLimit: Double = 2.0,
    val minKeywordMatches: Int = 1,
    val maxKeywords: Int = 5,
    val keywordBoostFactor: Double = 1.2
)

/**
 * 混合检索器，结合向量相似度搜索和关键词搜索。
 *
 * @property vectorStore 向量存储
 * @property embeddingService 嵌入服务
 * @property keywordExtractor 关键词提取器
 * @property config 混合检索器配置
 */
class HybridRetriever(
    private val vectorStore: RagVectorStore,
    private val embeddingService: EmbeddingService,
    private val keywordExtractor: KeywordExtractor,
    private val config: HybridRetrieverConfig = HybridRetrieverConfig()
) : Retriever {

    init {
        require(config.vectorWeight >= 0) { "Vector weight must be non-negative" }
        require(config.keywordWeight >= 0) { "Keyword weight must be non-negative" }
        require(config.vectorWeight + config.keywordWeight > 0) { "At least one weight must be positive" }
        require(config.expandLimit >= 1.0) { "Expand limit must be at least 1.0" }
    }

    /**
     * 使用查询文本检索文档。
     *
     * @param query 查询文本
     * @param limit 返回结果的最大数量
     * @param minScore 最小相似度分数
     * @return 文档列表，按组合分数降序排序
     */
    override suspend fun retrieve(query: String, limit: Int, minScore: Double): List<RagDocument> {
        logger.debug { "Retrieving documents for query: $query, limit: $limit, minScore: $minScore" }

        try {
            // 计算扩展的检索限制
            val expandedLimit = (limit * config.expandLimit).toInt().coerceAtLeast(limit)

            // 提取查询中的关键词
            val keywords = keywordExtractor.extractKeywords(query)
            logger.debug { "Extracted keywords: $keywords" }

            // 使用向量相似度搜索
            val vectorResults = vectorStore.similaritySearch(query, embeddingService, expandedLimit, minScore)

            // 如果没有关键词或关键词权重为 0，直接返回向量搜索结果
            if (keywords.isEmpty() || config.keywordWeight <= 0) {
                return vectorResults.take(limit).map { it.document }
            }

            // 使用关键词搜索
            val keywordResults = vectorStore.keywordSearch(keywords, expandedLimit)

            // 合并结果
            val combinedResults = combineResults(vectorResults, keywordResults, limit)

            return combinedResults
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving documents for query: $query" }
            return emptyList<RagDocument>()
        }
    }

    /**
     * 合并向量搜索结果和关键词搜索结果。
     *
     * @param vectorResults 向量搜索结果
     * @param keywordResults 关键词搜索结果
     * @param limit 返回结果的最大数量
     * @return 合并后的搜索结果
     */
    private fun combineResults(
        vectorResults: List<SearchResult>,
        keywordResults: List<SearchResult>,
        limit: Int
    ): List<SearchResult> {
        // 创建文档 ID 到结果的映射
        val resultMap = mutableMapOf<String, CombinedResult>()

        // 处理向量搜索结果
        for (result in vectorResults) {
            val docId = result.document.id
            resultMap[docId] = CombinedResult(
                result = result,
                vectorScore = result.score,
                keywordScore = 0.0
            )
        }

        // 处理关键词搜索结果
        for (result in keywordResults) {
            val docId = result.document.id
            val existing = resultMap[docId]

            if (existing != null) {
                // 更新现有结果的关键词分数
                resultMap[docId] = existing.copy(keywordScore = result.score)
            } else {
                // 添加新结果
                resultMap[docId] = CombinedResult(
                    result = result,
                    vectorScore = 0.0,
                    keywordScore = result.score
                )
            }
        }

        // 计算组合分数并排序
        val combinedResults = resultMap.values.map { combined ->
            val combinedScore = (combined.vectorScore * config.vectorWeight +
                combined.keywordScore * config.keywordWeight) /
                (config.vectorWeight + config.keywordWeight)

            SearchResult(combined.result.document, combinedScore)
        }

        // 按组合分数降序排序并限制结果数量
        return combinedResults.sortedByDescending { it.score }.take(limit).map { it.document }
    }

    /**
     * 组合结果，包含向量分数和关键词分数。
     *
     * @property result 原始搜索结果
     * @property vectorScore 向量相似度分数
     * @property keywordScore 关键词匹配分数
     */
    private data class CombinedResult(
        val result: SearchResult,
        val vectorScore: Double,
        val keywordScore: Double
    )
}

/**
 * 关键词提取器接口，用于从文本中提取关键词。
 */
interface KeywordExtractor {
    /**
     * 从文本中提取关键词。
     *
     * @param text 输入文本
     * @param maxKeywords 最大关键词数量，默认为 5
     * @return 关键词列表
     */
    fun extractKeywords(text: String, maxKeywords: Int = 5): List<String>
}

/**
 * 基于 TF-IDF 的关键词提取器。
 */
class TfIdfKeywordExtractor : KeywordExtractor {
    // 停用词列表
    private val stopWords = setOf(
        "a", "an", "the", "and", "or", "but", "if", "then", "else", "when",
        "at", "from", "by", "on", "off", "for", "in", "out", "over", "to",
        "into", "with", "about", "against", "between", "through", "during",
        "before", "after", "above", "below", "up", "down", "of", "is", "are",
        "am", "was", "were", "be", "been", "being", "have", "has", "had",
        "having", "do", "does", "did", "doing", "can", "could", "will",
        "would", "shall", "should", "may", "might", "must", "that", "which",
        "who", "whom", "this", "these", "those", "what", "how", "why", "when",
        "where", "there", "here", "as", "so", "than", "too", "very"
    )

    override fun extractKeywords(text: String, maxKeywords: Int): List<String> {
        // 分词并转换为小写
        val words = text.split(Regex("\\s+|[,.;:!?()\\[\\]{}'\"]"))
            .map { it.lowercase().trim() }
            .filter { it.isNotEmpty() && it !in stopWords && it.length > 1 }

        if (words.isEmpty()) {
            return emptyList()
        }

        // 计算词频
        val wordFrequency = words.groupingBy { it }.eachCount()

        // 简单的 TF-IDF 计算（这里只考虑 TF）
        val keywords = wordFrequency.entries
            .sortedByDescending { it.value }
            .take(maxKeywords)
            .map { it.key }

        return keywords
    }
}
