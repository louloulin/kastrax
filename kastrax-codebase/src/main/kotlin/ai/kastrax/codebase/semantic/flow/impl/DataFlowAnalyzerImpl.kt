package ai.kastrax.codebase.semantic.flow.impl

import ai.kastrax.codebase.semantic.flow.*
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 数据流分析器实现
 *
 * 分析代码元素的数据流，生成数据流图
 *
 * @property config 配置
 */
class DataFlowAnalyzerImpl(
    private val config: CodeFlowAnalyzerConfig = CodeFlowAnalyzerConfig()
) : CodeFlowAnalyzer {
    private val logger = KotlinLogging.logger {}
    
    // 流图缓存
    private val graphCache = ConcurrentHashMap<String, FlowGraph>()
    
    /**
     * 分析代码元素的流图
     *
     * @param element 代码元素
     * @return 流图
     */
    override suspend fun analyzeFlow(element: CodeElement): FlowGraph = withContext(Dispatchers.Default) {
        // 默认分析数据流
        return@withContext analyzeFlow(element, FlowType.DATA_FLOW)
    }
    
    /**
     * 分析指定类型的代码流
     *
     * @param element 代码元素
     * @param type 流类型
     * @return 流图
     */
    override suspend fun analyzeFlow(element: CodeElement, type: FlowType): FlowGraph = withContext(Dispatchers.Default) {
        // 检查是否支持该元素和流类型
        if (!supportsElement(element)) {
            logger.warn { "不支持的元素类型: ${element.type}" }
            return@withContext createEmptyGraph(element, type)
        }
        
        if (!supportsFlowType(type)) {
            logger.warn { "不支持的流类型: $type" }
            return@withContext createEmptyGraph(element, type)
        }
        
        // 检查缓存
        val cacheKey = "${element.id}:${type.name}"
        if (config.enableCaching && graphCache.containsKey(cacheKey)) {
            return@withContext graphCache[cacheKey]!!
        }
        
        logger.info { "开始分析数据流: ${element.qualifiedName}" }
        
        // 创建流图
        val graph = FlowGraph(
            id = UUID.randomUUID().toString(),
            name = "${element.name} ${type.name}",
            type = type
        )
        
        // 根据元素类型分析
        when (element.type) {
            CodeElementType.METHOD, CodeElementType.FUNCTION -> {
                // 检查方法大小
                val methodSize = element.location.endLine - element.location.startLine + 1
                if (methodSize > config.maxMethodSize) {
                    logger.warn { "方法过大，跳过分析: ${element.qualifiedName} (${methodSize} 行)" }
                    return@withContext createEmptyGraph(element, type)
                }
                
                analyzeMethodDataFlow(element, graph)
            }
            CodeElementType.CLASS, CodeElementType.INTERFACE -> {
                analyzeClassDataFlow(element, graph)
            }
            else -> {
                logger.warn { "不支持的元素类型: ${element.type}" }
                return@withContext createEmptyGraph(element, type)
            }
        }
        
        logger.info { "数据流分析完成: ${element.qualifiedName}, 节点数: ${graph.nodes.size}, 边数: ${graph.edges.size}" }
        
        // 缓存流图
        if (config.enableCaching) {
            graphCache[cacheKey] = graph
        }
        
        return@withContext graph
    }
    
    /**
     * 分析方法数据流
     *
     * @param element 方法元素
     * @param graph 流图
     */
    private suspend fun analyzeMethodDataFlow(element: CodeElement, graph: FlowGraph) {
        // 创建入口和出口节点
        val entryNode = FlowNode(
            id = UUID.randomUUID().toString(),
            type = FlowNodeType.ENTRY,
            element = element,
            metadata = mutableMapOf("label" to "${element.name} Entry")
        )
        
        val exitNode = FlowNode(
            id = UUID.randomUUID().toString(),
            type = FlowNodeType.EXIT,
            element = element,
            metadata = mutableMapOf("label" to "${element.name} Exit")
        )
        
        graph.addNode(entryNode)
        graph.addNode(exitNode)
        graph.setEntryNode(entryNode.id)
        graph.addExitNode(exitNode.id)
        
        // 分析方法参数
        val parameterElements = element.children.filter { it.type == CodeElementType.PARAMETER }
        val parameterNodes = mutableMapOf<String, FlowNode>() // 参数名到节点的映射
        
        for (parameter in parameterElements) {
            val paramNode = FlowNode(
                id = UUID.randomUUID().toString(),
                type = FlowNodeType.DECLARATION,
                element = parameter,
                metadata = mutableMapOf("label" to "Parameter: ${parameter.name}")
            )
            graph.addNode(paramNode)
            parameterNodes[parameter.name] = paramNode
            
            // 连接入口到参数节点
            val edge = FlowEdge(
                id = UUID.randomUUID().toString(),
                sourceId = entryNode.id,
                targetId = paramNode.id,
                type = FlowEdgeType.DATA_DEPENDENCY
            )
            graph.addEdge(edge)
        }
        
        // 分析方法体
        val bodyElements = element.children.filter { 
            it.type != CodeElementType.PARAMETER && it.type != CodeElementType.COMMENT 
        }
        
        if (bodyElements.isEmpty()) {
            // 如果方法体为空，直接连接参数到出口
            for (paramNode in parameterNodes.values) {
                val edge = FlowEdge(
                    id = UUID.randomUUID().toString(),
                    sourceId = paramNode.id,
                    targetId = exitNode.id,
                    type = FlowEdgeType.DATA_DEPENDENCY,
                    metadata = mutableMapOf("label" to "Empty Method")
                )
                graph.addEdge(edge)
            }
            return
        }
        
        // 变量定义和使用的跟踪
        val variableDefMap = mutableMapOf<String, FlowNode>() // 变量名到定义节点的映射
        
        // 添加参数到变量定义映射
        variableDefMap.putAll(parameterNodes)
        
        // 分析语句
        for (statement in bodyElements) {
            analyzeStatementDataFlow(statement, graph, variableDefMap, exitNode.id)
        }
        
        // 连接最后的变量使用到出口
        val returnVars = bodyElements.lastOrNull()?.metadata?.get("returnVars") as? List<String>
        if (returnVars != null && returnVars.isNotEmpty()) {
            for (varName in returnVars) {
                val varDefNode = variableDefMap[varName]
                if (varDefNode != null) {
                    val edge = FlowEdge(
                        id = UUID.randomUUID().toString(),
                        sourceId = varDefNode.id,
                        targetId = exitNode.id,
                        type = FlowEdgeType.DATA_DEPENDENCY,
                        metadata = mutableMapOf("label" to "Return: $varName")
                    )
                    graph.addEdge(edge)
                }
            }
        } else {
            // 如果没有明确的返回变量，连接所有变量定义到出口
            for (varDefNode in variableDefMap.values) {
                val edge = FlowEdge(
                    id = UUID.randomUUID().toString(),
                    sourceId = varDefNode.id,
                    targetId = exitNode.id,
                    type = FlowEdgeType.DATA_DEPENDENCY,
                    metadata = mutableMapOf("label" to "Implicit Return")
                )
                graph.addEdge(edge)
            }
        }
    }
    
    /**
     * 分析语句数据流
     *
     * @param statement 语句元素
     * @param graph 流图
     * @param variableDefMap 变量定义映射
     * @param exitNodeId 出口节点ID
     */
    private fun analyzeStatementDataFlow(
        statement: CodeElement,
        graph: FlowGraph,
        variableDefMap: MutableMap<String, FlowNode>,
        exitNodeId: String
    ) {
        // 分析语句中的变量定义
        val variableDefs = statement.metadata["variableDefs"] as? List<String> ?: emptyList()
        for (varName in variableDefs) {
            val varDefNode = FlowNode(
                id = UUID.randomUUID().toString(),
                type = FlowNodeType.DECLARATION,
                element = statement,
                metadata = mutableMapOf("label" to "Define: $varName")
            )
            graph.addNode(varDefNode)
            variableDefMap[varName] = varDefNode
        }
        
        // 分析语句中的变量使用
        val variableUses = statement.metadata["variableUses"] as? List<String> ?: emptyList()
        for (varName in variableUses) {
            val varUseNode = FlowNode(
                id = UUID.randomUUID().toString(),
                type = FlowNodeType.REFERENCE,
                element = statement,
                metadata = mutableMapOf("label" to "Use: $varName")
            )
            graph.addNode(varUseNode)
            
            // 连接变量定义到变量使用
            val varDefNode = variableDefMap[varName]
            if (varDefNode != null) {
                val edge = FlowEdge(
                    id = UUID.randomUUID().toString(),
                    sourceId = varDefNode.id,
                    targetId = varUseNode.id,
                    type = FlowEdgeType.DATA_DEPENDENCY,
                    metadata = mutableMapOf("label" to "Def-Use: $varName")
                )
                graph.addEdge(edge)
            }
        }
        
        // 分析方法调用
        if (statement.metadata["isMethodCall"] == true) {
            val methodCallNode = FlowNode(
                id = UUID.randomUUID().toString(),
                type = FlowNodeType.CALL,
                element = statement,
                metadata = mutableMapOf("label" to "Call: ${statement.name}")
            )
            graph.addNode(methodCallNode)
            
            // 连接参数到方法调用
            val callParams = statement.metadata["callParams"] as? List<String> ?: emptyList()
            for (paramName in callParams) {
                val paramDefNode = variableDefMap[paramName]
                if (paramDefNode != null) {
                    val edge = FlowEdge(
                        id = UUID.randomUUID().toString(),
                        sourceId = paramDefNode.id,
                        targetId = methodCallNode.id,
                        type = FlowEdgeType.DATA_DEPENDENCY,
                        metadata = mutableMapOf("label" to "Param: $paramName")
                    )
                    graph.addEdge(edge)
                }
            }
            
            // 如果方法调用有返回值，创建返回值节点
            val returnVar = statement.metadata["returnVar"] as? String
            if (returnVar != null) {
                val returnVarNode = FlowNode(
                    id = UUID.randomUUID().toString(),
                    type = FlowNodeType.DECLARATION,
                    element = statement,
                    metadata = mutableMapOf("label" to "Return Value: $returnVar")
                )
                graph.addNode(returnVarNode)
                variableDefMap[returnVar] = returnVarNode
                
                // 连接方法调用到返回值
                val edge = FlowEdge(
                    id = UUID.randomUUID().toString(),
                    sourceId = methodCallNode.id,
                    targetId = returnVarNode.id,
                    type = FlowEdgeType.DATA_DEPENDENCY,
                    metadata = mutableMapOf("label" to "Return Value")
                )
                graph.addEdge(edge)
            }
        }
        
        // 分析返回语句
        if (statement.metadata["isReturn"] == true) {
            val returnNode = FlowNode(
                id = UUID.randomUUID().toString(),
                type = FlowNodeType.RETURN,
                element = statement,
                metadata = mutableMapOf("label" to "Return")
            )
            graph.addNode(returnNode)
            
            // 连接返回值到返回节点
            val returnVars = statement.metadata["returnVars"] as? List<String> ?: emptyList()
            for (varName in returnVars) {
                val varDefNode = variableDefMap[varName]
                if (varDefNode != null) {
                    val edge = FlowEdge(
                        id = UUID.randomUUID().toString(),
                        sourceId = varDefNode.id,
                        targetId = returnNode.id,
                        type = FlowEdgeType.DATA_DEPENDENCY,
                        metadata = mutableMapOf("label" to "Return: $varName")
                    )
                    graph.addEdge(edge)
                }
            }
            
            // 连接返回节点到出口
            val edge = FlowEdge(
                id = UUID.randomUUID().toString(),
                sourceId = returnNode.id,
                targetId = exitNodeId,
                type = FlowEdgeType.DATA_DEPENDENCY,
                metadata = mutableMapOf("label" to "Return")
            )
            graph.addEdge(edge)
        }
    }
    
    /**
     * 分析类数据流
     *
     * @param element 类元素
     * @param graph 流图
     */
    private suspend fun analyzeClassDataFlow(element: CodeElement, graph: FlowGraph) {
        // 创建入口节点
        val entryNodeId = UUID.randomUUID().toString()
        val entryNode = FlowNode(
            id = entryNodeId,
            type = FlowNodeType.ENTRY,
            element = element,
            metadata = mutableMapOf("label" to "Class: ${element.name}")
        )
        graph.addNode(entryNode)
        graph.setEntryNode(entryNodeId)
        
        // 创建出口节点
        val exitNodeId = UUID.randomUUID().toString()
        val exitNode = FlowNode(
            id = exitNodeId,
            type = FlowNodeType.EXIT,
            element = element,
            metadata = mutableMapOf("label" to "End Class: ${element.name}")
        )
        graph.addNode(exitNode)
        graph.addExitNode(exitNodeId)
        
        // 分析类的字段
        val fields = element.children.filter { it.type == CodeElementType.FIELD }
        val fieldNodes = mutableMapOf<String, FlowNode>() // 字段名到节点的映射
        
        for (field in fields) {
            val fieldNode = FlowNode(
                id = UUID.randomUUID().toString(),
                type = FlowNodeType.DECLARATION,
                element = field,
                metadata = mutableMapOf("label" to "Field: ${field.name}")
            )
            graph.addNode(fieldNode)
            fieldNodes[field.name] = fieldNode
            
            // 连接入口到字段节点
            val edge = FlowEdge(
                id = UUID.randomUUID().toString(),
                sourceId = entryNodeId,
                targetId = fieldNode.id,
                type = FlowEdgeType.DATA_DEPENDENCY
            )
            graph.addEdge(edge)
        }
        
        // 分析类的方法
        val methods = element.children.filter { 
            it.type == CodeElementType.METHOD || it.type == CodeElementType.CONSTRUCTOR || it.type == CodeElementType.FUNCTION 
        }
        
        if (methods.isEmpty()) {
            // 如果类没有方法，直接连接字段到出口
            for (fieldNode in fieldNodes.values) {
                val edge = FlowEdge(
                    id = UUID.randomUUID().toString(),
                    sourceId = fieldNode.id,
                    targetId = exitNodeId,
                    type = FlowEdgeType.DATA_DEPENDENCY,
                    metadata = mutableMapOf("label" to "No methods")
                )
                graph.addEdge(edge)
            }
            return
        }
        
        // 并行或串行分析方法
        if (config.enableParallelAnalysis) {
            // 并行分析方法
            coroutineScope {
                methods.chunked(config.maxConcurrentTasks).forEach { chunk ->
                    chunk.map { method ->
                        async {
                            analyzeMethodInClassDataFlow(method, graph, entryNodeId, exitNodeId, fieldNodes)
                        }
                    }.awaitAll()
                }
            }
        } else {
            // 串行分析方法
            methods.forEach { method ->
                analyzeMethodInClassDataFlow(method, graph, entryNodeId, exitNodeId, fieldNodes)
            }
        }
    }
    
    /**
     * 在类中分析方法数据流
     *
     * @param method 方法元素
     * @param graph 流图
     * @param entryNodeId 入口节点ID
     * @param exitNodeId 出口节点ID
     * @param fieldNodes 字段节点映射
     */
    private fun analyzeMethodInClassDataFlow(
        method: CodeElement,
        graph: FlowGraph,
        entryNodeId: String,
        exitNodeId: String,
        fieldNodes: Map<String, FlowNode>
    ) {
        val methodNodeId = UUID.randomUUID().toString()
        val methodNode = FlowNode(
            id = methodNodeId,
            type = FlowNodeType.CALL,
            element = method,
            metadata = mutableMapOf("label" to "Method: ${method.name}")
        )
        graph.addNode(methodNode)
        
        // 连接入口和方法节点
        val entryEdge = FlowEdge(
            id = UUID.randomUUID().toString(),
            sourceId = entryNodeId,
            targetId = methodNodeId,
            type = FlowEdgeType.CONTROL_DEPENDENCY
        )
        graph.addEdge(entryEdge)
        
        // 分析方法使用的字段
        val usedFields = method.metadata["usedFields"] as? List<String> ?: emptyList()
        for (fieldName in usedFields) {
            val fieldNode = fieldNodes[fieldName]
            if (fieldNode != null) {
                val edge = FlowEdge(
                    id = UUID.randomUUID().toString(),
                    sourceId = fieldNode.id,
                    targetId = methodNodeId,
                    type = FlowEdgeType.DATA_DEPENDENCY,
                    metadata = mutableMapOf("label" to "Uses Field: $fieldName")
                )
                graph.addEdge(edge)
            }
        }
        
        // 分析方法修改的字段
        val modifiedFields = method.metadata["modifiedFields"] as? List<String> ?: emptyList()
        for (fieldName in modifiedFields) {
            val fieldNode = fieldNodes[fieldName]
            if (fieldNode != null) {
                val edge = FlowEdge(
                    id = UUID.randomUUID().toString(),
                    sourceId = methodNodeId,
                    targetId = fieldNode.id,
                    type = FlowEdgeType.DATA_DEPENDENCY,
                    metadata = mutableMapOf("label" to "Modifies Field: $fieldName")
                )
                graph.addEdge(edge)
            }
        }
        
        // 连接方法节点和出口节点
        val exitEdge = FlowEdge(
            id = UUID.randomUUID().toString(),
            sourceId = methodNodeId,
            targetId = exitNodeId,
            type = FlowEdgeType.CONTROL_DEPENDENCY
        )
        graph.addEdge(exitEdge)
    }
    
    /**
     * 创建空图
     *
     * @param element 代码元素
     * @param type 流类型
     * @return 流图
     */
    private fun createEmptyGraph(element: CodeElement, type: FlowType): FlowGraph {
        val graph = FlowGraph(
            id = UUID.randomUUID().toString(),
            name = "${element.name} ${type.name} (Empty)",
            type = type
        )
        
        // 创建入口节点
        val entryNodeId = UUID.randomUUID().toString()
        val entryNode = FlowNode(
            id = entryNodeId,
            type = FlowNodeType.ENTRY,
            element = element,
            metadata = mutableMapOf("label" to "Entry: ${element.name}")
        )
        graph.addNode(entryNode)
        graph.setEntryNode(entryNodeId)
        
        // 创建出口节点
        val exitNodeId = UUID.randomUUID().toString()
        val exitNode = FlowNode(
            id = exitNodeId,
            type = FlowNodeType.EXIT,
            element = element,
            metadata = mutableMapOf("label" to "Exit: ${element.name}")
        )
        graph.addNode(exitNode)
        graph.addExitNode(exitNodeId)
        
        // 连接入口和出口
        val edge = FlowEdge(
            id = UUID.randomUUID().toString(),
            sourceId = entryNodeId,
            targetId = exitNodeId,
            type = FlowEdgeType.DATA_DEPENDENCY,
            metadata = mutableMapOf("label" to "Empty")
        )
        graph.addEdge(edge)
        
        return graph
    }
    
    /**
     * 获取支持的代码元素类型
     *
     * @return 支持的代码元素类型集合
     */
    override fun getSupportedElementTypes(): Set<CodeElementType> {
        return setOf(
            CodeElementType.METHOD,
            CodeElementType.FUNCTION,
            CodeElementType.CLASS,
            CodeElementType.INTERFACE
        )
    }
    
    /**
     * 获取支持的流类型
     *
     * @return 支持的流类型集合
     */
    override fun getSupportedFlowTypes(): Set<FlowType> {
        return setOf(FlowType.DATA_FLOW)
    }
    
    /**
     * 清除缓存
     */
    override fun clearCache() {
        graphCache.clear()
    }
}
