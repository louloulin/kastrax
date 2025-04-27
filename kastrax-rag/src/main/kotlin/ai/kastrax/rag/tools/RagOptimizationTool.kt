package ai.kastrax.rag.tools

import ai.kastrax.core.llm.LlmClient
import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.retrieval.EnhancedHybridRetrieverConfig
import ai.kastrax.rag.retrieval.HybridRetrieverConfig
import ai.kastrax.rag.retrieval.HybridStrategy
import ai.kastrax.rag.retrieval.QueryEnhancedRetrieverConfig
import ai.kastrax.rag.retrieval.SemanticRetrieverConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

private val logger = KotlinLogging.logger {}

/**
 * RAG 优化工具，用于优化 RAG 系统的配置。
 *
 * @property rag RAG 系统
 * @property llmClient LLM 客户端，用于 LLM 辅助评估
 * @property evaluationTool RAG 评估工具
 */
class RagOptimizationTool(
    private val rag: RAG,
    private val llmClient: LlmClient? = null,
    private val evaluationTool: RagEvaluationTool = RagEvaluationTool(rag, llmClient)
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
                    val results = evaluationTool.evaluateBatch(
                        queries = queries,
                        generateAnswer = generateAnswer,
                        options = config,
                        groundTruths = groundTruths
                    )
                    
                    // 计算平均分数
                    val averageScore = results.map { it.overallScore }.average()
                    
                    // 返回配置和评估结果
                    ConfigurationResult(config, averageScore, results)
                } catch (e: Exception) {
                    logger.error(e) { "Error evaluating configuration: $config" }
                    ConfigurationResult(config, 0.0, emptyList(), e.message)
                }
            }
        }.map { it.await() }

        // 按分数排序
        val sortedResults = configResults.sortedByDescending { it.averageScore }
        
        // 获取最佳配置
        val bestConfig = sortedResults.firstOrNull()?.configuration ?: RagProcessOptions()
        
        RagOptimizationResult(
            bestConfiguration = bestConfig,
            allResults = sortedResults,
            optimizationOptions = optimizationOptions
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
        
        // 基础配置
        configurations.add(RagProcessOptions())
        
        // 检索器配置
        if (options.optimizeRetriever) {
            // 基础 Top-K 检索器
            configurations.add(RagProcessOptions(
                useHybridSearch = false,
                useEnhancedHybridSearch = false,
                useSemanticRetrieval = false,
                useReranking = true
            ))
            
            // 语义检索器
            configurations.add(RagProcessOptions(
                useHybridSearch = false,
                useEnhancedHybridSearch = false,
                useSemanticRetrieval = true,
                useReranking = true,
                semanticOptions = SemanticRetrieverConfig(
                    expandQuery = true,
                    useClustering = true
                )
            ))
            
            // 混合检索器
            configurations.add(RagProcessOptions(
                useHybridSearch = true,
                useEnhancedHybridSearch = false,
                useSemanticRetrieval = false,
                useReranking = true,
                hybridOptions = HybridRetrieverConfig(
                    hybridStrategy = HybridStrategy.WEIGHTED,
                    vectorWeight = 0.7,
                    keywordWeight = 0.3,
                    expandLimit = 2.0
                )
            ))
            
            // 增强混合检索器
            configurations.add(RagProcessOptions(
                useHybridSearch = false,
                useEnhancedHybridSearch = true,
                useSemanticRetrieval = false,
                useReranking = true,
                enhancedHybridOptions = EnhancedHybridRetrieverConfig(
                    useSemanticSearch = true,
                    useKeywordSearch = true,
                    useMetadataSearch = false,
                    hybridStrategy = HybridStrategy.WEIGHTED,
                    vectorWeight = 0.7,
                    keywordWeight = 0.3
                )
            ))
        }
        
        // 查询增强配置
        if (options.optimizeQueryEnhancement) {
            // 启用查询增强
            configurations.add(RagProcessOptions(
                useHybridSearch = true,
                useQueryEnhancement = true,
                hybridOptions = HybridRetrieverConfig(
                    hybridStrategy = HybridStrategy.WEIGHTED,
                    vectorWeight = 0.7,
                    keywordWeight = 0.3
                ),
                queryEnhancementOptions = QueryEnhancedRetrieverConfig(
                    useMultiQuery = true,
                    maxQueriesPerRequest = 3
                )
            ))
            
            // 增强混合检索器 + 查询增强
            configurations.add(RagProcessOptions(
                useHybridSearch = false,
                useEnhancedHybridSearch = true,
                useQueryEnhancement = true,
                enhancedHybridOptions = EnhancedHybridRetrieverConfig(
                    useSemanticSearch = true,
                    useKeywordSearch = true,
                    hybridStrategy = HybridStrategy.WEIGHTED
                ),
                queryEnhancementOptions = QueryEnhancedRetrieverConfig(
                    useMultiQuery = true,
                    maxQueriesPerRequest = 3
                )
            ))
        }
        
        // 重排序配置
        if (options.optimizeReranking) {
            // 启用上下文感知重排序
            configurations.add(RagProcessOptions(
                useHybridSearch = true,
                useReranking = true,
                useContextAwareReranking = true,
                hybridOptions = HybridRetrieverConfig(
                    hybridStrategy = HybridStrategy.WEIGHTED,
                    vectorWeight = 0.7,
                    keywordWeight = 0.3
                )
            ))
            
            // 增强混合检索器 + 上下文感知重排序
            configurations.add(RagProcessOptions(
                useHybridSearch = false,
                useEnhancedHybridSearch = true,
                useReranking = true,
                useContextAwareReranking = true,
                enhancedHybridOptions = EnhancedHybridRetrieverConfig(
                    useSemanticSearch = true,
                    useKeywordSearch = true,
                    hybridStrategy = HybridStrategy.WEIGHTED
                )
            ))
        }
        
        // 上下文构建配置
        if (options.optimizeContextBuilding) {
            // 优化上下文构建
            configurations.add(RagProcessOptions(
                useHybridSearch = true,
                useReranking = true,
                hybridOptions = HybridRetrieverConfig(
                    hybridStrategy = HybridStrategy.WEIGHTED,
                    vectorWeight = 0.7,
                    keywordWeight = 0.3
                ),
                contextOptions = ContextBuilderConfig(
                    maxTokens = 2048,
                    addMetadata = true,
                    formatTemplate = "文档内容: {content}\n来源: {source}\n",
                    addQueryToContext = true
                )
            ))
            
            // 增强混合检索器 + 优化上下文构建
            configurations.add(RagProcessOptions(
                useHybridSearch = false,
                useEnhancedHybridSearch = true,
                useReranking = true,
                enhancedHybridOptions = EnhancedHybridRetrieverConfig(
                    useSemanticSearch = true,
                    useKeywordSearch = true,
                    hybridStrategy = HybridStrategy.WEIGHTED
                ),
                contextOptions = ContextBuilderConfig(
                    maxTokens = 2048,
                    addMetadata = true,
                    formatTemplate = "文档内容: {content}\n来源: {source}\n",
                    addQueryToContext = true
                )
            ))
        }
        
        // 组合配置
        if (options.optimizeCombinations) {
            // 最佳组合配置
            configurations.add(RagProcessOptions(
                useHybridSearch = false,
                useEnhancedHybridSearch = true,
                useQueryEnhancement = true,
                useReranking = true,
                useContextAwareReranking = true,
                enhancedHybridOptions = EnhancedHybridRetrieverConfig(
                    useSemanticSearch = true,
                    useKeywordSearch = true,
                    hybridStrategy = HybridStrategy.WEIGHTED,
                    vectorWeight = 0.7,
                    keywordWeight = 0.3
                ),
                queryEnhancementOptions = QueryEnhancedRetrieverConfig(
                    useMultiQuery = true,
                    maxQueriesPerRequest = 3
                ),
                contextOptions = ContextBuilderConfig(
                    maxTokens = 2048,
                    addMetadata = true,
                    formatTemplate = "文档内容: {content}\n来源: {source}\n",
                    addQueryToContext = true
                )
            ))
        }
        
        return configurations
    }

    /**
     * 生成优化报告。
     *
     * @param result 优化结果
     * @param detailed 是否生成详细报告
     * @return 优化报告
     */
    fun generateReport(result: RagOptimizationResult, detailed: Boolean = false): String {
        return buildString {
            append("# RAG 优化报告\n\n")
            
            append("## 最佳配置\n\n")
            append("平均分数: ${result.allResults.firstOrNull()?.averageScore?.format(2) ?: "N/A"}\n\n")
            append("配置详情:\n\n")
            append("```\n")
            append(formatConfiguration(result.bestConfiguration))
            append("\n```\n\n")
            
            append("## 所有配置评分\n\n")
            append("| 配置 | 平均分数 |\n")
            append("|------|----------|\n")
            
            result.allResults.forEachIndexed { index, configResult ->
                append("| 配置 ${index + 1} | ${configResult.averageScore.format(2)} |\n")
            }
            
            if (detailed) {
                append("\n## 配置详情\n\n")
                result.allResults.forEachIndexed { index, configResult ->
                    append("### 配置 ${index + 1}\n\n")
                    append("平均分数: ${configResult.averageScore.format(2)}\n\n")
                    append("配置详情:\n\n")
                    append("```\n")
                    append(formatConfiguration(configResult.configuration))
                    append("\n```\n\n")
                    
                    if (configResult.error != null) {
                        append("错误: ${configResult.error}\n\n")
                    }
                    
                    append("查询评估结果:\n\n")
                    append("| 查询 | 总体分数 |\n")
                    append("|------|----------|\n")
                    
                    configResult.results.forEach { result ->
                        append("| ${result.query.take(30)}${if (result.query.length > 30) "..." else ""} ")
                        append("| ${result.overallScore.format(2)} |\n")
                    }
                    
                    append("\n---\n\n")
                }
            }
            
            append("## 优化选项\n\n")
            append("- 优化检索器: ${result.optimizationOptions.optimizeRetriever}\n")
            append("- 优化查询增强: ${result.optimizationOptions.optimizeQueryEnhancement}\n")
            append("- 优化重排序: ${result.optimizationOptions.optimizeReranking}\n")
            append("- 优化上下文构建: ${result.optimizationOptions.optimizeContextBuilding}\n")
            append("- 优化组合: ${result.optimizationOptions.optimizeCombinations}\n")
        }
    }

    /**
     * 格式化配置。
     *
     * @param config 配置
     * @return 格式化后的配置字符串
     */
    private fun formatConfiguration(config: RagProcessOptions): String {
        return buildString {
            append("useHybridSearch: ${config.useHybridSearch}\n")
            append("useEnhancedHybridSearch: ${config.useEnhancedHybridSearch}\n")
            append("useSemanticRetrieval: ${config.useSemanticRetrieval}\n")
            append("useReranking: ${config.useReranking}\n")
            append("useContextAwareReranking: ${config.useContextAwareReranking}\n")
            append("useQueryEnhancement: ${config.useQueryEnhancement}\n")
            
            if (config.useHybridSearch) {
                append("\nhybridOptions:\n")
                append("  hybridStrategy: ${config.hybridOptions.hybridStrategy}\n")
                append("  vectorWeight: ${config.hybridOptions.vectorWeight}\n")
                append("  keywordWeight: ${config.hybridOptions.keywordWeight}\n")
                append("  expandLimit: ${config.hybridOptions.expandLimit}\n")
            }
            
            if (config.useEnhancedHybridSearch) {
                append("\nenhancedHybridOptions:\n")
                append("  useSemanticSearch: ${config.enhancedHybridOptions.useSemanticSearch}\n")
                append("  useKeywordSearch: ${config.enhancedHybridOptions.useKeywordSearch}\n")
                append("  useMetadataSearch: ${config.enhancedHybridOptions.useMetadataSearch}\n")
                append("  hybridStrategy: ${config.enhancedHybridOptions.hybridStrategy}\n")
                append("  vectorWeight: ${config.enhancedHybridOptions.vectorWeight}\n")
                append("  keywordWeight: ${config.enhancedHybridOptions.keywordWeight}\n")
            }
            
            if (config.useSemanticRetrieval) {
                append("\nsemanticOptions:\n")
                append("  expandQuery: ${config.semanticOptions.expandQuery}\n")
                append("  useClustering: ${config.semanticOptions.useClustering}\n")
            }
            
            if (config.useQueryEnhancement) {
                append("\nqueryEnhancementOptions:\n")
                append("  useMultiQuery: ${config.queryEnhancementOptions.useMultiQuery}\n")
                append("  maxQueriesPerRequest: ${config.queryEnhancementOptions.maxQueriesPerRequest}\n")
            }
            
            append("\ncontextOptions:\n")
            append("  maxTokens: ${config.contextOptions.maxTokens}\n")
            append("  addMetadata: ${config.contextOptions.addMetadata}\n")
            append("  formatTemplate: ${config.contextOptions.formatTemplate}\n")
            append("  addQueryToContext: ${config.contextOptions.addQueryToContext}\n")
        }
    }

    /**
     * 格式化 Double 值，保留指定位数的小数。
     *
     * @param digits 小数位数
     * @return 格式化后的字符串
     */
    private fun Double.format(digits: Int): String {
        return "%.${digits}f".format(this)
    }
}

