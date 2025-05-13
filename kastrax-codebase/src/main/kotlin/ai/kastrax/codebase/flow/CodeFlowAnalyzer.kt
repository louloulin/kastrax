package ai.kastrax.codebase.flow

import ai.kastrax.codebase.semantic.flow.CodeFlowAnalyzer
import ai.kastrax.codebase.semantic.flow.CodeFlowAnalyzerConfig
import ai.kastrax.codebase.semantic.flow.FlowEdgeType
import ai.kastrax.codebase.semantic.flow.FlowGraph
import ai.kastrax.codebase.semantic.flow.FlowNode
import ai.kastrax.codebase.semantic.flow.FlowNodeType
import ai.kastrax.codebase.semantic.flow.FlowType
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val logger = KotlinLogging.logger {}

/**
 * 代码流分析适配器
 *
 * 适配新旧代码流分析接口
 *
 * @property controlFlowAnalyzer 控制流分析器
 * @property dataFlowAnalyzer 数据流分析器
 */
class CodeFlowAnalyzerAdapter(
    private val controlFlowAnalyzer: ControlFlowAnalyzer,
    private val dataFlowAnalyzer: DataFlowAnalyzer
) {
    /**
     * 分析控制流
     *
     * @param element 代码元素
     * @return 控制流图，如果无法分析则返回null
     */
    suspend fun analyzeControlFlow(element: CodeElement): FlowGraph? {
        return controlFlowAnalyzer.analyze(element)
    }
    
    /**
     * 分析数据流
     *
     * @param element 代码元素
     * @return 数据流图，如果无法分析则返回null
     */
    suspend fun analyzeDataFlow(element: CodeElement): FlowGraph? {
        return dataFlowAnalyzer.analyze(element)
    }
    
    /**
     * 清除缓存
     */
    fun clearCache() {
        controlFlowAnalyzer.clearCache()
        dataFlowAnalyzer.clearCache()
    }
}
