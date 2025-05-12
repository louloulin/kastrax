package ai.kastrax.codebase.semantic.pattern.detector

import ai.kastrax.codebase.semantic.flow.FlowGraph
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.pattern.PatternCategory
import ai.kastrax.codebase.semantic.pattern.PatternMatch

/**
 * 模式检测器接口
 *
 * 定义模式检测器的通用接口，用于检测代码中的特定模式
 */
interface PatternDetector {
    /**
     * 检测代码元素中的模式
     *
     * @param element 代码元素
     * @return 模式匹配结果列表
     */
    suspend fun detectPatterns(element: CodeElement): List<PatternMatch>
    
    /**
     * 获取检测器支持的模式类别
     *
     * @return 模式类别
     */
    fun getCategory(): PatternCategory
    
    /**
     * 获取检测器支持的模式列表
     *
     * @return 模式ID和名称的对列表
     */
    fun getSupportedPatterns(): List<Pair<String, String>>
}

/**
 * 流图模式检测器接口
 *
 * 定义基于流图的模式检测器接口，用于从流图中检测特定模式
 */
interface FlowGraphPatternDetector : PatternDetector {
    /**
     * 从流图中检测模式
     *
     * @param flowGraph 流图
     * @return 模式匹配结果列表
     */
    suspend fun detectPatternsFromFlowGraph(flowGraph: FlowGraph): List<PatternMatch>
}

/**
 * 抽象模式检测器
 *
 * 提供模式检测器的基本实现
 *
 * @property category 模式类别
 * @property supportedPatterns 支持的模式列表
 */
abstract class AbstractPatternDetector(
    private val category: PatternCategory,
    private val supportedPatterns: List<Pair<String, String>>
) : PatternDetector {
    
    override fun getCategory(): PatternCategory = category
    
    override fun getSupportedPatterns(): List<Pair<String, String>> = supportedPatterns
}

/**
 * 抽象流图模式检测器
 *
 * 提供基于流图的模式检测器的基本实现
 *
 * @property category 模式类别
 * @property supportedPatterns 支持的模式列表
 */
abstract class AbstractFlowGraphPatternDetector(
    private val category: PatternCategory,
    private val supportedPatterns: List<Pair<String, String>>
) : FlowGraphPatternDetector {
    
    override fun getCategory(): PatternCategory = category
    
    override fun getSupportedPatterns(): List<Pair<String, String>> = supportedPatterns
}
