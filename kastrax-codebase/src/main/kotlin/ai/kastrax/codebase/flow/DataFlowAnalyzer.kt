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
 * 数据流分析器
 *
 * 分析代码的数据流，构建数据流图
 *
 * @property config 配置
 */
class DataFlowAnalyzer(
    private val config: CodeFlowAnalyzerConfig
) : CodeFlowAnalyzer {
    
    /**
     * 获取流类型
     *
     * @return 流类型
     */
    override fun getFlowType(): FlowType {
        return FlowType.DATA_FLOW
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
                logger.debug { "数据流分析只支持方法、构造函数和函数: ${element.qualifiedName}" }
                return@withContext null
            }
            
            // 创建流图
            val graph = FlowGraph(
                id = "${element.id}:data-flow",
                name = "${element.name} Data Flow",
                type = FlowType.DATA_FLOW,
                element = element
            )
            
            // 分析参数
            analyzeParameters(element, graph)
            
            // 分析局部变量
            analyzeLocalVariables(element, graph)
            
            // 分析赋值语句
            analyzeAssignments(element, graph)
            
            // 分析方法调用
            analyzeMethodCalls(element, graph)
            
            // 分析返回语句
            analyzeReturns(element, graph)
            
            // 缓存流图
            graphCache[element.id] = graph
            
            return@withContext graph
        } catch (e: Exception) {
            logger.error(e) { "数据流分析失败: ${element.qualifiedName}" }
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
     * 分析参数
     *
     * @param element 代码元素
     * @param graph 流图
     */
    private fun analyzeParameters(element: CodeElement, graph: FlowGraph) {
        // 获取参数
        val parameters = element.children.filter { it.type == CodeElementType.PARAMETER }
        
        parameters.forEach { param ->
            // 创建声明节点
            val paramType = param.metadata["type"] as? String ?: "Object"
            val declarationNode = FlowNode(
                id = "${param.id}:declaration",
                type = FlowNodeType.STATEMENT,
                element = param,
                label = "$paramType ${param.name}"
            )
            graph.addNode(declarationNode)
        }
    }
    
    /**
     * 分析局部变量
     *
     * @param element 代码元素
     * @param graph 流图
     */
    private fun analyzeLocalVariables(element: CodeElement, graph: FlowGraph) {
        // 获取局部变量信息
        val localVariables = element.metadata["localVariables"] as? List<Map<String, String>> ?: emptyList()
        
        localVariables.forEach { varInfo ->
            val name = varInfo["name"] ?: "unknown"
            val type = varInfo["type"] ?: "Object"
            val initialValue = varInfo["initialValue"]
            
            // 创建声明节点
            val declarationNode = FlowNode(
                id = "${element.id}:var:$name",
                type = FlowNodeType.STATEMENT,
                element = element,
                label = if (initialValue != null) "$type $name = $initialValue" else "$type $name"
            )
            graph.addNode(declarationNode)
        }
    }
    
    /**
     * 分析赋值语句
     *
     * @param element 代码元素
     * @param graph 流图
     */
    private fun analyzeAssignments(element: CodeElement, graph: FlowGraph) {
        // 获取赋值语句信息
        val assignments = element.metadata["assignments"] as? List<Map<String, String>> ?: emptyList()
        
        assignments.forEach { assignInfo ->
            val target = assignInfo["target"] ?: "unknown"
            val value = assignInfo["value"] ?: "unknown"
            
            // 创建赋值节点
            val assignmentNode = FlowNode(
                id = "${element.id}:assign:$target:${System.nanoTime()}",
                type = FlowNodeType.STATEMENT,
                element = element,
                label = "$target = $value"
            )
            graph.addNode(assignmentNode)
        }
    }
    
    /**
     * 分析方法调用
     *
     * @param element 代码元素
     * @param graph 流图
     */
    private fun analyzeMethodCalls(element: CodeElement, graph: FlowGraph) {
        // 获取方法调用信息
        val methodCalls = element.metadata["methodCalls"] as? List<Map<String, Any>> ?: emptyList()
        
        methodCalls.forEach { callInfo ->
            val target = callInfo["target"] as? String ?: "unknown"
            val callNode = FlowNode(
                id = "${element.id}:call:$target:${System.nanoTime()}",
                type = FlowNodeType.METHOD_CALL,
                element = element,
                label = target
            )
            graph.addNode(callNode)
        }
    }
    
    /**
     * 分析返回语句
     *
     * @param element 代码元素
     * @param graph 流图
     */
    private fun analyzeReturns(element: CodeElement, graph: FlowGraph) {
        // 获取返回语句信息
        val returns = element.metadata["returns"] as? List<Map<String, String>> ?: emptyList()
        
        returns.forEach { returnInfo ->
            val value = returnInfo["value"] ?: ""
            
            // 创建返回节点
            val returnNode = FlowNode(
                id = "${element.id}:return:${System.nanoTime()}",
                type = FlowNodeType.RETURN,
                element = element,
                label = if (value.isEmpty()) "return" else "return $value"
            )
            graph.addNode(returnNode)
        }
    }
    
    // 缓存
    private val graphCache = mutableMapOf<String, FlowGraph>()
}
