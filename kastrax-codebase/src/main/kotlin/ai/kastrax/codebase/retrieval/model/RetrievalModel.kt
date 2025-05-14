package ai.kastrax.codebase.retrieval.model

import ai.kastrax.codebase.embedding.CodeEmbeddingService
import ai.kastrax.codebase.semantic.memory.SemanticMemory
import ai.kastrax.codebase.semantic.memory.SemanticMemorySearchResult
import ai.kastrax.rag.embedding.EmbeddingService
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}



/**
 * 检索模型配置
 *
 * @property name 模型名称
 * @property embeddingModelName 嵌入模型名称
 * @property vectorDimension 向量维度
 * @property maxResults 最大结果数
 * @property minScore 最小分数
 * @property enableCaching 是否启用缓存
 * @property cacheSize 缓存大小
 * @property featureWeights 特征权重
 */
data class RetrievalModelConfig(
    val name: String = "default",
    val embeddingModelName: String = "default",
    val vectorDimension: Int = 1536,
    val maxResults: Int = 10,
    val minScore: Double = 0.7,
    val enableCaching: Boolean = true,
    val cacheSize: Int = 1000,
    val featureWeights: Map<String, Double> = mapOf(
        "semantic" to 0.7,
        "keyword" to 0.2,
        "recency" to 0.05,
        "popularity" to 0.05
    )
)

/**
 * 检索上下文
 *
 * @property query 查询文本
 * @property previousQueries 之前的查询列表
 * @property previousResults 之前的结果列表
 * @property userFeedback 用户反馈
 * @property currentFile 当前文件
 * @property currentPosition 当前位置
 * @property selectedText 选中的文本
 * @property metadata 元数据
 */
data class RetrievalContext(
    val query: String,
    val previousQueries: List<String> = emptyList(),
    val previousResults: List<SemanticMemorySearchResult> = emptyList(),
    val userFeedback: Map<String, Double> = emptyMap(),
    val currentFile: String? = null,
    val currentPosition: Int? = null,
    val selectedText: String? = null,
    val metadata: Map<String, Any> = emptyMap()
)


/**
 * 检索模型
 *
 * 用于根据查询上下文提供更精确的代码检索结果
 *
 * @property embeddingService 嵌入服务
 * @property config 配置
 */
abstract class RetrievalModel(
    protected val embeddingService: EmbeddingService,
    protected val config: RetrievalModelConfig = RetrievalModelConfig()
) {
    // 查询缓存
    protected val queryCache = ConcurrentHashMap<String, List<MemoryRetrievalResult>>()

    /**
     * 检索记忆
     *
     * @param context 检索上下文
     * @param limit 返回结果的最大数量
     * @param minScore 最小分数
     * @return 检索结果列表
     */
    abstract suspend fun retrieve(
        context: RetrievalContext,
        limit: Int = config.maxResults,
        minScore: Double = config.minScore
    ): List<MemoryRetrievalResult>

    /**
     * 计算特征
     *
     * @param memory 语义记忆
     * @param context 检索上下文
     * @return 特征列表
     */
    abstract suspend fun computeFeatures(
        memory: SemanticMemory,
        context: RetrievalContext
    ): List<RetrievalFeature>

    /**
     * 计算最终分数
     *
     * @param features 特征列表
     * @return 最终分数
     */
    protected fun computeFinalScore(features: List<RetrievalFeature>): Double {
        if (features.isEmpty()) {
            return 0.0
        }

        var totalScore = 0.0
        var totalWeight = 0.0

        features.forEach { feature ->
            totalScore += feature.value * feature.weight
            totalWeight += feature.weight
        }

        return if (totalWeight > 0) {
            totalScore / totalWeight
        } else {
            0.0
        }
    }

    /**
     * 生成解释
     *
     * @param memory 语义记忆
     * @param features 特征列表
     * @param finalScore 最终分数
     * @return 解释
     */
    protected fun generateExplanation(
        memory: SemanticMemory,
        features: List<RetrievalFeature>,
        finalScore: Double
    ): String {
        val sb = StringBuilder()

        sb.appendLine("记忆: ${memory.getShortDescription()}")
        sb.appendLine("最终分数: $finalScore")
        sb.appendLine("特征:")

        features.forEach { feature ->
            sb.appendLine("- ${feature.name}: ${feature.value} (权重: ${feature.weight})")
        }

        return sb.toString()
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        queryCache.clear()
    }
}
