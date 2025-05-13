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
 * 控制流分析器
 *
 * 分析代码的控制流，构建控制流图
 *
 * @property config 配置
 */
class ControlFlowAnalyzer(
    private val config: CodeFlowAnalyzerConfig
) : CodeFlowAnalyzer {
    
    /**
     * 获取流类型
     *
     * @return 流类型
     */
    override fun getFlowType(): FlowType {
        return FlowType.CONTROL_FLOW
    }
    
    /**
     * 分析代码元素
     *
     * @param element 代码元素
     * @return 流图
     */
    override suspend fun analyze(element: CodeElement): FlowGraph? = withContext(Dispatchers.Default) {
        try {
            // 检查缓存
            val cachedGraph = getCachedGraph(element)
            if (cachedGraph != null) {
                return@withContext cachedGraph
            }
            
            // 只分析方法、构造函数和函数
            if (element.type != CodeElementType.METHOD && 
                element.type != CodeElementType.CONSTRUCTOR && 
                element.type != CodeElementType.FUNCTION) {
                logger.debug { "控制流分析只支持方法、构造函数和函数: ${element.qualifiedName}" }
                return@withContext null
            }
            
            // 创建流图
            val graph = FlowGraph(
                id = "${element.id}:control-flow",
                name = "${element.name} Control Flow",
                type = FlowType.CONTROL_FLOW,
                element = element
            )
            
            // 创建入口和出口节点
            val entryNode = FlowNode(
                id = "${element.id}:entry",
                type = FlowNodeType.ENTRY,
                element = element,
                label = "ENTRY"
            )
            
            val exitNode = FlowNode(
                id = "${element.id}:exit",
                type = FlowNodeType.EXIT,
                element = element,
                label = "EXIT"
            )
            
            // 设置入口节点
            graph.addNode(entryNode)
            graph.entryNode = entryNode
            
            // 添加出口节点
            graph.addNode(exitNode)
            graph.exitNodes.add(exitNode)
            
            // 分析方法体
            analyzeMethodBody(element, graph, entryNode, exitNode)
            
            // 缓存流图
            graphCache[element.id] = graph
            
            return@withContext graph
        } catch (e: Exception) {
            logger.error(e) { "控制流分析失败: ${element.qualifiedName}" }
            return@withContext null
        }
    }
    
    /**
     * 分析代码元素集合
     *
     * @param elements 代码元素集合
     * @return 流图集合
     */
    override suspend fun analyzeAll(elements: Collection<CodeElement>): Map<CodeElement, FlowGraph> {
        val result = mutableMapOf<CodeElement, FlowGraph>()
        
        for (element in elements) {
            val graph = analyze(element)
            if (graph != null) {
                result[element] = graph
            }
        }
        
        return result
    }
    
    /**
     * 获取缓存的流图
     *
     * @param element 代码元素
     * @return 流图，如果缓存中不存在则返回null
     */
    override fun getCachedGraph(element: CodeElement): FlowGraph? {
        return graphCache[element.id]
    }
    
    /**
     * 清除缓存
     */
    override fun clearCache() {
        graphCache.clear()
    }
    
    /**
     * 分析方法体
     *
     * @param element 代码元素
     * @param graph 流图
     * @param entryNode 入口节点
     * @param exitNode 出口节点
     */
    private fun analyzeMethodBody(element: CodeElement, graph: FlowGraph, entryNode: FlowNode, exitNode: FlowNode) {
        // 获取方法体
        val methodBody = element.metadata["body"] as? String ?: ""
        if (methodBody.isBlank()) {
            // 如果方法体为空，则直接连接入口和出口节点
            graph.addEdge(entryNode, exitNode, FlowEdgeType.SEQUENTIAL)
            return
        }
        
        // 创建语句节点
        val statementNode = createStatementNode(element, "Method Body")
        graph.addNode(statementNode)
        
        // 连接入口节点和语句节点
        graph.addEdge(entryNode, statementNode, FlowEdgeType.SEQUENTIAL)
        
        // 连接语句节点和出口节点
        graph.addEdge(statementNode, exitNode, FlowEdgeType.SEQUENTIAL)
    }
    
    /**
     * 创建语句节点
     *
     * @param element 代码元素
     * @param label 标签
     * @return 语句节点
     */
    private fun createStatementNode(element: CodeElement, label: String = ""): FlowNode {
        return FlowNode(
            id = "${element.id}:statement:${System.nanoTime()}",
            type = FlowNodeType.STATEMENT,
            element = element,
            label = label.ifEmpty { element.name }
        )
    }
    
    // 缓存
    private val graphCache = mutableMapOf<String, FlowGraph>()
}
