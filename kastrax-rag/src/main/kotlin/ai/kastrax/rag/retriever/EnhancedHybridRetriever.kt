package ai.kastrax.rag.retriever

import ai.kastrax.store.document.DocumentSearchResult
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * 增强混合检索器配置。
 *
 * @property vectorWeight 向量权重
 * @property keywordWeight 关键词权重
 * @property metadataWeight 元数据权重
 * @property hybridStrategy 混合策略
 */
data class EnhancedHybridRetrieverConfig(
    val vectorWeight: Double = 0.6,
    val keywordWeight: Double = 0.3,
    val metadataWeight: Double = 0.1,
    val hybridStrategy: HybridStrategy = HybridStrategy.WEIGHTED_AVERAGE
)

/**
 * 混合策略。
 */
enum class HybridStrategy {
    /**
     * 加权平均。
     */
    WEIGHTED_AVERAGE,
    
    /**
     * 最大值。
     */
    MAX,
    
    /**
     * 投票。
     */
    VOTING
}

/**
 * 增强混合检索器，结合向量检索、关键词检索和元数据检索。
 *
 * @property vectorRetriever 向量检索器
 * @property keywordRetriever 关键词检索器
 * @property metadataRetriever 元数据检索器
 * @property config 配置
 */
class EnhancedHybridRetriever(
    private val vectorRetriever: Retriever,
    private val keywordRetriever: Retriever,
    private val metadataRetriever: Retriever? = null,
    private val config: EnhancedHybridRetrieverConfig = EnhancedHybridRetrieverConfig()
) : Retriever {
    init {
        val sum = config.vectorWeight + config.keywordWeight + (if (metadataRetriever != null) config.metadataWeight else 0.0)
        require(sum > 0.99 && sum < 1.01) {
            "Vector weight (${config.vectorWeight}) + keyword weight (${config.keywordWeight}) + " +
                    "metadata weight (${config.metadataWeight}) must be approximately 1.0"
        }
    }
    
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
            // 并行执行各种检索
            val vectorResults = async { vectorRetriever.retrieve(query, limit * 2, 0.0) }
            val keywordResults = async { keywordRetriever.retrieve(query, limit * 2, 0.0) }
            val metadataResults = if (metadataRetriever != null) {
                async { metadataRetriever.retrieve(query, limit * 2, 0.0) }
            } else {
                null
            }
            
            // 等待所有检索完成
            val results = if (metadataResults != null) {
                val (vector, keyword, metadata) = awaitAll(vectorResults, keywordResults, metadataResults)
                mergeResults(
                    vectorResults = vector as List<DocumentSearchResult>,
                    keywordResults = keyword as List<DocumentSearchResult>,
                    metadataResults = metadata as List<DocumentSearchResult>,
                    limit = limit
                )
            } else {
                val (vector, keyword) = awaitAll(vectorResults, keywordResults)
                mergeResults(
                    vectorResults = vector as List<DocumentSearchResult>,
                    keywordResults = keyword as List<DocumentSearchResult>,
                    metadataResults = emptyList(),
                    limit = limit
                )
            }
            
            // 过滤低分结果
            return@withContext results.filter { it.score >= minScore }
        } catch (e: Exception) {
            logger.error(e) { "Error retrieving documents with enhanced hybrid retriever" }
            return@withContext emptyList()
        }
    }
    
    /**
     * 合并检索结果。
     *
     * @param vectorResults 向量检索结果
     * @param keywordResults 关键词检索结果
     * @param metadataResults 元数据检索结果
     * @param limit 返回结果的最大数量
     * @return 合并后的结果列表
     */
    private fun mergeResults(
        vectorResults: List<DocumentSearchResult>,
        keywordResults: List<DocumentSearchResult>,
        metadataResults: List<DocumentSearchResult>,
        limit: Int
    ): List<DocumentSearchResult> {
        // 创建 ID 到结果的映射
        val resultMap = mutableMapOf<String, MutableMap<String, Any>>()
        
        // 处理向量检索结果
        vectorResults.forEach { result ->
            resultMap[result.document.id] = mutableMapOf(
                "document" to result.document,
                "vectorScore" to result.score,
                "keywordScore" to 0.0,
                "metadataScore" to 0.0,
                "finalScore" to (result.score * config.vectorWeight)
            )
        }
        
        // 处理关键词检索结果
        keywordResults.forEach { result ->
            val id = result.document.id
            if (id in resultMap) {
                // 更新已存在的结果
                val entry = resultMap[id]!!
                entry["keywordScore"] = result.score
                
                // 根据混合策略更新最终分数
                when (config.hybridStrategy) {
                    HybridStrategy.WEIGHTED_AVERAGE -> {
                        entry["finalScore"] = (entry["vectorScore"] as Double) * config.vectorWeight +
                                (result.score * config.keywordWeight) +
                                (entry["metadataScore"] as Double) * config.metadataWeight
                    }
                    HybridStrategy.MAX -> {
                        val maxScore = maxOf(
                            (entry["vectorScore"] as Double) * config.vectorWeight,
                            result.score * config.keywordWeight,
                            (entry["metadataScore"] as Double) * config.metadataWeight
                        )
                        entry["finalScore"] = maxScore
                    }
                    HybridStrategy.VOTING -> {
                        // 简单投票：每个检索器的分数大于 0.5 算一票
                        var votes = 0
                        if ((entry["vectorScore"] as Double) > 0.5) votes++
                        if (result.score > 0.5) votes++
                        if ((entry["metadataScore"] as Double) > 0.5) votes++
                        entry["finalScore"] = votes.toDouble() / 3.0
                    }
                }
            } else {
                // 添加新结果
                resultMap[id] = mutableMapOf(
                    "document" to result.document,
                    "vectorScore" to 0.0,
                    "keywordScore" to result.score,
                    "metadataScore" to 0.0,
                    "finalScore" to (result.score * config.keywordWeight)
                )
            }
        }
        
        // 处理元数据检索结果
        metadataResults.forEach { result ->
            val id = result.document.id
            if (id in resultMap) {
                // 更新已存在的结果
                val entry = resultMap[id]!!
                entry["metadataScore"] = result.score
                
                // 根据混合策略更新最终分数
                when (config.hybridStrategy) {
                    HybridStrategy.WEIGHTED_AVERAGE -> {
                        entry["finalScore"] = (entry["vectorScore"] as Double) * config.vectorWeight +
                                (entry["keywordScore"] as Double) * config.keywordWeight +
                                (result.score * config.metadataWeight)
                    }
                    HybridStrategy.MAX -> {
                        val maxScore = maxOf(
                            (entry["vectorScore"] as Double) * config.vectorWeight,
                            (entry["keywordScore"] as Double) * config.keywordWeight,
                            result.score * config.metadataWeight
                        )
                        entry["finalScore"] = maxScore
                    }
                    HybridStrategy.VOTING -> {
                        // 简单投票：每个检索器的分数大于 0.5 算一票
                        var votes = 0
                        if ((entry["vectorScore"] as Double) > 0.5) votes++
                        if ((entry["keywordScore"] as Double) > 0.5) votes++
                        if (result.score > 0.5) votes++
                        entry["finalScore"] = votes.toDouble() / 3.0
                    }
                }
            } else {
                // 添加新结果
                resultMap[id] = mutableMapOf(
                    "document" to result.document,
                    "vectorScore" to 0.0,
                    "keywordScore" to 0.0,
                    "metadataScore" to result.score,
                    "finalScore" to (result.score * config.metadataWeight)
                )
            }
        }
        
        // 转换为 DocumentSearchResult 列表并排序
        return resultMap.values
            .map { entry ->
                DocumentSearchResult(
                    document = entry["document"] as ai.kastrax.store.document.Document,
                    score = entry["finalScore"] as Double
                )
            }
            .sortedByDescending { it.score }
            .take(limit)
    }
}
