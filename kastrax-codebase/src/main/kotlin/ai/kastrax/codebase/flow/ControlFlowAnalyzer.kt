package ai.kastrax.codebase.flow

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
    config: CodeFlowAnalyzerConfig
) : AbstractCodeFlowAnalyzer(config) {
    
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
            val graph = createFlowGraph(element)
            
            // 创建入口和出口节点
            val entryNode = createEntryNode(element)
            val exitNode = createExitNode(element)
            
            // 设置入口节点
            graph.setEntryNode(entryNode)
            
            // 添加出口节点
            graph.addExitNode(exitNode)
            
            // 分析方法体
            analyzeMethodBody(element, graph, entryNode, exitNode)
            
            // 缓存流图
            cacheGraph(element, graph)
            
            return@withContext graph
        } catch (e: Exception) {
            logger.error(e) { "控制流分析失败: ${element.qualifiedName}" }
            return@withContext null
        }
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
        
        // 分析方法调用
        analyzeMethodCalls(element, graph, statementNode)
        
        // 分析条件语句
        analyzeConditions(element, graph, statementNode)
        
        // 分析循环语句
        analyzeLoops(element, graph, statementNode)
        
        // 分析异常处理
        analyzeExceptionHandling(element, graph, statementNode, exitNode)
    }
    
    /**
     * 分析方法调用
     *
     * @param element 代码元素
     * @param graph 流图
     * @param parentNode 父节点
     */
    private fun analyzeMethodCalls(element: CodeElement, graph: FlowGraph, parentNode: FlowNode) {
        // 获取方法调用信息
        val methodCalls = element.metadata["methodCalls"] as? List<Map<String, String>> ?: emptyList()
        
        methodCalls.forEach { callInfo ->
            val target = callInfo["target"] ?: "unknown"
            val callNode = createCallNode(element, target)
            graph.addNode(callNode)
            
            // 连接父节点和调用节点
            graph.addEdge(parentNode, callNode, FlowEdgeType.CALL)
        }
    }
    
    /**
     * 分析条件语句
     *
     * @param element 代码元素
     * @param graph 流图
     * @param parentNode 父节点
     */
    private fun analyzeConditions(element: CodeElement, graph: FlowGraph, parentNode: FlowNode) {
        // 获取条件语句信息
        val conditions = element.metadata["conditions"] as? List<Map<String, Any>> ?: emptyList()
        
        conditions.forEach { conditionInfo ->
            val condition = conditionInfo["condition"] as? String ?: "unknown"
            val conditionNode = createConditionNode(element, condition)
            graph.addNode(conditionNode)
            
            // 连接父节点和条件节点
            graph.addEdge(parentNode, conditionNode, FlowEdgeType.SEQUENTIAL)
            
            // 创建真分支节点
            val trueBranchNode = createStatementNode(element, "True Branch")
            graph.addNode(trueBranchNode)
            
            // 连接条件节点和真分支节点
            graph.addEdge(conditionNode, trueBranchNode, FlowEdgeType.CONDITIONAL, "true")
            
            // 创建假分支节点
            val falseBranchNode = createStatementNode(element, "False Branch")
            graph.addNode(falseBranchNode)
            
            // 连接条件节点和假分支节点
            graph.addEdge(conditionNode, falseBranchNode, FlowEdgeType.CONDITIONAL, "false")
        }
    }
    
    /**
     * 分析循环语句
     *
     * @param element 代码元素
     * @param graph 流图
     * @param parentNode 父节点
     */
    private fun analyzeLoops(element: CodeElement, graph: FlowGraph, parentNode: FlowNode) {
        // 获取循环语句信息
        val loops = element.metadata["loops"] as? List<Map<String, Any>> ?: emptyList()
        
        loops.forEach { loopInfo ->
            val condition = loopInfo["condition"] as? String ?: "unknown"
            val loopNode = createLoopNode(element, condition)
            graph.addNode(loopNode)
            
            // 连接父节点和循环节点
            graph.addEdge(parentNode, loopNode, FlowEdgeType.SEQUENTIAL)
            
            // 创建循环体节点
            val loopBodyNode = createStatementNode(element, "Loop Body")
            graph.addNode(loopBodyNode)
            
            // 连接循环节点和循环体节点
            graph.addEdge(loopNode, loopBodyNode, FlowEdgeType.CONDITIONAL, "true")
            
            // 连接循环体节点和循环节点（回边）
            graph.addEdge(loopBodyNode, loopNode, FlowEdgeType.LOOP_BACK)
            
            // 创建循环出口节点
            val loopExitNode = createStatementNode(element, "Loop Exit")
            graph.addNode(loopExitNode)
            
            // 连接循环节点和循环出口节点
            graph.addEdge(loopNode, loopExitNode, FlowEdgeType.CONDITIONAL, "false")
        }
    }
    
    /**
     * 分析异常处理
     *
     * @param element 代码元素
     * @param graph 流图
     * @param parentNode 父节点
     * @param exitNode 出口节点
     */
    private fun analyzeExceptionHandling(element: CodeElement, graph: FlowGraph, parentNode: FlowNode, exitNode: FlowNode) {
        // 获取异常处理信息
        val exceptionHandlers = element.metadata["exceptionHandlers"] as? List<Map<String, Any>> ?: emptyList()
        
        exceptionHandlers.forEach { handlerInfo ->
            val exceptionType = handlerInfo["exceptionType"] as? String ?: "Exception"
            
            // 创建 try 节点
            val tryNode = createStatementNode(element, "try")
            graph.addNode(tryNode)
            
            // 连接父节点和 try 节点
            graph.addEdge(parentNode, tryNode, FlowEdgeType.SEQUENTIAL)
            
            // 创建 catch 节点
            val catchNode = createCatchNode(element, exceptionType)
            graph.addNode(catchNode)
            
            // 连接 try 节点和 catch 节点（异常边）
            graph.addEdge(tryNode, catchNode, FlowEdgeType.EXCEPTION)
            
            // 连接 catch 节点和出口节点
            graph.addEdge(catchNode, exitNode, FlowEdgeType.SEQUENTIAL)
        }
    }
}
