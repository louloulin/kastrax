package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.memory.api.SemanticSearchResult

/**
 * 相关性重排序器接口，用于重新排序搜索结果。
 */
interface RelevanceReranker {
    /**
     * 重新排序搜索结果。
     *
     * @param query 搜索查询
     * @param results 原始搜索结果
     * @param limit 返回的最大结果数量
     * @return 重新排序后的结果
     */
    suspend fun rerank(
        query: String,
        results: List<SemanticSearchResult>,
        limit: Int = results.size
    ): List<SemanticSearchResult>
}

/**
 * 多样性重排序器，确保结果的多样性。
 *
 * @property diversityWeight 多样性权重（0.0-1.0），默认为0.3
 * @property similarityThreshold 相似度阈值，默认为0.8
 */
class DiversityReranker(
    private val diversityWeight: Float = 0.3f,
    @Suppress("unused") private val similarityThreshold: Float = 0.8f // 目前未使用，但保留以备将来需要
) : RelevanceReranker, KastraXBase(component = "RERANKER", name = "diversity") {

    override suspend fun rerank(
        query: String,
        results: List<SemanticSearchResult>,
        limit: Int
    ): List<SemanticSearchResult> {
        if (results.size <= 1 || limit >= results.size) {
            return results.take(limit)
        }

        // 初始化已选择的结果和候选结果
        val selected = mutableListOf<SemanticSearchResult>()
        val candidates = results.toMutableList()

        // 选择第一个结果（分数最高的）
        selected.add(candidates.removeAt(0))

        // 选择剩余结果
        while (selected.size < limit && candidates.isNotEmpty()) {
            // 计算每个候选结果与已选择结果的最大相似度
            val candidateScores = candidates.map { candidate ->
                val content = candidate.message.message.content

                // 计算与已选择结果的最大相似度
                val maxSimilarity = selected.maxOfOrNull { selected ->
                    val selectedContent = selected.message.message.content
                    calculateTextSimilarity(content, selectedContent)
                } ?: 0f

                // 计算多样性分数（相似度越低，多样性越高）
                val diversityScore = 1f - maxSimilarity

                // 计算最终分数（原始分数 + 多样性分数 * 权重）
                val finalScore = candidate.score * (1f - diversityWeight) + diversityScore * diversityWeight

                Pair(candidate, finalScore)
            }

            // 选择分数最高的候选结果
            val (bestCandidate, _) = candidateScores.maxByOrNull { it.second }
                ?: break

            // 从候选列表中移除并添加到已选择列表
            candidates.remove(bestCandidate)
            selected.add(bestCandidate)
        }

        return selected
    }

    /**
     * 计算两段文本的相似度。
     */
    private fun calculateTextSimilarity(text1: String, text2: String): Float {
        // 简单的Jaccard相似度计算
        val words1 = text1.lowercase().split(Regex("\\s+")).toSet()
        val words2 = text2.lowercase().split(Regex("\\s+")).toSet()

        val intersection = words1.intersect(words2).size
        val union = words1.union(words2).size

        return if (union == 0) 0f else intersection.toFloat() / union
    }
}

/**
 * 上下文感知重排序器，考虑消息的上下文关系。
 *
 * @property contextWeight 上下文权重（0.0-1.0），默认为0.2
 */
class ContextAwareReranker(
    private val contextWeight: Float = 0.2f
) : RelevanceReranker, KastraXBase(component = "RERANKER", name = "context-aware") {

    override suspend fun rerank(
        query: String,
        results: List<SemanticSearchResult>,
        limit: Int
    ): List<SemanticSearchResult> {
        if (results.size <= 1 || limit >= results.size) {
            return results.take(limit)
        }

        // 按时间排序
        val sortedResults = results.sortedBy { it.message.createdAt }

        // 计算上下文分数
        val contextScores = sortedResults.mapIndexed { index, result ->
            // 计算与前后消息的连贯性
            var contextScore = 0f

            // 检查前一条消息
            if (index > 0) {
                val prevMessage = sortedResults[index - 1]
                if (isContextual(prevMessage.message.message.content, result.message.message.content)) {
                    contextScore += 0.5f
                }
            }

            // 检查后一条消息
            if (index < sortedResults.size - 1) {
                val nextMessage = sortedResults[index + 1]
                if (isContextual(result.message.message.content, nextMessage.message.message.content)) {
                    contextScore += 0.5f
                }
            }

            // 计算最终分数（原始分数 + 上下文分数 * 权重）
            val finalScore = result.score * (1f - contextWeight) + contextScore * contextWeight

            SemanticSearchResult(result.message, finalScore)
        }

        // 按最终分数排序
        return contextScores.sortedByDescending { it.score }.take(limit)
    }

    /**
     * 判断两条消息是否具有上下文关系。
     */
    private fun isContextual(text1: String, text2: String): Boolean {
        // 检查代词引用
        val pronouns = listOf("it", "this", "that", "these", "those", "he", "she", "they", "I", "you", "we")

        for (pronoun in pronouns) {
            if (text2.lowercase().contains("\\b$pronoun\\b".toRegex())) {
                return true
            }
        }

        // 检查共同主题词
        val words1 = text1.lowercase().split(Regex("\\s+"))
            .filter { it.length > 3 }
            .toSet()
        val words2 = text2.lowercase().split(Regex("\\s+"))
            .filter { it.length > 3 }
            .toSet()

        val commonWords = words1.intersect(words2)

        return commonWords.size >= 2
    }
}

/**
 * 组合重排序器，结合多个重排序器。
 *
 * @property rerankers 重排序器列表
 */
class CompositeReranker(
    private val rerankers: List<RelevanceReranker>
) : RelevanceReranker, KastraXBase(component = "RERANKER", name = "composite") {

    override suspend fun rerank(
        query: String,
        results: List<SemanticSearchResult>,
        limit: Int
    ): List<SemanticSearchResult> {
        var currentResults = results

        // 依次应用每个重排序器
        for (reranker in rerankers) {
            currentResults = reranker.rerank(query, currentResults, limit)
        }

        return currentResults
    }
}
