package ai.kastrax.store.hybrid

import ai.kastrax.store.document.Document
import ai.kastrax.store.document.DocumentSearchResult
import ai.kastrax.store.document.DocumentVectorStore
import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.VectorStore
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * 混合搜索选项。
 *
 * @property vectorWeight 向量搜索权重
 * @property keywordWeight 关键词搜索权重
 * @property minVectorScore 最小向量分数
 * @property minKeywordScore 最小关键词分数
 * @property useReranking 是否使用重排序
 */
data class HybridSearchOptions(
    val vectorWeight: Double = 0.7,
    val keywordWeight: Double = 0.3,
    val minVectorScore: Double = 0.0,
    val minKeywordScore: Double = 0.0,
    val useReranking: Boolean = true
)

/**
 * 混合搜索结果。
 *
 * @property document 文档
 * @property score 混合分数
 * @property vectorScore 向量分数
 * @property keywordScore 关键词分数
 */
data class HybridSearchResult(
    val document: Document,
    val score: Double,
    val vectorScore: Double,
    val keywordScore: Double
)

/**
 * 混合搜索工具类。
 */
object HybridSearch {

    /**
     * 执行混合搜索。
     *
     * @param query 查询文本
     * @param vectorStore 向量存储
     * @param embeddingService 嵌入服务
     * @param keywords 关键词列表
     * @param limit 返回结果的最大数量
     * @param options 混合搜索选项
     * @return 混合搜索结果列表
     */
    suspend fun search(
        query: String,
        vectorStore: VectorStore,
        embeddingService: EmbeddingService,
        keywords: List<String>,
        limit: Int = 5,
        options: HybridSearchOptions = HybridSearchOptions()
    ): List<HybridSearchResult> {
        // 创建文档向量存储适配器
        val documentVectorStore = ai.kastrax.store.VectorStoreFactory.adaptToDocumentVectorStore(vectorStore)
        return search(query, documentVectorStore, embeddingService, keywords, limit, options)
    }

    /**
     * 执行混合搜索。
     *
     * @param query 查询文本
     * @param vectorStore RAG 向量存储
     * @param embeddingService 嵌入服务
     * @param keywords 关键词列表
     * @param limit 返回结果的最大数量
     * @param options 混合搜索选项
     * @return 混合搜索结果列表
     */
    suspend fun search(
        query: String,
        vectorStore: DocumentVectorStore,
        embeddingService: EmbeddingService,
        keywords: List<String>,
        limit: Int = 5,
        options: HybridSearchOptions = HybridSearchOptions()
    ): List<HybridSearchResult> {
        // 执行向量搜索
        val vectorResults = vectorStore.similaritySearch(
            query = query,
            embeddingService = embeddingService,
            limit = limit * 2 // 获取更多结果以便后续合并
        )

        // 如果关键词为空或权重为 0，则直接返回向量搜索结果
        if (keywords.isEmpty() || options.keywordWeight <= 0.0) {
            return vectorResults.map { result ->
                HybridSearchResult(
                    document = result.document,
                    score = result.score,
                    vectorScore = result.score,
                    keywordScore = 0.0
                )
            }.take(limit)
        }

        // 执行关键词搜索
        val keywordResults = vectorStore.keywordSearch(
            keywords = keywords,
            limit = limit * 2 // 获取更多结果以便后续合并
        ).filter { it.score >= options.minKeywordScore }

        // 合并结果
        return mergeResults(
            vectorResults = vectorResults,
            keywordResults = keywordResults,
            vectorWeight = options.vectorWeight,
            keywordWeight = options.keywordWeight,
            useReranking = options.useReranking,
            limit = limit
        )
    }

    /**
     * 合并向量搜索和关键词搜索结果。
     *
     * @param vectorResults 向量搜索结果
     * @param keywordResults 关键词搜索结果
     * @param vectorWeight 向量搜索权重
     * @param keywordWeight 关键词搜索权重
     * @param useReranking 是否使用重排序
     * @param limit 返回结果的最大数量
     * @return 混合搜索结果列表
     */
    private fun mergeResults(
        vectorResults: List<DocumentSearchResult>,
        keywordResults: List<DocumentSearchResult>,
        vectorWeight: Double,
        keywordWeight: Double,
        useReranking: Boolean,
        limit: Int
    ): List<HybridSearchResult> {
        // 创建文档 ID 到分数的映射
        val vectorScores = vectorResults.associate { it.document.id to it.score }
        val keywordScores = keywordResults.associate { it.document.id to it.score }

        // 获取所有唯一文档
        val allDocuments = (vectorResults.map { it.document } + keywordResults.map { it.document })
            .distinctBy { it.id }

        // 计算混合分数
        val hybridResults = allDocuments.map { document ->
            val vectorScore = vectorScores[document.id] ?: 0.0
            val keywordScore = keywordScores[document.id] ?: 0.0

            // 计算混合分数
            val score = if (useReranking) {
                // 使用重排序：如果文档在两个结果集中都出现，则提高其分数
                val combinedScore = vectorWeight * vectorScore + keywordWeight * keywordScore
                val boostFactor = if (vectorScore > 0.0 && keywordScore > 0.0) 1.2 else 1.0
                min(1.0, combinedScore * boostFactor)
            } else {
                // 简单加权平均
                vectorWeight * vectorScore + keywordWeight * keywordScore
            }

            HybridSearchResult(
                document = document,
                score = score,
                vectorScore = vectorScore,
                keywordScore = keywordScore
            )
        }

        // 按分数降序排序并限制结果数量
        return hybridResults
            .sortedByDescending { it.score }
            .take(limit)
    }

    /**
     * 从查询文本中提取关键词。
     *
     * @param query 查询文本
     * @param minLength 最小关键词长度
     * @param maxKeywords 最大关键词数量
     * @return 关键词列表
     */
    fun extractKeywords(
        query: String,
        minLength: Int = 3,
        maxKeywords: Int = 5
    ): List<String> {
        // 停用词列表
        val stopWords = setOf(
            "a", "an", "the", "and", "or", "but", "if", "then", "else", "when",
            "at", "from", "by", "on", "off", "for", "in", "out", "over", "to",
            "into", "with", "about", "against", "between", "through", "during",
            "before", "after", "above", "below", "up", "down", "of", "is", "are",
            "was", "were", "be", "been", "being", "have", "has", "had", "having",
            "do", "does", "did", "doing", "can", "could", "should", "would", "may",
            "might", "must", "shall", "will", "that", "these", "those", "this", "what",
            "which", "who", "whom", "whose", "why", "how"
        )

        // 分词并过滤
        return query.lowercase()
            .replace("""[^\w\s]""".toRegex(), " ") // 移除标点符号
            .split("""\s+""".toRegex()) // 按空白字符分割
            .filter { it.length >= minLength && it !in stopWords } // 过滤短词和停用词
            .take(maxKeywords) // 限制关键词数量
    }
}
