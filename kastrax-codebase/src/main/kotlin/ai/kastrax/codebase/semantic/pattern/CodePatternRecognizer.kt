package ai.kastrax.codebase.semantic.pattern

import ai.kastrax.codebase.semantic.flow.FlowGraph
import ai.kastrax.codebase.semantic.model.CodeElement
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 代码模式识别器配置
 *
 * @property minConfidence 最小置信度
 * @property maxResults 最大结果数
 * @property includeLibraries 是否包含库代码
 * @property patternCategories 要识别的模式类别
 */
data class CodePatternRecognizerConfig(
    val minConfidence: Double = 0.7,
    val maxResults: Int = 10,
    val includeLibraries: Boolean = false,
    val patternCategories: Set<PatternCategory> = PatternCategory.values().toSet()
)

/**
 * 模式类别
 */
enum class PatternCategory {
    DESIGN_PATTERN,    // 设计模式
    ARCHITECTURE,      // 架构模式
    ANTI_PATTERN,      // 反模式
    CODE_SMELL,        // 代码气味
    PERFORMANCE,       // 性能模式
    SECURITY,          // 安全模式
    CONCURRENCY,       // 并发模式
    FUNCTIONAL,        // 函数式模式
    LANGUAGE_SPECIFIC  // 语言特定模式
}

/**
 * 模式匹配结果
 *
 * @property patternId 模式ID
 * @property patternName 模式名称
 * @property category 模式类别
 * @property confidence 置信度
 * @property elements 匹配的代码元素
 * @property description 描述
 * @property suggestion 建议
 * @property metadata 元数据
 */
data class PatternMatch(
    val patternId: String,
    val patternName: String,
    val category: PatternCategory,
    val confidence: Double,
    val elements: List<CodeElement>,
    val description: String,
    val suggestion: String? = null,
    val metadata: Map<String, Any> = mapOf()
)

/**
 * 代码模式识别器接口
 *
 * 定义代码模式识别器的通用接口，用于识别代码中的各种模式
 */
interface CodePatternRecognizer {
    /**
     * 识别代码元素中的模式
     *
     * @param element 代码元素
     * @return 模式匹配结果列表
     */
    suspend fun recognizePatterns(element: CodeElement): List<PatternMatch>
    
    /**
     * 基于流图识别模式
     *
     * @param flowGraph 流图
     * @return 模式匹配结果列表
     */
    suspend fun recognizePatternsFromFlowGraph(flowGraph: FlowGraph): List<PatternMatch>
    
    /**
     * 获取支持的模式类别
     *
     * @return 支持的模式类别集合
     */
    fun getSupportedCategories(): Set<PatternCategory>
    
    /**
     * 获取所有已知模式
     *
     * @return 模式ID到名称的映射
     */
    fun getKnownPatterns(): Map<String, String>
}
