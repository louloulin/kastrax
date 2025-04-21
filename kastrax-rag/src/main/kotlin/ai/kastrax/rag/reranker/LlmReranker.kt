package ai.kastrax.rag.reranker

import ai.kastrax.core.agent.Agent
import ai.kastrax.rag.vectorstore.SearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * 基于 LLM 的重排序器配置。
 *
 * @property batchSize 批处理大小，默认为 5
 * @property scorePromptTemplate 评分提示模板
 * @property minScore 最小分数，默认为 0.0
 * @property maxScore 最大分数，默认为 1.0
 * @property originalScoreWeight 原始分数的权重，默认为 0.3
 * @property llmScoreWeight LLM 分数的权重，默认为 0.7
 */
data class LlmRerankerConfig(
    val batchSize: Int = 5,
    val scorePromptTemplate: String = DEFAULT_SCORE_PROMPT_TEMPLATE,
    val minScore: Double = 0.0,
    val maxScore: Double = 1.0,
    val originalScoreWeight: Double = 0.3,
    val llmScoreWeight: Double = 0.7
) {
    init {
        require(batchSize > 0) { "Batch size must be positive" }
        require(minScore <= maxScore) { "Min score must be less than or equal to max score" }
        require(originalScoreWeight >= 0) { "Original score weight must be non-negative" }
        require(llmScoreWeight >= 0) { "LLM score weight must be non-negative" }
        require(originalScoreWeight + llmScoreWeight > 0) { "At least one weight must be positive" }
    }

    companion object {
        /**
         * 默认评分提示模板。
         */
        const val DEFAULT_SCORE_PROMPT_TEMPLATE = """
            Rate the relevance of the following document to the query on a scale from 0 to 1,
            where 0 means completely irrelevant and 1 means highly relevant.

            Query: {query}

            Document: {document}

            Provide only a single number between 0 and 1 as your answer, with up to two decimal places.
            Relevance score:
        """
    }
}

/**
 * 基于 LLM 的重排序器，使用语言模型评估查询和文档的相关性。
 *
 * @property agent 语言模型代理
 * @property config 重排序器配置
 */
class LlmReranker(
    private val agent: Agent,
    private val config: LlmRerankerConfig = LlmRerankerConfig()
) : Reranker {

    /**
     * 对搜索结果进行重排序。
     *
     * @param query 查询文本
     * @param results 原始搜索结果
     * @return 重排序后的搜索结果
     */
    override suspend fun rerank(query: String, results: List<SearchResult>): List<SearchResult> {
        if (results.isEmpty()) {
            return results
        }

        logger.debug { "Reranking ${results.size} results using LLM" }

        try {
            // 按批次处理结果
            val batches = results.chunked(config.batchSize)
            val rerankedResults = mutableListOf<SearchResult>()

            for (batch in batches) {
                val batchResults = rerankBatch(query, batch)
                rerankedResults.addAll(batchResults)
            }

            // 按组合分数降序排序
            return rerankedResults.sortedByDescending { it.score }
        } catch (e: Exception) {
            logger.error(e) { "Error reranking results using LLM" }
            return results
        }
    }

    /**
     * 对一批搜索结果进行重排序。
     *
     * @param query 查询文本
     * @param batch 一批搜索结果
     * @return 重排序后的搜索结果
     */
    private suspend fun rerankBatch(query: String, batch: List<SearchResult>): List<SearchResult> = coroutineScope {
        // 并行评估每个文档的相关性
        val scoringTasks = batch.map { result ->
            async {
                val llmScore = scoreDocument(query, result.document.content)

                // 计算组合分数
                val combinedScore = (result.score * config.originalScoreWeight + llmScore * config.llmScoreWeight) /
                    (config.originalScoreWeight + config.llmScoreWeight)

                SearchResult(result.document, combinedScore)
            }
        }

        // 等待所有评分任务完成
        scoringTasks.awaitAll()
    }

    /**
     * 使用 LLM 评估文档与查询的相关性。
     *
     * @param query 查询文本
     * @param document 文档内容
     * @return 相关性分数
     */
    private suspend fun scoreDocument(query: String, document: String): Double {
        // 准备提示
        val prompt = config.scorePromptTemplate
            .replace("{query}", query)
            .replace("{document}", document)

        try {
            // 调用 LLM
            val response = agent.generate(prompt).text

            // 解析分数
            val score = parseScore(response)

            // 确保分数在有效范围内
            return score.coerceIn(config.minScore, config.maxScore)
        } catch (e: Exception) {
            logger.error(e) { "Error scoring document using LLM" }
            return config.minScore
        }
    }

    /**
     * 从 LLM 响应中解析分数。
     *
     * @param response LLM 响应
     * @return 解析的分数
     */
    private fun parseScore(response: String): Double {
        // 尝试从响应中提取数字
        val scoreRegex = Regex("""(\d+(\.\d+)?)""")
        val matchResult = scoreRegex.find(response.trim())

        return matchResult?.groupValues?.get(1)?.toDoubleOrNull() ?: config.minScore
    }
}
