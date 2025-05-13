package ai.kastrax.codebase.flow

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
    config: CodeFlowAnalyzerConfig
) : AbstractCodeFlowAnalyzer(config) {
    
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
            val graph = createFlowGraph(element)
            
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
            cacheGraph(element, graph)
            
            return@withContext graph
        } catch (e: Exception) {
            logger.error(e) { "数据流分析失败: ${element.qualifiedName}" }
            return@withContext null
        }
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
            val declarationNode = createDeclarationNode(param, param.name, paramType)
            graph.addNode(declarationNode)
            
            // 如果有默认值，则创建赋值节点
            val defaultValue = param.metadata["defaultValue"] as? String
            if (defaultValue != null) {
                val assignmentNode = createAssignmentNode(param, param.name, defaultValue)
                graph.addNode(assignmentNode)
                
                // 连接声明节点和赋值节点
                graph.addEdge(declarationNode, assignmentNode, FlowEdgeType.DATA_FLOW)
            }
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
            val declarationNode = createDeclarationNode(element, name, type, initialValue)
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
            val assignmentNode = createAssignmentNode(element, target, value)
            graph.addNode(assignmentNode)
            
            // 如果值是一个变量引用，则创建引用节点并连接
            if (!value.startsWith("\"") && !value.matches(Regex("\\d+"))) {
                val referenceNode = createReferenceNode(element, value)
                graph.addNode(referenceNode)
                
                // 连接引用节点和赋值节点
                graph.addEdge(referenceNode, assignmentNode, FlowEdgeType.DATA_FLOW)
            }
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
            val callNode = createCallNode(element, target)
            graph.addNode(callNode)
            
            // 获取参数
            val args = callInfo["arguments"] as? List<String> ?: emptyList()
            
            // 为每个参数创建引用节点并连接到调用节点
            args.forEach { arg ->
                if (!arg.startsWith("\"") && !arg.matches(Regex("\\d+"))) {
                    val referenceNode = createReferenceNode(element, arg)
                    graph.addNode(referenceNode)
                    
                    // 连接引用节点和调用节点
                    graph.addEdge(referenceNode, callNode, FlowEdgeType.DATA_FLOW)
                }
            }
            
            // 如果方法调用有赋值目标，则创建赋值节点并连接
            val assignTarget = callInfo["assignTarget"] as? String
            if (assignTarget != null) {
                val assignmentNode = createAssignmentNode(element, assignTarget, "$target()")
                graph.addNode(assignmentNode)
                
                // 连接调用节点和赋值节点
                graph.addEdge(callNode, assignmentNode, FlowEdgeType.DATA_FLOW)
            }
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
            val returnNode = createReturnNode(element, value)
            graph.addNode(returnNode)
            
            // 如果返回值是一个变量引用，则创建引用节点并连接
            if (value.isNotEmpty() && !value.startsWith("\"") && !value.matches(Regex("\\d+"))) {
                val referenceNode = createReferenceNode(element, value)
                graph.addNode(referenceNode)
                
                // 连接引用节点和返回节点
                graph.addEdge(referenceNode, returnNode, FlowEdgeType.DATA_FLOW)
            }
        }
    }
}
