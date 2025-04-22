package ai.kastrax.rag.reranker

import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 重排序器接口，用于对搜索结果进行重排序。
 */
interface Reranker {
    /**
     * 对搜索结果进行重排序。
     *
     * @param query 查询文本
     * @param results 原始搜索结果
     * @return 重排序后的搜索结果
     */
    suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult>
}

/**
 * 简单重排序器，保持原始排序不变。
 */
class IdentityReranker : Reranker {
    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        return results
    }
}

/**
 * 基于关键词匹配的重排序器，根据查询关键词在文档中的出现次数进行重排序。
 *
 * @property keywordWeight 关键词匹配的权重，默认为 0.5
 * @property originalScoreWeight 原始分数的权重，默认为 0.5
 */
class KeywordMatchReranker(
    private val keywordWeight: Double = 0.5,
    private val originalScoreWeight: Double = 0.5
) : Reranker {

    init {
        require(keywordWeight >= 0) { "Keyword weight must be non-negative" }
        require(originalScoreWeight >= 0) { "Original score weight must be non-negative" }
        require(keywordWeight + originalScoreWeight > 0) { "At least one weight must be positive" }
    }

    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        if (results.isEmpty()) {
            return emptyList()
        }

        // 提取查询关键词
        val keywords = query.lowercase().split(Regex("\\s+"))
            .filter { it.length > 2 } // 过滤掉太短的词
            .toSet()

        if (keywords.isEmpty()) {
            return results
        }

        // 计算每个文档的关键词匹配分数
        val rerankedResults = results.map { result ->
            val content = result.document.content.lowercase()

            // 计算每个关键词的出现次数
            val keywordCounts = keywords.associateWith { keyword ->
                countOccurrences(content, keyword)
            }

            // 计算总的关键词匹配分数
            val keywordScore = keywordCounts.values.sum().toDouble() / (content.length / 100.0)

            // 计算组合分数
            val combinedScore = (keywordScore * keywordWeight + result.score * originalScoreWeight) /
                (keywordWeight + originalScoreWeight)

            SearchResult(result.document, combinedScore)
        }

        // 按组合分数降序排序
        return rerankedResults.sortedByDescending { it.score }
    }

    /**
     * 计算关键词在文本中的出现次数。
     */
    private fun countOccurrences(text: String, keyword: String): Int {
        var count = 0
        var index = 0

        while (index != -1) {
            index = text.indexOf(keyword, index)
            if (index != -1) {
                count++
                index += keyword.length
            }
        }

        return count
    }
}

/**
 * 基于元数据的重排序器，根据文档元数据进行重排序。
 *
 * @property metadataKey 用于重排序的元数据键
 * @property ascending 是否按升序排序，默认为 false（降序）
 * @property metadataWeight 元数据的权重，默认为 0.5
 * @property originalScoreWeight 原始分数的权重，默认为 0.5
 */
class MetadataReranker(
    private val metadataKey: String,
    private val ascending: Boolean = false,
    private val metadataWeight: Double = 0.5,
    private val originalScoreWeight: Double = 0.5
) : Reranker {

    init {
        require(metadataWeight >= 0) { "Metadata weight must be non-negative" }
        require(originalScoreWeight >= 0) { "Original score weight must be non-negative" }
        require(metadataWeight + originalScoreWeight > 0) { "At least one weight must be positive" }
    }

    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        if (results.isEmpty()) {
            return emptyList()
        }

        // 过滤出包含指定元数据的文档
        val filteredResults = results.filter { result ->
            result.document.metadata.containsKey(metadataKey)
        }

        if (filteredResults.isEmpty()) {
            logger.warn { "No documents contain metadata key: $metadataKey" }
            return results
        }

        // 获取元数据值
        val metadataValues = filteredResults.map { result ->
            val value = result.document.metadata[metadataKey]
            if (value == null) {
                0.0
            } else {
                value.toDoubleOrNull() ?: 0.0
            }
        }

        // 找出最大和最小值，用于归一化
        val maxValue = metadataValues.maxOrNull() ?: 1.0
        val minValue = metadataValues.minOrNull() ?: 0.0
        val range = maxValue - minValue

        // 重排序
        val rerankedResults = filteredResults.mapIndexed { index, result ->
            val metadataValue = metadataValues[index]

            // 归一化元数据值到 [0, 1] 范围
            val normalizedValue = if (range > 0) {
                (metadataValue - minValue) / range
            } else {
                0.5 // 如果所有值都相同，使用中间值
            }

            // 根据升序/降序调整分数
            // 对于降序，较大的值应该有较高的分数
            val metadataScore = if (!ascending) normalizedValue else (1.0 - normalizedValue)

            // 计算组合分数
            val combinedScore = (metadataScore * metadataWeight + result.score * originalScoreWeight) /
                (metadataWeight + originalScoreWeight)

            SearchResult(result.document, combinedScore)
        }

        // 按组合分数降序排序
        return rerankedResults.sortedByDescending { it.score }
    }
}

/**
 * 组合重排序器，按顺序应用多个重排序器。
 *
 * @property rerankers 要应用的重排序器列表
 */
class CompositeReranker(
    private val rerankers: List<Reranker>
) : Reranker {

    constructor(vararg rerankers: Reranker) : this(rerankers.toList())

    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        var currentResults = results

        for (reranker in rerankers) {
            currentResults = reranker.rerank(query, currentResults)
        }

        return currentResults
    }
}
