package ai.kastrax.rag.optimization

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.evaluation.RagEvaluationTool
import ai.kastrax.rag.evaluation.RagEvaluationResult
import ai.kastrax.rag.HybridOptions
import ai.kastrax.rag.SemanticOptions
import ai.kastrax.rag.QueryEnhancementOptions
import ai.kastrax.rag.context.ContextBuilderConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable

private val logger = KotlinLogging.logger {}

/**
 * RAG 优化选项。
 *
 * @property optimizeHybridSearch 是否优化混合搜索
 * @property optimizeSemanticSearch 是否优化语义搜索
 * @property optimizeQueryEnhancement 是否优化查询增强
 * @property optimizeContextBuilding 是否优化上下文构建
 * @property maxConfigurations 最大配置数量
 */
@Serializable
data class RagOptimizationOptions(
    val optimizeHybridSearch: Boolean = true,
    val optimizeSemanticSearch: Boolean = true,
    val optimizeQueryEnhancement: Boolean = true,
    val optimizeContextBuilding: Boolean = true,
    val maxConfigurations: Int = 10
)

/**
 * 配置结果。
 *
 * @property config RAG 处理选项
 * @property score 评分
 * @property evaluationResults 评估结果列表
 * @property error 错误信息
 */
data class ConfigurationResult(
    val config: RagProcessOptions,
    val score: Double,
    val evaluationResults: List<RagEvaluationResult>,
    val error: String? = null
)

/**
 * RAG 优化结果。
 *
 * @property bestConfig 最佳配置
 * @property bestScore 最佳评分
 * @property allConfigurations 所有配置结果
 */
data class RagOptimizationResult(
    val bestConfig: RagProcessOptions,
    val bestScore: Double,
    val allConfigurations: List<ConfigurationResult>
)

/**
 * RAG 优化工具，用于优化 RAG 系统的配置。
 *
 * @property rag RAG 系统
 * @property evaluationTool RAG 评估工具
 */
