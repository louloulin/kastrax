package ai.kastrax.codebase.semantic.flow.impl

import ai.kastrax.codebase.semantic.flow.*
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

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

    override suspend fun analyzeFlow(element: CodeElement): FlowGraph = withContext(Dispatchers.Default) {
        logger.info { "开始分析数据流: ${element.qualifiedName}" }

        if (!supportsElement(element)) {
            logger.warn { "不支持的代码元素类型: ${element.type}" }
            return@withContext createEmptyFlowGraph(element)
        }

        // 创建流图
        val flowGraph = FlowGraph(
            id = UUID.randomUUID().toString(),
            name = "${element.name} 数据流图",
            type = FlowType.DATA_FLOW,
            metadata = mutableMapOf(
                "elementId" to element.id,
                "elementType" to element.type.name,
                "language" to element.language
            )
        )

        try {
            when (element.type) {
                CodeElementType.METHOD, CodeElementType.FUNCTION -> analyzeMethodDataFlow(element, flowGraph)
                CodeElementType.CLASS -> analyzeClassDataFlow(element, flowGraph)
                CodeElementType.FILE -> analyzeFileDataFlow(element, flowGraph)
                else -> {
                    logger.warn { "未实现的代码元素类型分析: ${element.type}" }
                    return@withContext createEmptyFlowGraph(element)
                }
            }

            logger.info { "数据流分析完成: ${element.qualifiedName}, 节点数: ${flowGraph.nodes.size}, 边数: ${flowGraph.edges.size}" }
            return@withContext flowGraph
        } catch (e: Exception) {
            logger.error(e) { "分析数据流时出错: ${element.qualifiedName}" }
            return@withContext createEmptyFlowGraph(element)
        }
    }

    override fun getSupportedElementTypes(): Set<CodeElementType> = setOf(
        CodeElementType.METHOD,
        CodeElementType.FUNCTION,
        CodeElementType.CLASS,
        CodeElementType.FILE
    )

    /**
     * 分析方法/函数的数据流
     *
     * @param element 方法/函数元素
     * @param flowGraph 流图
     */
    private fun analyzeMethodDataFlow(element: CodeElement, flowGraph: FlowGraph) {
        // 创建入口节点
        val entryNodeId = UUID.randomUUID().toString()
        val entryNode = FlowNode(
            id = entryNodeId,
            type = FlowNodeType.ENTRY,
            element = element,
            metadata = mutableMapOf(
                "label" to "Entry: ${element.name}"
            )
        )
        flowGraph.addNode(entryNode)
        flowGraph.setEntryNode(entryNodeId)

        // 创建出口节点
        val exitNodeId = UUID.randomUUID().toString()
        val exitNode = FlowNode(
            id = exitNodeId,
            type = FlowNodeType.EXIT,
            element = element,
            metadata = mutableMapOf(
                "label" to "Exit: ${element.name}"
            )
        )
        flowGraph.addNode(exitNode)
        flowGraph.addExitNode(exitNodeId)

        // 分析参数
        val parameterNodes = analyzeParameters(element, flowGraph)
        
        // 连接入口节点和参数节点
        for (paramNodeId in parameterNodes) {
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = entryNodeId,
                targetId = paramNodeId,
                type = FlowEdgeType.DATA_DEPENDENCY
            )
            flowGraph.addEdge(edge)
        }

        // 分析方法体中的变量定义和使用
        val variableMap = mutableMapOf<String, String>() // 变量名到节点ID的映射
        val bodyElements = element.children.filter { it.type in STATEMENT_TYPES }
        
        for (statement in bodyElements) {
            analyzeStatementDataFlow(statement, flowGraph, variableMap, parameterNodes)
        }

        // 分析返回值
        val returnStatements = element.children.filter { it.type == CodeElementType.RETURN_STATEMENT }
        for (returnStatement in returnStatements) {
            val returnNodeId = UUID.randomUUID().toString()
            val returnNode = FlowNode(
                id = returnNodeId,
                type = FlowNodeType.RETURN,
                element = returnStatement,
                metadata = mutableMapOf(
                    "label" to "Return: ${returnStatement.name}"
                )
            )
            flowGraph.addNode(returnNode)
            
            // 连接返回节点和出口节点
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = returnNodeId,
                targetId = exitNodeId,
                type = FlowEdgeType.DATA_DEPENDENCY
            )
            flowGraph.addEdge(edge)
            
            // 分析返回值表达式中的变量引用
            analyzeExpressionReferences(returnStatement, returnNodeId, flowGraph, variableMap)
        }
    }

    /**
     * 分析类的数据流
     *
     * @param element 类元素
     * @param flowGraph 流图
     */
    private fun analyzeClassDataFlow(element: CodeElement, flowGraph: FlowGraph) {
        // 创建入口节点
        val entryNodeId = UUID.randomUUID().toString()
        val entryNode = FlowNode(
            id = entryNodeId,
            type = FlowNodeType.ENTRY,
            element = element,
            metadata = mutableMapOf(
                "label" to "Class: ${element.name}"
            )
        )
        flowGraph.addNode(entryNode)
        flowGraph.setEntryNode(entryNodeId)

        // 创建出口节点
        val exitNodeId = UUID.randomUUID().toString()
        val exitNode = FlowNode(
            id = exitNodeId,
            type = FlowNodeType.EXIT,
            element = element,
            metadata = mutableMapOf(
                "label" to "End Class: ${element.name}"
            )
        )
        flowGraph.addNode(exitNode)
        flowGraph.addExitNode(exitNodeId)

        // 分析类的字段
        val fieldNodes = mutableMapOf<String, String>() // 字段名到节点ID的映射
        val fields = element.children.filter { it.type == CodeElementType.FIELD }
        
        for (field in fields) {
            val fieldNodeId = UUID.randomUUID().toString()
            val fieldNode = FlowNode(
                id = fieldNodeId,
                type = FlowNodeType.DECLARATION,
                element = field,
                metadata = mutableMapOf(
                    "label" to "Field: ${field.name}"
                )
            )
            flowGraph.addNode(fieldNode)
            fieldNodes[field.name] = fieldNodeId
            
            // 连接入口和字段节点
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = entryNodeId,
                targetId = fieldNodeId,
                type = FlowEdgeType.DATA_DEPENDENCY
            )
            flowGraph.addEdge(edge)
        }

        // 分析类的方法之间的数据流
        val methods = element.children.filter { it.type == CodeElementType.METHOD || it.type == CodeElementType.CONSTRUCTOR }
        val methodNodes = mutableMapOf<String, String>() // 方法名到节点ID的映射
        
        // 为每个方法创建节点
        for (method in methods) {
            val methodNodeId = UUID.randomUUID().toString()
            val methodNode = FlowNode(
                id = methodNodeId,
                type = FlowNodeType.CALL,
                element = method,
                metadata = mutableMapOf(
                    "label" to "Method: ${method.name}"
                )
            )
            flowGraph.addNode(methodNode)
            methodNodes[method.name] = methodNodeId
        }
        
        // 分析方法之间的调用关系
        for (method in methods) {
            val methodNodeId = methodNodes[method.name] ?: continue
            
            // 查找方法中对其他方法的调用
            val methodCalls = findMethodCalls(method)
            for (call in methodCalls) {
                val calledMethodName = call.name
                val calledMethodNodeId = methodNodes[calledMethodName]
                
                if (calledMethodNodeId != null) {
                    // 创建方法调用边
                    val edgeId = UUID.randomUUID().toString()
                    val edge = FlowEdge(
                        id = edgeId,
                        sourceId = methodNodeId,
                        targetId = calledMethodNodeId,
                        type = FlowEdgeType.CALL
                    )
                    flowGraph.addEdge(edge)
                }
            }
            
            // 查找方法中对字段的访问
            val fieldAccesses = findFieldAccesses(method)
            for (access in fieldAccesses) {
                val fieldName = access.name
                val fieldNodeId = fieldNodes[fieldName]
                
                if (fieldNodeId != null) {
                    // 创建字段访问边
                    val edgeId = UUID.randomUUID().toString()
                    val edge = FlowEdge(
                        id = edgeId,
                        sourceId = methodNodeId,
                        targetId = fieldNodeId,
                        type = FlowEdgeType.DATA_DEPENDENCY
                    )
                    flowGraph.addEdge(edge)
                }
            }
        }
    }

    /**
     * 分析文件的数据流
     *
     * @param element 文件元素
     * @param flowGraph 流图
     */
    private fun analyzeFileDataFlow(element: CodeElement, flowGraph: FlowGraph) {
        // 创建入口节点
        val entryNodeId = UUID.randomUUID().toString()
        val entryNode = FlowNode(
            id = entryNodeId,
            type = FlowNodeType.ENTRY,
            element = element,
            metadata = mutableMapOf(
                "label" to "File: ${element.name}"
            )
        )
        flowGraph.addNode(entryNode)
        flowGraph.setEntryNode(entryNodeId)

        // 创建出口节点
        val exitNodeId = UUID.randomUUID().toString()
        val exitNode = FlowNode(
            id = exitNodeId,
            type = FlowNodeType.EXIT,
            element = element,
            metadata = mutableMapOf(
                "label" to "End File: ${element.name}"
            )
        )
        flowGraph.addNode(exitNode)
        flowGraph.addExitNode(exitNodeId)

        // 分析文件中的全局变量
        val globalVarNodes = mutableMapOf<String, String>() // 变量名到节点ID的映射
        val globalVars = element.children.filter { it.type == CodeElementType.VARIABLE_DECLARATION && it.parent?.type == CodeElementType.FILE }
        
        for (variable in globalVars) {
            val varNodeId = UUID.randomUUID().toString()
            val varNode = FlowNode(
                id = varNodeId,
                type = FlowNodeType.DECLARATION,
                element = variable,
                metadata = mutableMapOf(
                    "label" to "Global Var: ${variable.name}"
                )
            )
            flowGraph.addNode(varNode)
            globalVarNodes[variable.name] = varNodeId
            
            // 连接入口和变量节点
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = entryNodeId,
                targetId = varNodeId,
                type = FlowEdgeType.DATA_DEPENDENCY
            )
            flowGraph.addEdge(edge)
        }

        // 分析文件中的函数
        val functions = element.children.filter { it.type == CodeElementType.FUNCTION }
        val functionNodes = mutableMapOf<String, String>() // 函数名到节点ID的映射
        
        // 为每个函数创建节点
        for (function in functions) {
            val functionNodeId = UUID.randomUUID().toString()
            val functionNode = FlowNode(
                id = functionNodeId,
                type = FlowNodeType.CALL,
                element = function,
                metadata = mutableMapOf(
                    "label" to "Function: ${function.name}"
                )
            )
            flowGraph.addNode(functionNode)
            functionNodes[function.name] = functionNodeId
            
            // 连接入口和函数节点
            val entryEdgeId = UUID.randomUUID().toString()
            val entryEdge = FlowEdge(
                id = entryEdgeId,
                sourceId = entryNodeId,
                targetId = functionNodeId,
                type = FlowEdgeType.SEQUENTIAL
            )
            flowGraph.addEdge(entryEdge)
        }
        
        // 分析函数之间的调用关系
        for (function in functions) {
            val functionNodeId = functionNodes[function.name] ?: continue
            
            // 查找函数中对其他函数的调用
            val functionCalls = findMethodCalls(function)
            for (call in functionCalls) {
                val calledFunctionName = call.name
                val calledFunctionNodeId = functionNodes[calledFunctionName]
                
                if (calledFunctionNodeId != null) {
                    // 创建函数调用边
                    val edgeId = UUID.randomUUID().toString()
                    val edge = FlowEdge(
                        id = edgeId,
                        sourceId = functionNodeId,
                        targetId = calledFunctionNodeId,
                        type = FlowEdgeType.CALL
                    )
                    flowGraph.addEdge(edge)
                }
            }
            
            // 查找函数中对全局变量的访问
            val globalVarAccesses = findGlobalVarAccesses(function, globalVars)
            for (access in globalVarAccesses) {
                val varName = access.name
                val varNodeId = globalVarNodes[varName]
                
                if (varNodeId != null) {
                    // 创建变量访问边
                    val edgeId = UUID.randomUUID().toString()
                    val edge = FlowEdge(
                        id = edgeId,
                        sourceId = functionNodeId,
                        targetId = varNodeId,
                        type = FlowEdgeType.DATA_DEPENDENCY
                    )
                    flowGraph.addEdge(edge)
                }
            }
        }
    }

    /**
     * 分析参数
     *
     * @param element 方法/函数元素
     * @param flowGraph 流图
     * @return 参数节点ID列表
     */
    private fun analyzeParameters(element: CodeElement, flowGraph: FlowGraph): List<String> {
        val parameterNodeIds = mutableListOf<String>()
        val parameters = element.children.filter { it.type == CodeElementType.PARAMETER }
        
        for (parameter in parameters) {
            val paramNodeId = UUID.randomUUID().toString()
            val paramNode = FlowNode(
                id = paramNodeId,
                type = FlowNodeType.DECLARATION,
                element = parameter,
                metadata = mutableMapOf(
                    "label" to "Param: ${parameter.name}"
                )
            )
            flowGraph.addNode(paramNode)
            parameterNodeIds.add(paramNodeId)
        }
        
        return parameterNodeIds
    }

    /**
     * 分析语句的数据流
     *
     * @param element 语句元素
     * @param flowGraph 流图
     * @param variableMap 变量名到节点ID的映射
     * @param parameterNodes 参数节点ID列表
     */
    private fun analyzeStatementDataFlow(
        element: CodeElement, 
        flowGraph: FlowGraph, 
        variableMap: MutableMap<String, String>,
        parameterNodes: List<String> = emptyList()
    ) {
        when (element.type) {
            CodeElementType.VARIABLE_DECLARATION -> {
                // 创建变量声明节点
                val varNodeId = UUID.randomUUID().toString()
                val varNode = FlowNode(
                    id = varNodeId,
                    type = FlowNodeType.DECLARATION,
                    element = element,
                    metadata = mutableMapOf(
                        "label" to "Var: ${element.name}"
                    )
                )
                flowGraph.addNode(varNode)
                variableMap[element.name] = varNodeId
                
                // 分析初始化表达式中的变量引用
                analyzeExpressionReferences(element, varNodeId, flowGraph, variableMap)
            }
            CodeElementType.ASSIGNMENT -> {
                // 创建赋值节点
                val assignNodeId = UUID.randomUUID().toString()
                val assignNode = FlowNode(
                    id = assignNodeId,
                    type = FlowNodeType.ASSIGNMENT,
                    element = element,
                    metadata = mutableMapOf(
                        "label" to "Assign: ${element.name}"
                    )
                )
                flowGraph.addNode(assignNode)
                
                // 获取左侧变量名
                val leftSide = element.name.substringBefore("=").trim()
                
                // 更新变量映射
                variableMap[leftSide] = assignNodeId
                
                // 分析右侧表达式中的变量引用
                analyzeExpressionReferences(element, assignNodeId, flowGraph, variableMap)
            }
            CodeElementType.METHOD_CALL -> {
                // 创建方法调用节点
                val callNodeId = UUID.randomUUID().toString()
                val callNode = FlowNode(
                    id = callNodeId,
                    type = FlowNodeType.CALL,
                    element = element,
                    metadata = mutableMapOf(
                        "label" to "Call: ${element.name}"
                    )
                )
                flowGraph.addNode(callNode)
                
                // 分析方法调用参数中的变量引用
                analyzeExpressionReferences(element, callNodeId, flowGraph, variableMap)
            }
            CodeElementType.IF_STATEMENT, 
            CodeElementType.FOR_STATEMENT, 
            CodeElementType.WHILE_STATEMENT, 
            CodeElementType.DO_WHILE_STATEMENT -> {
                // 创建条件节点
                val condNodeId = UUID.randomUUID().toString()
                val condNode = FlowNode(
                    id = condNodeId,
                    type = when (element.type) {
                        CodeElementType.IF_STATEMENT -> FlowNodeType.CONDITION
                        else -> FlowNodeType.LOOP
                    },
                    element = element,
                    metadata = mutableMapOf(
                        "label" to "${element.type}: ${element.name}"
                    )
                )
                flowGraph.addNode(condNode)
                
                // 分析条件表达式中的变量引用
                analyzeExpressionReferences(element, condNodeId, flowGraph, variableMap)
                
                // 递归分析语句块中的语句
                val bodyElements = element.children.filter { it.type in STATEMENT_TYPES }
                for (statement in bodyElements) {
                    analyzeStatementDataFlow(statement, flowGraph, variableMap, parameterNodes)
                }
            }
            else -> {
                // 对于其他类型的语句，递归分析子语句
                val childStatements = element.children.filter { it.type in STATEMENT_TYPES }
                for (statement in childStatements) {
                    analyzeStatementDataFlow(statement, flowGraph, variableMap, parameterNodes)
                }
            }
        }
    }

    /**
     * 分析表达式中的变量引用
     *
     * @param element 表达式元素
     * @param nodeId 当前节点ID
     * @param flowGraph 流图
     * @param variableMap 变量名到节点ID的映射
     */
    private fun analyzeExpressionReferences(
        element: CodeElement, 
        nodeId: String, 
        flowGraph: FlowGraph, 
        variableMap: Map<String, String>
    ) {
        // 查找表达式中的变量引用
        val references = findVariableReferences(element)
        
        for (reference in references) {
            val varName = reference.name
            val varNodeId = variableMap[varName]
            
            if (varNodeId != null) {
                // 创建数据依赖边
                val edgeId = UUID.randomUUID().toString()
                val edge = FlowEdge(
                    id = edgeId,
                    sourceId = varNodeId,
                    targetId = nodeId,
                    type = FlowEdgeType.DATA_DEPENDENCY,
                    metadata = mutableMapOf(
                        "label" to "Uses: $varName"
                    )
                )
                flowGraph.addEdge(edge)
            }
        }
    }

    /**
     * 查找变量引用
     *
     * @param element 代码元素
     * @return 变量引用元素列表
     */
    private fun findVariableReferences(element: CodeElement): List<CodeElement> {
        val references = mutableListOf<CodeElement>()
        
        // 如果元素本身是变量引用
        if (element.type == CodeElementType.VARIABLE_REFERENCE) {
            references.add(element)
        }
        
        // 递归查找子元素中的变量引用
        for (child in element.children) {
            references.addAll(findVariableReferences(child))
        }
        
        return references
    }

    /**
     * 查找方法调用
     *
     * @param element 代码元素
     * @return 方法调用元素列表
     */
    private fun findMethodCalls(element: CodeElement): List<CodeElement> {
        val calls = mutableListOf<CodeElement>()
        
        // 如果元素本身是方法调用
        if (element.type == CodeElementType.METHOD_CALL) {
            calls.add(element)
        }
        
        // 递归查找子元素中的方法调用
        for (child in element.children) {
            calls.addAll(findMethodCalls(child))
        }
        
        return calls
    }

    /**
     * 查找字段访问
     *
     * @param element 代码元素
     * @return 字段访问元素列表
     */
    private fun findFieldAccesses(element: CodeElement): List<CodeElement> {
        val accesses = mutableListOf<CodeElement>()
        
        // 如果元素本身是字段访问
        if (element.type == CodeElementType.FIELD_ACCESS) {
            accesses.add(element)
        }
        
        // 递归查找子元素中的字段访问
        for (child in element.children) {
            accesses.addAll(findFieldAccesses(child))
        }
        
        return accesses
    }

    /**
     * 查找全局变量访问
     *
     * @param element 代码元素
     * @param globalVars 全局变量列表
     * @return 全局变量访问元素列表
     */
    private fun findGlobalVarAccesses(element: CodeElement, globalVars: List<CodeElement>): List<CodeElement> {
        val accesses = mutableListOf<CodeElement>()
        val globalVarNames = globalVars.map { it.name }.toSet()
        
        // 查找变量引用
        val references = findVariableReferences(element)
        
        // 过滤出全局变量引用
        for (reference in references) {
            if (reference.name in globalVarNames) {
                accesses.add(reference)
            }
        }
        
        return accesses
    }

    /**
     * 创建空的流图
     *
     * @param element 代码元素
     * @return 空流图
     */
    private fun createEmptyFlowGraph(element: CodeElement): FlowGraph {
        val flowGraph = FlowGraph(
            id = UUID.randomUUID().toString(),
            name = "${element.name} 数据流图 (空)",
            type = FlowType.DATA_FLOW,
            metadata = mutableMapOf(
                "elementId" to element.id,
                "elementType" to element.type.name,
                "language" to element.language,
                "isEmpty" to true
            )
        )
        
        // 创建入口节点
        val entryNodeId = UUID.randomUUID().toString()
        val entryNode = FlowNode(
            id = entryNodeId,
            type = FlowNodeType.ENTRY,
            element = element,
            metadata = mutableMapOf(
                "label" to "Entry: ${element.name}"
            )
        )
        flowGraph.addNode(entryNode)
        flowGraph.setEntryNode(entryNodeId)
        
        // 创建出口节点
        val exitNodeId = UUID.randomUUID().toString()
        val exitNode = FlowNode(
            id = exitNodeId,
            type = FlowNodeType.EXIT,
            element = element,
            metadata = mutableMapOf(
                "label" to "Exit: ${element.name}"
            )
        )
        flowGraph.addNode(exitNode)
        flowGraph.addExitNode(exitNodeId)
        
        // 连接入口和出口
        val edgeId = UUID.randomUUID().toString()
        val edge = FlowEdge(
            id = edgeId,
            sourceId = entryNodeId,
            targetId = exitNodeId,
            type = FlowEdgeType.DATA_DEPENDENCY,
            metadata = mutableMapOf(
                "label" to "Empty"
            )
        )
        flowGraph.addEdge(edge)
        
        return flowGraph
    }

    companion object {
        /**
         * 语句类型集合
         */
        private val STATEMENT_TYPES = setOf(
            CodeElementType.STATEMENT,
            CodeElementType.IF_STATEMENT,
            CodeElementType.FOR_STATEMENT,
            CodeElementType.WHILE_STATEMENT,
            CodeElementType.DO_WHILE_STATEMENT,
            CodeElementType.SWITCH_STATEMENT,
            CodeElementType.TRY_STATEMENT,
            CodeElementType.CATCH_CLAUSE,
            CodeElementType.FINALLY_BLOCK,
            CodeElementType.RETURN_STATEMENT,
            CodeElementType.THROW_STATEMENT,
            CodeElementType.METHOD_CALL,
            CodeElementType.VARIABLE_DECLARATION,
            CodeElementType.ASSIGNMENT,
            CodeElementType.EXPRESSION
        )
    }
}
