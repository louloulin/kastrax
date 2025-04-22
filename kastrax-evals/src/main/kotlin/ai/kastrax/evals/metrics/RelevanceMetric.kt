package ai.kastrax.evals.metrics

import ai.kastrax.evals.embedding.EmbeddingService
import ai.kastrax.evals.util.cosineSimilarity
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlin.math.max
import kotlin.math.min

private val logger = KotlinLogging.logger {}

/**
 * 相关性指标，用于评估输出与输入的相关程度。
 *
 * 支持多种相关性评估方法：
 * - 关键词匹配：检查输出是否包含输入中的关键词
 * - 语义相似度：计算输出与输入的语义相似度
 * - 混合评估：结合关键词匹配和语义相似度
 *
 * @param method 相关性评估方法
 * @param embeddingService 嵌入服务，用于计算语义相似度，仅在使用语义相似度或混合评估时需要
 * @param keywordWeight 关键词匹配的权重，仅在使用混合评估时有效
 * @param semanticWeight 语义相似度的权重，仅在使用混合评估时有效
 */
class RelevanceMetric(
    private val method: RelevanceMethod = RelevanceMethod.KEYWORD_MATCH,
    private val embeddingService: EmbeddingService? = null,
    private val keywordWeight: Double = 0.5,
    private val semanticWeight: Double = 0.5
) : Metric<String, String> {

    override val name: String = "Relevance"
    override val description: String = "Evaluates the relevance of the output to the input"
    override val category: MetricCategory = MetricCategory.RELEVANCE

    init {
        if (method == RelevanceMethod.SEMANTIC_SIMILARITY || method == RelevanceMethod.HYBRID) {
            requireNotNull(embeddingService) { "Embedding service must be provided for semantic similarity or hybrid evaluation" }
        }
    }

    override suspend fun calculate(
        input: String,
        output: String,
        options: Map<String, Any?>
    ): MetricResult = coroutineScope {
        // 根据方法计算相关性
        val score = when (method) {
            RelevanceMethod.KEYWORD_MATCH -> calculateKeywordMatch(input, output, options)
            RelevanceMethod.SEMANTIC_SIMILARITY -> {
                val semanticSimilarityDeferred = async { calculateSemanticSimilarity(input, output) }
                semanticSimilarityDeferred.await()
            }
            RelevanceMethod.HYBRID -> {
                val keywordMatchDeferred = async { calculateKeywordMatch(input, output, options) }
                val semanticSimilarityDeferred = async { calculateSemanticSimilarity(input, output) }

                val keywordMatchScore = keywordMatchDeferred.await()
                val semanticSimilarityScore = semanticSimilarityDeferred.await()

                // 计算加权平均分数
                keywordMatchScore * keywordWeight + semanticSimilarityScore * semanticWeight
            }
        }

        MetricResult(
            score = score,
            details = mapOf(
                "method" to method.name,
                "keywordWeight" to keywordWeight,
                "semanticWeight" to semanticWeight,
                "input" to input,
                "output" to output
            )
        )
    }

    /**
     * 计算关键词匹配的相关性。
     *
     * @param input 输入文本
     * @param output 输出文本
     * @param options 计算选项
     * @return 相关性分数，范围为 0.0 到 1.0
     */
    private fun calculateKeywordMatch(
        input: String,
        output: String,
        options: Map<String, Any?>
    ): Double {
        // 从选项中获取关键词，如果没有提供，则从输入中提取
        val keywords = options["keywords"] as? List<String> ?: extractKeywords(input)

        // 如果没有关键词，则返回 0.0
        if (keywords.isEmpty()) {
            return 0.0
        }

        // 计算包含的关键词数量
        val matchedKeywords = keywords.count { keyword ->
            output.contains(keyword, ignoreCase = true)
        }

        // 计算关键词匹配分数
        return matchedKeywords.toDouble() / keywords.size
    }

    /**
     * 计算语义相似度的相关性。
     *
     * @param input 输入文本
     * @param output 输出文本
     * @return 相关性分数，范围为 0.0 到 1.0
     */
    private suspend fun calculateSemanticSimilarity(
        input: String,
        output: String
    ): Double {
        // 如果输入或输出为空，则返回 0.0
        if (input.isBlank() || output.isBlank()) {
            return 0.0
        }

        try {
            // 计算输入和输出的嵌入向量
            val inputEmbedding = embeddingService!!.embed(input)
            val outputEmbedding = embeddingService.embed(output)

            // 计算余弦相似度
            return ai.kastrax.evals.util.cosineSimilarity(inputEmbedding, outputEmbedding).toDouble()
        } catch (e: Exception) {
            logger.error(e) { "Error calculating semantic similarity" }
            return 0.0
        }
    }

    /**
     * 从文本中提取关键词。
     *
     * @param text 文本
     * @return 关键词列表
     */
    private fun extractKeywords(text: String): List<String> {
        // 简单的关键词提取：移除停用词，保留长度大于 3 的单词
        val stopWords = setOf(
            "a", "an", "the", "and", "or", "but", "is", "are", "was", "were",
            "be", "been", "being", "have", "has", "had", "do", "does", "did",
            "to", "at", "by", "for", "with", "about", "against", "between",
            "into", "through", "during", "before", "after", "above", "below",
            "from", "up", "down", "in", "out", "on", "off", "over", "under",
            "again", "further", "then", "once", "here", "there", "when", "where",
            "why", "how", "all", "any", "both", "each", "few", "more", "most",
            "other", "some", "such", "no", "nor", "not", "only", "own", "same",
            "so", "than", "too", "very", "s", "t", "can", "will", "just", "don",
            "should", "now", "d", "ll", "m", "o", "re", "ve", "y", "ain", "aren",
            "couldn", "didn", "doesn", "hadn", "hasn", "haven", "isn", "ma",
            "mightn", "mustn", "needn", "shan", "shouldn", "wasn", "weren", "won",
            "wouldn", "的", "了", "和", "是", "在", "我", "有", "你", "他", "她",
            "它", "们", "这", "那", "什么", "怎么", "为什么", "如何", "什么时候",
            "哪里", "谁", "哪个", "哪些", "多少", "几", "如果", "因为", "所以",
            "但是", "而且", "或者", "不", "很", "非常", "太", "越", "更", "最"
        )

        return text.split(Regex("\\s+|\\p{Punct}"))
            .filter { it.length > 3 && it.lowercase() !in stopWords }
            .distinct()
    }
}

/**
 * 相关性评估方法。
 */
enum class RelevanceMethod {
    /**
     * 关键词匹配，检查输出是否包含输入中的关键词。
     */
    KEYWORD_MATCH,

    /**
     * 语义相似度，计算输出与输入的语义相似度。
     */
    SEMANTIC_SIMILARITY,

    /**
     * 混合评估，结合关键词匹配和语义相似度。
     */
    HYBRID
}

/**
 * 创建相关性指标。
 *
 * @param method 相关性评估方法
 * @param embeddingService 嵌入服务，用于计算语义相似度，仅在使用语义相似度或混合评估时需要
 * @param keywordWeight 关键词匹配的权重，仅在使用混合评估时有效
 * @param semanticWeight 语义相似度的权重，仅在使用混合评估时有效
 * @return 相关性指标
 */
fun relevanceMetric(
    method: RelevanceMethod = RelevanceMethod.KEYWORD_MATCH,
    embeddingService: EmbeddingService? = null,
    keywordWeight: Double = 0.5,
    semanticWeight: Double = 0.5
): RelevanceMetric {
    return RelevanceMetric(method, embeddingService, keywordWeight, semanticWeight)
}
