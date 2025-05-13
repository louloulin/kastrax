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
 * 控制流分析器实现
 *
 * 分析代码元素的控制流，生成控制流图
 *
 * @property config 配置
 */
class ControlFlowAnalyzerImpl(
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
        logger.info { "开始分析控制流: ${element.qualifiedName}" }

        if (!supportsElement(element)) {
            logger.warn { "不支持的代码元素类型: ${element.type}" }
            return@withContext createEmptyFlowGraph(element)
        }

        // 创建流图
        val flowGraph = FlowGraph(
            id = UUID.randomUUID().toString(),
            name = "${element.name} 控制流图",
            type = FlowType.CONTROL_FLOW,
            metadata = mutableMapOf(
                "elementId" to element.id,
                "elementType" to element.type.name,
                "language" to element.language
            )
        )

        try {
            when (element.type) {
                CodeElementType.METHOD -> analyzeMethodFlow(element, flowGraph)
                CodeElementType.CLASS -> analyzeClassFlow(element, flowGraph)
                CodeElementType.FILE -> analyzeFileFlow(element, flowGraph)
                else -> {
                    logger.warn { "未实现的代码元素类型分析: ${element.type}" }
                    return@withContext createEmptyFlowGraph(element)
                }
            }

            logger.info { "控制流分析完成: ${element.qualifiedName}, 节点数: ${flowGraph.nodes.size}, 边数: ${flowGraph.edges.size}" }
            return@withContext flowGraph
        } catch (e: Exception) {
            logger.error(e) { "分析控制流时出错: ${element.qualifiedName}" }
            return@withContext createEmptyFlowGraph(element)
        }
    }

    /**
     * 分析指定类型的代码流
     *
     * @param element 代码元素
     * @param type 流类型
     * @return 流图
     */
    override suspend fun analyzeFlow(element: CodeElement, type: FlowType): FlowGraph = withContext(Dispatchers.Default) {
        // 只支持控制流
        if (type != FlowType.CONTROL_FLOW) {
            logger.warn { "不支持的流类型: $type" }
            return@withContext createEmptyFlowGraph(element)
        }

        // 检查缓存
        val cacheKey = "${element.id}:${type.name}"
        if (config.enableCaching && graphCache.containsKey(cacheKey)) {
            return@withContext graphCache[cacheKey]!!
        }

        // 分析控制流
        val flowGraph = analyzeFlow(element)

        // 缓存流图
        if (config.enableCaching) {
            graphCache[cacheKey] = flowGraph
        }

        return@withContext flowGraph
    }

    /**
     * 获取支持的代码元素类型
     *
     * @return 支持的代码元素类型集合
     */
    override fun getSupportedElementTypes(): Set<CodeElementType> = setOf(
        CodeElementType.METHOD,
        CodeElementType.CLASS,
        CodeElementType.FILE,
        CodeElementType.FUNCTION,
        CodeElementType.CONSTRUCTOR
    )

    /**
     * 获取支持的流类型
     *
     * @return 支持的流类型集合
     */
    override fun getSupportedFlowTypes(): Set<FlowType> = setOf(
        FlowType.CONTROL_FLOW
    )

    /**
     * 清除缓存
     */
    override fun clearCache() {
        graphCache.clear()
    }

    /**
     * 分析方法/函数的控制流
     *
     * @param element 方法/函数元素
     * @param flowGraph 流图
     */
    private fun analyzeMethodFlow(element: CodeElement, flowGraph: FlowGraph) {
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

        // 分析方法体
        val bodyElements = element.children.filter { it.type in STATEMENT_TYPES }

        if (bodyElements.isEmpty()) {
            // 如果方法体为空，直接连接入口和出口
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = entryNodeId,
                targetId = exitNodeId,
                type = FlowEdgeType.SEQUENTIAL,
                metadata = mutableMapOf(
                    "label" to "Empty body"
                )
            )
            flowGraph.addEdge(edge)
            return
        }

        // 分析方法体语句
        var currentNodeId = entryNodeId
        for (statement in bodyElements) {
            val statementNodeId = analyzeStatement(statement, flowGraph)

            // 连接当前节点和语句节点
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = currentNodeId,
                targetId = statementNodeId,
                type = FlowEdgeType.SEQUENTIAL
            )
            flowGraph.addEdge(edge)

            currentNodeId = statementNodeId
        }

        // 连接最后一个语句和出口节点
        val edgeId = UUID.randomUUID().toString()
        val edge = FlowEdge(
            id = edgeId,
            sourceId = currentNodeId,
            targetId = exitNodeId,
            type = FlowEdgeType.SEQUENTIAL
        )
        flowGraph.addEdge(edge)
    }

    /**
     * 分析类的控制流
     *
     * @param element 类元素
     * @param flowGraph 流图
     */
    private suspend fun analyzeClassFlow(element: CodeElement, flowGraph: FlowGraph) {
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

        // 分析类的方法
        val methods = element.children.filter { it.type == CodeElementType.METHOD || it.type == CodeElementType.CONSTRUCTOR || it.type == CodeElementType.FUNCTION }

        if (methods.isEmpty()) {
            // 如果类没有方法，直接连接入口和出口
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = entryNodeId,
                targetId = exitNodeId,
                type = FlowEdgeType.SEQUENTIAL,
                metadata = mutableMapOf(
                    "label" to "No methods"
                )
            )
            flowGraph.addEdge(edge)
            return
        }

        // 并行或串行分析方法
        if (config.enableParallelAnalysis) {
            // 并行分析方法
            coroutineScope {
                methods.chunked(config.maxConcurrentTasks).forEach { chunk ->
                    chunk.map { method ->
                        async {
                            analyzeMethodInClass(method, flowGraph, entryNodeId, exitNodeId)
                        }
                    }.awaitAll()
                }
            }
        } else {
            // 串行分析方法
            methods.forEach { method ->
                analyzeMethodInClass(method, flowGraph, entryNodeId, exitNodeId)
            }
        }
    }

    /**
     * 在类中分析方法
     *
     * @param method 方法元素
     * @param flowGraph 流图
     * @param entryNodeId 入口节点ID
     * @param exitNodeId 出口节点ID
     */
    private fun analyzeMethodInClass(method: CodeElement, flowGraph: FlowGraph, entryNodeId: String, exitNodeId: String) {
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

        // 连接入口和方法节点
        val entryEdgeId = UUID.randomUUID().toString()
        val entryEdge = FlowEdge(
            id = entryEdgeId,
            sourceId = entryNodeId,
            targetId = methodNodeId,
            type = FlowEdgeType.CALL
        )
        flowGraph.addEdge(entryEdge)

        // 连接方法节点和出口节点
        val exitEdgeId = UUID.randomUUID().toString()
        val exitEdge = FlowEdge(
            id = exitEdgeId,
            sourceId = methodNodeId,
            targetId = exitNodeId,
            type = FlowEdgeType.RETURN
        )
        flowGraph.addEdge(exitEdge)
    }

    /**
     * 分析文件的控制流
     *
     * @param element 文件元素
     * @param flowGraph 流图
     */
    private fun analyzeFileFlow(element: CodeElement, flowGraph: FlowGraph) {
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

        // 分析文件中的顶级元素
        val topLevelElements = element.children.filter {
            it.type == CodeElementType.CLASS ||
            it.type == CodeElementType.METHOD ||
            it.type == CodeElementType.STATEMENT
        }

        if (topLevelElements.isEmpty()) {
            // 如果文件没有顶级元素，直接连接入口和出口
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = entryNodeId,
                targetId = exitNodeId,
                type = FlowEdgeType.SEQUENTIAL,
                metadata = mutableMapOf(
                    "label" to "Empty file"
                )
            )
            flowGraph.addEdge(edge)
            return
        }

        // 为每个顶级元素创建节点
        for (element in topLevelElements) {
            val elementNodeId = UUID.randomUUID().toString()
            val elementNode = FlowNode(
                id = elementNodeId,
                type = when (element.type) {
                    CodeElementType.CLASS -> FlowNodeType.STATEMENT
                    CodeElementType.METHOD -> FlowNodeType.CALL
                    else -> FlowNodeType.STATEMENT
                },
                element = element,
                metadata = mutableMapOf(
                    "label" to "${element.type}: ${element.name}"
                )
            )
            flowGraph.addNode(elementNode)

            // 连接入口和元素节点
            val entryEdgeId = UUID.randomUUID().toString()
            val entryEdge = FlowEdge(
                id = entryEdgeId,
                sourceId = entryNodeId,
                targetId = elementNodeId,
                type = FlowEdgeType.SEQUENTIAL
            )
            flowGraph.addEdge(entryEdge)

            // 连接元素节点和出口节点
            val exitEdgeId = UUID.randomUUID().toString()
            val exitEdge = FlowEdge(
                id = exitEdgeId,
                sourceId = elementNodeId,
                targetId = exitNodeId,
                type = FlowEdgeType.SEQUENTIAL
            )
            flowGraph.addEdge(exitEdge)
        }
    }

    /**
     * 分析语句
     *
     * @param element 语句元素
     * @param flowGraph 流图
     * @return 语句节点ID
     */
    private fun analyzeStatement(element: CodeElement, flowGraph: FlowGraph): String {
        val nodeId = UUID.randomUUID().toString()
        val nodeType = when (element.type) {
            CodeElementType.STATEMENT -> {
                // 根据语句名称或内容推断节点类型
                when {
                    element.name.contains("if", ignoreCase = true) -> FlowNodeType.CONDITION
                    element.name.contains("for", ignoreCase = true) ||
                    element.name.contains("while", ignoreCase = true) -> FlowNodeType.LOOP
                    element.name.contains("switch", ignoreCase = true) ||
                    element.name.contains("case", ignoreCase = true) -> FlowNodeType.BRANCH
                    element.name.contains("try", ignoreCase = true) -> FlowNodeType.STATEMENT
                    element.name.contains("catch", ignoreCase = true) -> FlowNodeType.CATCH
                    element.name.contains("finally", ignoreCase = true) -> FlowNodeType.FINALLY
                    element.name.contains("return", ignoreCase = true) -> FlowNodeType.RETURN
                    element.name.contains("throw", ignoreCase = true) -> FlowNodeType.THROW
                    element.name.contains("call", ignoreCase = true) ||
                    element.name.contains("invoke", ignoreCase = true) -> FlowNodeType.CALL
                    element.name.contains("var", ignoreCase = true) ||
                    element.name.contains("val", ignoreCase = true) ||
                    element.name.contains("let", ignoreCase = true) ||
                    element.name.contains("const", ignoreCase = true) -> FlowNodeType.DECLARATION
                    element.name.contains("=") && !element.name.contains("==") -> FlowNodeType.ASSIGNMENT
                    else -> FlowNodeType.STATEMENT
                }
            }
            CodeElementType.BLOCK -> FlowNodeType.STATEMENT
            CodeElementType.EXPRESSION -> FlowNodeType.STATEMENT
            else -> FlowNodeType.STATEMENT
        }

        val node = FlowNode(
            id = nodeId,
            type = nodeType,
            element = element,
            metadata = mutableMapOf(
                "label" to "${element.type}: ${element.name}"
            )
        )
        flowGraph.addNode(node)

        return nodeId
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
            name = "${element.name} 控制流图 (空)",
            type = FlowType.CONTROL_FLOW,
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
            type = FlowEdgeType.SEQUENTIAL,
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
            CodeElementType.EXPRESSION,
            CodeElementType.BLOCK
        )
    }
}