/**
 * RAG 优化选项。
 *
 * @property optimizeRetriever 是否优化检索器
 * @property optimizeQueryEnhancement 是否优化查询增强
 * @property optimizeReranking 是否优化重排序
 * @property optimizeContextBuilding 是否优化上下文构建
 * @property optimizeCombinations 是否优化组合
 */
data class RagOptimizationOptions(
    val optimizeRetriever: Boolean = true,
    val optimizeQueryEnhancement: Boolean = true,
    val optimizeReranking: Boolean = true,
    val optimizeContextBuilding: Boolean = true,
    val optimizeCombinations: Boolean = true
)

/**
 * 配置评估结果。
 *
 * @property configuration 配置
 * @property averageScore 平均分数
 * @property results 评估结果列表
 * @property error 错误信息（如果有）
 */
data class ConfigurationResult(
    val configuration: RagProcessOptions,
    val averageScore: Double,
    val results: List<RagEvaluationResult>,
    val error: String? = null
)

/**
 * RAG 优化结果。
 *
 * @property bestConfiguration 最佳配置
 * @property allResults 所有配置的评估结果
 * @property optimizationOptions 优化选项
 */
data class RagOptimizationResult(
    val bestConfiguration: RagProcessOptions,
    val allResults: List<ConfigurationResult>,
    val optimizationOptions: RagOptimizationOptions
)
