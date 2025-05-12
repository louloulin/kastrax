package ai.kastrax.codebase.semantic.pattern.impl

import ai.kastrax.codebase.semantic.flow.FlowGraph
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.pattern.*
import ai.kastrax.codebase.semantic.pattern.detector.*
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext

/**
 * 代码模式识别器实现
 *
 * @property config 配置
 * @property detectors 模式检测器列表
 */
class CodePatternRecognizerImpl(
    private val config: CodePatternRecognizerConfig = CodePatternRecognizerConfig(),
    private val detectors: List<PatternDetector> = defaultDetectors()
) : CodePatternRecognizer {
    private val logger = KotlinLogging.logger {}

    override suspend fun recognizePatterns(element: CodeElement): List<PatternMatch> = withContext(Dispatchers.Default) {
        logger.info { "开始识别代码模式: ${element.qualifiedName}" }

        // 过滤出配置中指定的模式类别对应的检测器
        val activeDetectors = detectors.filter { detector ->
            detector.getCategory() in config.patternCategories
        }

        if (activeDetectors.isEmpty()) {
            logger.warn { "没有启用的模式检测器" }
            return@withContext emptyList()
        }

        try {
            // 并行运行所有检测器
            val results = coroutineScope {
                activeDetectors.map { detector ->
                    async {
                        try {
                            detector.detectPatterns(element)
                        } catch (e: Exception) {
                            logger.error(e) { "检测器 ${detector.javaClass.simpleName} 执行出错" }
                            emptyList()
                        }
                    }
                }.awaitAll().flatten()
            }

            // 过滤和排序结果
            val filteredResults = results
                .filter { it.confidence >= config.minConfidence }
                .sortedByDescending { it.confidence }
                .take(config.maxResults)

            logger.info { "代码模式识别完成: ${element.qualifiedName}, 找到 ${filteredResults.size} 个模式" }
            return@withContext filteredResults
        } catch (e: Exception) {
            logger.error(e) { "识别代码模式时出错: ${element.qualifiedName}" }
            return@withContext emptyList()
        }
    }

    override suspend fun recognizePatternsFromFlowGraph(flowGraph: FlowGraph): List<PatternMatch> = withContext(Dispatchers.Default) {
        logger.info { "开始从流图识别代码模式: ${flowGraph.name}" }

        // 过滤出支持流图分析的检测器
        val flowGraphDetectors = detectors.filterIsInstance<FlowGraphPatternDetector>()
            .filter { detector -> detector.getCategory() in config.patternCategories }

        if (flowGraphDetectors.isEmpty()) {
            logger.warn { "没有支持流图分析的模式检测器" }
            return@withContext emptyList()
        }

        try {
            // 并行运行所有流图检测器
            val results = coroutineScope {
                flowGraphDetectors.map { detector ->
                    async {
                        try {
                            detector.detectPatternsFromFlowGraph(flowGraph)
                        } catch (e: Exception) {
                            logger.error(e) { "流图检测器 ${detector.javaClass.simpleName} 执行出错" }
                            emptyList()
                        }
                    }
                }.awaitAll().flatten()
            }

            // 过滤和排序结果
            val filteredResults = results
                .filter { it.confidence >= config.minConfidence }
                .sortedByDescending { it.confidence }
                .take(config.maxResults)

            logger.info { "流图代码模式识别完成: ${flowGraph.name}, 找到 ${filteredResults.size} 个模式" }
            return@withContext filteredResults
        } catch (e: Exception) {
            logger.error(e) { "从流图识别代码模式时出错: ${flowGraph.name}" }
            return@withContext emptyList()
        }
    }

    override fun getSupportedCategories(): Set<PatternCategory> {
        return detectors.map { it.getCategory() }.toSet()
    }

    override fun getKnownPatterns(): Map<String, String> {
        return detectors.flatMap { detector ->
            detector.getSupportedPatterns().map { it.first to it.second }
        }.toMap()
    }

    companion object {
        /**
         * 创建默认的模式检测器列表
         *
         * @return 默认的模式检测器列表
         */
        fun defaultDetectors(): List<PatternDetector> {
            return listOf(
                DesignPatternDetector()
                // 其他检测器尚未实现
                // AntiPatternDetector(),
                // CodeSmellDetector(),
                // PerformancePatternDetector(),
                // SecurityPatternDetector(),
                // ConcurrencyPatternDetector()
            )
        }
    }
}