class RagOptimizationTool(
    private val rag: RAG,
    private val evaluationTool: RagEvaluationTool
) {
    /**
     * 优化 RAG 系统的配置。
     *
     * @param queries 测试查询列表
     * @param generateAnswer 生成回答的函数
     * @param groundTruths 参考答案列表（可选）
     * @param optimizationOptions 优化选项
     * @return 优化结果
     */
    suspend fun optimize(
        queries: List<String>,
        generateAnswer: suspend (String, String) -> String,
        groundTruths: List<String>? = null,
        optimizationOptions: RagOptimizationOptions = RagOptimizationOptions()
    ): RagOptimizationResult = coroutineScope {
        logger.info { "Optimizing RAG configuration for ${queries.size} queries" }

        // 创建要测试的配置列表
        val configurations = createConfigurations(optimizationOptions)
        logger.info { "Created ${configurations.size} configurations to test" }

        // 并行评估每个配置
        val configResults = configurations.map { config ->
            async {
                try {
                    // 评估当前配置
                    // 评估每个查询
                    val results = mutableListOf<RagEvaluationResult>()
                    for (i in queries.indices) {
                        val query = queries[i]
                        val groundTruth = groundTruths?.getOrNull(i)

                        // 生成上下文
                        val context = rag.generateContext(query, options = config)

                        // 生成回答
                        val answer = generateAnswer(query, context)

                        // 评估
                        val result = evaluationTool.runFullEvaluation(
                            query = query,
                            expectedAnswer = groundTruth ?: "",
                            relevantDocIds = emptyList()
                        )

                        results.add(result)
                    }

                    // 计算平均分数
                    val averageScore = results.map { it.totalScore }.average()

                    // 返回配置和评估结果
                    ConfigurationResult(config, averageScore, results)
                } catch (e: Exception) {
                    logger.error(e) { "Error evaluating configuration: $config" }
                    ConfigurationResult(config, 0.0, emptyList(), e.message)
                }
            }
        }.awaitAll()

        // 找出最佳配置
        val bestResult = configResults.maxByOrNull { it.score }
            ?: throw IllegalStateException("No valid configurations found")

        logger.info { "Best configuration found with score ${bestResult.score}: ${bestResult.config}" }

        RagOptimizationResult(
            bestConfig = bestResult.config,
            bestScore = bestResult.score,
            allConfigurations = configResults.sortedByDescending { it.score }
        )
    }

    /**
     * 创建要测试的配置列表。
     *
     * @param options 优化选项
     * @return 配置列表
     */
    private fun createConfigurations(options: RagOptimizationOptions): List<RagProcessOptions> {
        val configurations = mutableListOf<RagProcessOptions>()

        // 添加默认配置
        configurations.add(RagProcessOptions())

        // 优化混合搜索
        if (options.optimizeHybridSearch) {
            configurations.addAll(createHybridSearchConfigurations())
        }

        // 优化语义搜索
        if (options.optimizeSemanticSearch) {
            configurations.addAll(createSemanticSearchConfigurations())
        }

        // 优化查询增强
        if (options.optimizeQueryEnhancement) {
            configurations.addAll(createQueryEnhancementConfigurations())
        }

        // 优化上下文构建
        if (options.optimizeContextBuilding) {
            configurations.addAll(createContextBuildingConfigurations())
        }

        // 组合配置
        configurations.addAll(createCombinedConfigurations())

        // 限制配置数量
        return configurations.take(options.maxConfigurations)
    }

    /**
     * 创建混合搜索配置。
     *
     * @return 配置列表
     */
    private fun createHybridSearchConfigurations(): List<RagProcessOptions> {
        return listOf(
            RagProcessOptions(
                useHybridSearch = true,
                hybridOptions = HybridOptions(
                    vectorWeight = 0.7,
                    keywordWeight = 0.3
                )
            ),
            RagProcessOptions(
                useHybridSearch = true,
                hybridOptions = HybridOptions(
                    vectorWeight = 0.5,
                    keywordWeight = 0.5
                )
            ),
            RagProcessOptions(
                useHybridSearch = true,
                hybridOptions = HybridOptions(
                    vectorWeight = 0.3,
                    keywordWeight = 0.7
                )
            )
        )
    }

    /**
     * 创建语义搜索配置。
     *
     * @return 配置列表
     */
    private fun createSemanticSearchConfigurations(): List<RagProcessOptions> {
        return listOf(
            RagProcessOptions(
                useSemanticRetrieval = true,
                semanticOptions = SemanticOptions(
                    useChunking = true,
                    chunkSize = 1000,
                    chunkOverlap = 200
                )
            ),
            RagProcessOptions(
                useSemanticRetrieval = true,
                semanticOptions = SemanticOptions(
                    useChunking = false,
                    chunkSize = 800,
                    chunkOverlap = 100
                )
            ),
            RagProcessOptions(
                useSemanticRetrieval = true,
                semanticOptions = SemanticOptions(
                    useChunking = true,
                    chunkSize = 500,
                    chunkOverlap = 50
                )
            )
        )
    }

    /**
     * 创建查询增强配置。
     *
     * @return 配置列表
     */
    private fun createQueryEnhancementConfigurations(): List<RagProcessOptions> {
        return listOf(
            RagProcessOptions(
                useQueryEnhancement = true,
                queryEnhancementOptions = QueryEnhancementOptions(
                    useSynonyms = true,
                    useDecomposition = false,
                    useNormalization = true
                )
            ),
            RagProcessOptions(
                useQueryEnhancement = true,
                queryEnhancementOptions = QueryEnhancementOptions(
                    useSynonyms = false,
                    useDecomposition = true,
                    useNormalization = true
                )
            ),
            RagProcessOptions(
                useQueryEnhancement = true,
                queryEnhancementOptions = QueryEnhancementOptions(
                    useSynonyms = true,
                    useDecomposition = true,
                    useNormalization = false
                )
            )
        )
    }

    /**
     * 创建上下文构建配置。
     *
     * @return 配置列表
     */
    private fun createContextBuildingConfigurations(): List<RagProcessOptions> {
        return listOf(
            RagProcessOptions(
                contextOptions = ContextBuilderConfig(
                    maxTokens = 1000,
                    includeMetadata = true
                )
            ),
            RagProcessOptions(
                contextOptions = ContextBuilderConfig(
                    maxTokens = 2000,
                    includeMetadata = true
                )
            ),
            RagProcessOptions(
                contextOptions = ContextBuilderConfig(
                    maxTokens = 3000,
                    includeMetadata = true
                )
            )
        )
    }

    /**
     * 创建组合配置。
     *
     * @return 配置列表
     */
    private fun createCombinedConfigurations(): List<RagProcessOptions> {
        return listOf(
            RagProcessOptions(
                useHybridSearch = true,
                useQueryEnhancement = true,
                hybridOptions = HybridOptions(
                    vectorWeight = 0.7,
                    keywordWeight = 0.3
                ),
                queryEnhancementOptions = QueryEnhancementOptions(
                    useSynonyms = true,
                    useDecomposition = false,
                    useNormalization = true
                )
            ),
            RagProcessOptions(
                useSemanticRetrieval = true,
                useReranking = true,
                semanticOptions = SemanticOptions(
                    useChunking = true,
                    chunkSize = 1000,
                    chunkOverlap = 200
                )
            ),
            RagProcessOptions(
                useHybridSearch = true,
                useSemanticRetrieval = true,
                useQueryEnhancement = true,
                useReranking = true,
                hybridOptions = HybridOptions(
                    vectorWeight = 0.6,
                    keywordWeight = 0.4
                ),
                semanticOptions = SemanticOptions(
                    useChunking = true,
                    chunkSize = 800,
                    chunkOverlap = 100
                ),
                queryEnhancementOptions = QueryEnhancementOptions(
                    useSynonyms = true,
                    useDecomposition = true,
                    useNormalization = true
                )
            )
        )
    }
}
