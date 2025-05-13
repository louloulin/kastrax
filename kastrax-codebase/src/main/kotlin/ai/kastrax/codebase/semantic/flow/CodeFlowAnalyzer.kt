package ai.kastrax.codebase.semantic.flow

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 代码流分析器配置
 *
 * @property maxAnalysisDepth 最大分析深度
 * @property enableCaching 是否启用缓存
 * @property cacheSize 缓存大小
 * @property enableParallelAnalysis 是否启用并行分析
 * @property enableIncrementalAnalysis 是否启用增量分析
 */
data class CodeFlowAnalyzerConfig(
    val maxAnalysisDepth: Int = 10,
    val enableCaching: Boolean = true,
    val cacheSize: Int = 1000,
    val enableParallelAnalysis: Boolean = true,
    val enableIncrementalAnalysis: Boolean = true
)

/**
 * 代码流分析器接口
 *
 * 提供代码控制流和数据流分析功能
 */
interface CodeFlowAnalyzer {
    /**
     * 获取流类型
     *
     * @return 流类型
     */
    fun getFlowType(): FlowType
    
    /**
     * 分析代码元素
     *
     * @param element 代码元素
     * @return 流图
     */
    suspend fun analyze(element: CodeElement): FlowGraph?
    
    /**
     * 分析代码元素集合
     *
     * @param elements 代码元素集合
     * @return 流图集合
     */
    suspend fun analyzeAll(elements: Collection<CodeElement>): Map<CodeElement, FlowGraph>
    
    /**
     * 获取缓存的流图
     *
     * @param element 代码元素
     * @return 流图，如果缓存中不存在则返回null
     */
    fun getCachedGraph(element: CodeElement): FlowGraph?
    
    /**
     * 清除缓存
     */
    fun clearCache()
}
