package ai.kastrax.codebase.semantic.flow.lang

import ai.kastrax.codebase.semantic.flow.*
import ai.kastrax.codebase.semantic.flow.impl.CodeFlowAnalyzerImpl
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Java/Kotlin 代码流分析器
 *
 * 针对 Java 和 Kotlin 语言的代码流分析器，提供特定语言的分析逻辑
 *
 * @property baseAnalyzer 基础代码流分析器
 * @property config 配置
 */
class JavaKotlinFlowAnalyzer(
    private val baseAnalyzer: CodeFlowAnalyzerImpl = CodeFlowAnalyzerImpl(),
    private val config: CodeFlowAnalyzerConfig = CodeFlowAnalyzerConfig()
) : CodeFlowAnalyzer {
    private val logger = KotlinLogging.logger {}
    
    /**
     * 支持的语言
     */
    private val supportedLanguages = setOf("java", "kotlin")

    override suspend fun analyzeFlow(element: CodeElement): FlowGraph = withContext(Dispatchers.Default) {
        logger.info { "开始分析 Java/Kotlin 代码流: ${element.qualifiedName}" }

        if (!supportsElement(element)) {
            logger.warn { "不支持的代码元素类型或语言: ${element.type}, ${element.language}" }
            return@withContext createEmptyFlowGraph(element)
        }

        try {
            // 使用基础分析器进行分析
            val baseFlowGraph = baseAnalyzer.analyzeFlow(element)
            
            // 增强流图，添加 Java/Kotlin 特定的分析
            val enhancedGraph = enhanceFlowGraph(baseFlowGraph, element)
            
            logger.info { "Java/Kotlin 代码流分析完成: ${element.qualifiedName}" }
            return@withContext enhancedGraph
        } catch (e: Exception) {
            logger.error(e) { "分析 Java/Kotlin 代码流时出错: ${element.qualifiedName}" }
            return@withContext createEmptyFlowGraph(element)
        }
    }

    override fun getSupportedElementTypes(): Set<CodeElementType> {
        return baseAnalyzer.getSupportedElementTypes()
    }

    override fun supportsElement(element: CodeElement): Boolean {
        return baseAnalyzer.supportsElement(element) && element.language.lowercase() in supportedLanguages
    }

    /**
     * 增强流图，添加 Java/Kotlin 特定的分析
     *
     * @param baseGraph 基础流图
     * @param element 代码元素
     * @return 增强后的流图
     */
    private fun enhanceFlowGraph(baseGraph: FlowGraph, element: CodeElement): FlowGraph {
        // 创建增强后的流图副本
        val enhancedGraph = baseGraph.copy(
            metadata = baseGraph.metadata.toMutableMap().apply {
                put("enhancedForLanguage", element.language)
            }
        )
        
        // 复制节点和边
        for ((nodeId, node) in baseGraph.nodes) {
            enhancedGraph.nodes[nodeId] = node
        }
        
        for ((edgeId, edge) in baseGraph.edges) {
            enhancedGraph.edges[edgeId] = edge
        }
        
        // 设置入口和出口节点
        baseGraph.entryNodeId?.let { enhancedGraph.setEntryNode(it) }
        baseGraph.exitNodeIds.forEach { enhancedGraph.addExitNode(it) }
        
        // 根据元素类型进行特定增强
        when (element.type) {
            CodeElementType.METHOD, CodeElementType.FUNCTION -> enhanceMethodFlow(enhancedGraph, element)
            CodeElementType.CLASS -> enhanceClassFlow(enhancedGraph, element)
            CodeElementType.FILE -> enhanceFileFlow(enhancedGraph, element)
            else -> {
                logger.warn { "未实现的 Java/Kotlin 特定增强: ${element.type}" }
            }
        }
        
        return enhancedGraph
    }

    /**
     * 增强方法/函数流图
     *
     * @param graph 流图
     * @param element 方法/函数元素
     */
    private fun enhanceMethodFlow(graph: FlowGraph, element: CodeElement) {
        // 分析 Java/Kotlin 特有的语言特性
        
        // 1. 分析 Lambda 表达式
        val lambdas = findLambdaExpressions(element)
        for (lambda in lambdas) {
            val lambdaNodeId = UUID.randomUUID().toString()
            val lambdaNode = FlowNode(
                id = lambdaNodeId,
                type = FlowNodeType.CALL,
                element = lambda,
                metadata = mutableMapOf(
                    "label" to "Lambda: ${lambda.name}",
                    "isLambda" to true
                )
            )
            graph.addNode(lambdaNode)
            
            // 连接 Lambda 和相关节点
            connectLambdaToGraph(graph, lambdaNodeId, lambda)
        }
        
        // 2. 分析异常处理
        val tryBlocks = findTryBlocks(element)
        for (tryBlock in tryBlocks) {
            enhanceTryBlock(graph, tryBlock)
        }
        
        // 3. 分析协程 (Kotlin 特有)
        if (element.language.equals("kotlin", ignoreCase = true)) {
            val suspendCalls = findSuspendCalls(element)
            for (suspendCall in suspendCalls) {
                enhanceSuspendCall(graph, suspendCall)
            }
        }
    }

    /**
     * 增强类流图
     *
     * @param graph 流图
     * @param element 类元素
     */
    private fun enhanceClassFlow(graph: FlowGraph, element: CodeElement) {
        // 分析 Java/Kotlin 类特有的语言特性
        
        // 1. 分析继承关系
        val superClass = findSuperClass(element)
        if (superClass != null) {
            val superClassNodeId = UUID.randomUUID().toString()
            val superClassNode = FlowNode(
                id = superClassNodeId,
                type = FlowNodeType.STATEMENT,
                element = superClass,
                metadata = mutableMapOf(
                    "label" to "SuperClass: ${superClass.name}",
                    "isInheritance" to true
                )
            )
            graph.addNode(superClassNode)
            
            // 连接类和父类
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = graph.entryNodeId ?: return,
                targetId = superClassNodeId,
                type = FlowEdgeType.CONTROL_DEPENDENCY,
                metadata = mutableMapOf(
                    "label" to "Extends",
                    "isInheritance" to true
                )
            )
            graph.addEdge(edge)
        }
        
        // 2. 分析接口实现
        val interfaces = findInterfaces(element)
        for (interface_ in interfaces) {
            val interfaceNodeId = UUID.randomUUID().toString()
            val interfaceNode = FlowNode(
                id = interfaceNodeId,
                type = FlowNodeType.STATEMENT,
                element = interface_,
                metadata = mutableMapOf(
                    "label" to "Interface: ${interface_.name}",
                    "isInterface" to true
                )
            )
            graph.addNode(interfaceNode)
            
            // 连接类和接口
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = graph.entryNodeId ?: return,
                targetId = interfaceNodeId,
                type = FlowEdgeType.CONTROL_DEPENDENCY,
                metadata = mutableMapOf(
                    "label" to "Implements",
                    "isInterface" to true
                )
            )
            graph.addEdge(edge)
        }
        
        // 3. 分析内部类
        val innerClasses = findInnerClasses(element)
        for (innerClass in innerClasses) {
            val innerClassNodeId = UUID.randomUUID().toString()
            val innerClassNode = FlowNode(
                id = innerClassNodeId,
                type = FlowNodeType.STATEMENT,
                element = innerClass,
                metadata = mutableMapOf(
                    "label" to "InnerClass: ${innerClass.name}",
                    "isInnerClass" to true
                )
            )
            graph.addNode(innerClassNode)
            
            // 连接类和内部类
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = graph.entryNodeId ?: return,
                targetId = innerClassNodeId,
                type = FlowEdgeType.CONTROL_DEPENDENCY,
                metadata = mutableMapOf(
                    "label" to "Contains",
                    "isInnerClass" to true
                )
            )
            graph.addEdge(edge)
        }
    }

    /**
     * 增强文件流图
     *
     * @param graph 流图
     * @param element 文件元素
     */
    private fun enhanceFileFlow(graph: FlowGraph, element: CodeElement) {
        // 分析 Java/Kotlin 文件特有的语言特性
        
        // 1. 分析包声明
        val packageDecl = findPackageDeclaration(element)
        if (packageDecl != null) {
            val packageNodeId = UUID.randomUUID().toString()
            val packageNode = FlowNode(
                id = packageNodeId,
                type = FlowNodeType.STATEMENT,
                element = packageDecl,
                metadata = mutableMapOf(
                    "label" to "Package: ${packageDecl.name}",
                    "isPackage" to true
                )
            )
            graph.addNode(packageNode)
            
            // 连接文件和包声明
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = graph.entryNodeId ?: return,
                targetId = packageNodeId,
                type = FlowEdgeType.SEQUENTIAL,
                metadata = mutableMapOf(
                    "label" to "Declares",
                    "isPackage" to true
                )
            )
            graph.addEdge(edge)
        }
        
        // 2. 分析导入语句
        val imports = findImports(element)
        for (import in imports) {
            val importNodeId = UUID.randomUUID().toString()
            val importNode = FlowNode(
                id = importNodeId,
                type = FlowNodeType.STATEMENT,
                element = import,
                metadata = mutableMapOf(
                    "label" to "Import: ${import.name}",
                    "isImport" to true
                )
            )
            graph.addNode(importNode)
            
            // 连接文件和导入语句
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = graph.entryNodeId ?: return,
                targetId = importNodeId,
                type = FlowEdgeType.SEQUENTIAL,
                metadata = mutableMapOf(
                    "label" to "Imports",
                    "isImport" to true
                )
            )
            graph.addEdge(edge)
        }
    }

    /**
     * 连接 Lambda 表达式到流图
     *
     * @param graph 流图
     * @param lambdaNodeId Lambda 节点 ID
     * @param lambda Lambda 表达式元素
     */
    private fun connectLambdaToGraph(graph: FlowGraph, lambdaNodeId: String, lambda: CodeElement) {
        // 查找包含 Lambda 的父元素
        val parent = lambda.parent ?: return
        
        // 查找父元素对应的节点
        val parentNode = graph.nodes.values.find { it.element?.id == parent.id }
        if (parentNode != null) {
            // 连接父节点和 Lambda 节点
            val edgeId = UUID.randomUUID().toString()
            val edge = FlowEdge(
                id = edgeId,
                sourceId = parentNode.id,
                targetId = lambdaNodeId,
                type = FlowEdgeType.CALL,
                metadata = mutableMapOf(
                    "label" to "Lambda",
                    "isLambda" to true
                )
            )
            graph.addEdge(edge)
        }
    }

    /**
     * 增强 try 块
     *
     * @param graph 流图
     * @param tryBlock try 块元素
     */
    private fun enhanceTryBlock(graph: FlowGraph, tryBlock: CodeElement) {
        // 查找 try 块对应的节点
        val tryNode = graph.nodes.values.find { it.element?.id == tryBlock.id }
        if (tryNode == null) return
        
        // 查找 catch 子句
        val catchClauses = tryBlock.children.filter { it.type == CodeElementType.CATCH_CLAUSE }
        for (catchClause in catchClauses) {
            // 查找 catch 子句对应的节点
            val catchNode = graph.nodes.values.find { it.element?.id == catchClause.id }
            if (catchNode != null) {
                // 连接 try 节点和 catch 节点
                val edgeId = UUID.randomUUID().toString()
                val edge = FlowEdge(
                    id = edgeId,
                    sourceId = tryNode.id,
                    targetId = catchNode.id,
                    type = FlowEdgeType.EXCEPTION,
                    metadata = mutableMapOf(
                        "label" to "Catches",
                        "isException" to true
                    )
                )
                graph.addEdge(edge)
            }
        }
        
        // 查找 finally 块
        val finallyBlock = tryBlock.children.find { it.type == CodeElementType.FINALLY_BLOCK }
        if (finallyBlock != null) {
            // 查找 finally 块对应的节点
            val finallyNode = graph.nodes.values.find { it.element?.id == finallyBlock.id }
            if (finallyNode != null) {
                // 连接 try 节点和 finally 节点
                val edgeId = UUID.randomUUID().toString()
                val edge = FlowEdge(
                    id = edgeId,
                    sourceId = tryNode.id,
                    targetId = finallyNode.id,
                    type = FlowEdgeType.SEQUENTIAL,
                    metadata = mutableMapOf(
                        "label" to "Finally",
                        "isFinally" to true
                    )
                )
                graph.addEdge(edge)
                
                // 连接 catch 节点和 finally 节点
                for (catchClause in catchClauses) {
                    val catchNode = graph.nodes.values.find { it.element?.id == catchClause.id }
                    if (catchNode != null) {
                        val catchFinallyEdgeId = UUID.randomUUID().toString()
                        val catchFinallyEdge = FlowEdge(
                            id = catchFinallyEdgeId,
                            sourceId = catchNode.id,
                            targetId = finallyNode.id,
                            type = FlowEdgeType.SEQUENTIAL,
                            metadata = mutableMapOf(
                                "label" to "Finally",
                                "isFinally" to true
                            )
                        )
                        graph.addEdge(catchFinallyEdge)
                    }
                }
            }
        }
    }

    /**
     * 增强 suspend 调用
     *
     * @param graph 流图
     * @param suspendCall suspend 调用元素
     */
    private fun enhanceSuspendCall(graph: FlowGraph, suspendCall: CodeElement) {
        // 查找 suspend 调用对应的节点
        val suspendNode = graph.nodes.values.find { it.element?.id == suspendCall.id }
        if (suspendNode == null) return
        
        // 更新节点元数据
        suspendNode.metadata["isSuspend"] = true
        suspendNode.metadata["label"] = "Suspend: ${suspendCall.name}"
    }

    /**
     * 查找 Lambda 表达式
     *
     * @param element 代码元素
     * @return Lambda 表达式元素列表
     */
    private fun findLambdaExpressions(element: CodeElement): List<CodeElement> {
        val lambdas = mutableListOf<CodeElement>()
        
        // 如果元素本身是 Lambda 表达式
        if (element.type == CodeElementType.LAMBDA_EXPRESSION) {
            lambdas.add(element)
        }
        
        // 递归查找子元素中的 Lambda 表达式
        for (child in element.children) {
            lambdas.addAll(findLambdaExpressions(child))
        }
        
        return lambdas
    }

    /**
     * 查找 try 块
     *
     * @param element 代码元素
     * @return try 块元素列表
     */
    private fun findTryBlocks(element: CodeElement): List<CodeElement> {
        val tryBlocks = mutableListOf<CodeElement>()
        
        // 如果元素本身是 try 块
        if (element.type == CodeElementType.TRY_STATEMENT) {
            tryBlocks.add(element)
        }
        
        // 递归查找子元素中的 try 块
        for (child in element.children) {
            tryBlocks.addAll(findTryBlocks(child))
        }
        
        return tryBlocks
    }

    /**
     * 查找 suspend 调用
     *
     * @param element 代码元素
     * @return suspend 调用元素列表
     */
    private fun findSuspendCalls(element: CodeElement): List<CodeElement> {
        val suspendCalls = mutableListOf<CodeElement>()
        
        // 如果元素本身是方法调用，且是 suspend 调用
        if (element.type == CodeElementType.METHOD_CALL && element.modifiers.any { it.name.equals("suspend", ignoreCase = true) }) {
            suspendCalls.add(element)
        }
        
        // 递归查找子元素中的 suspend 调用
        for (child in element.children) {
            suspendCalls.addAll(findSuspendCalls(child))
        }
        
        return suspendCalls
    }

    /**
     * 查找父类
     *
     * @param element 类元素
     * @return 父类元素，如果没有则返回 null
     */
    private fun findSuperClass(element: CodeElement): CodeElement? {
        // 查找类的父类引用
        return element.children.find { it.type == CodeElementType.SUPER_CLASS }
    }

    /**
     * 查找接口
     *
     * @param element 类元素
     * @return 接口元素列表
     */
    private fun findInterfaces(element: CodeElement): List<CodeElement> {
        // 查找类实现的接口
        return element.children.filter { it.type == CodeElementType.INTERFACE }
    }

    /**
     * 查找内部类
     *
     * @param element 类元素
     * @return 内部类元素列表
     */
    private fun findInnerClasses(element: CodeElement): List<CodeElement> {
        // 查找类中的内部类
        return element.children.filter { it.type == CodeElementType.CLASS && it.parent?.id == element.id }
    }

    /**
     * 查找包声明
     *
     * @param element 文件元素
     * @return 包声明元素，如果没有则返回 null
     */
    private fun findPackageDeclaration(element: CodeElement): CodeElement? {
        // 查找文件的包声明
        return element.children.find { it.type == CodeElementType.PACKAGE_DECLARATION }
    }

    /**
     * 查找导入语句
     *
     * @param element 文件元素
     * @return 导入语句元素列表
     */
    private fun findImports(element: CodeElement): List<CodeElement> {
        // 查找文件的导入语句
        return element.children.filter { it.type == CodeElementType.IMPORT_DECLARATION }
    }

    /**
     * 创建空的流图
     *
     * @param element 代码元素
     * @return 空流图
     */
    private fun createEmptyFlowGraph(element: CodeElement): FlowGraph {
        return FlowGraph(
            id = UUID.randomUUID().toString(),
            name = "${element.name} Java/Kotlin 流图 (空)",
            type = FlowType.CONTROL_FLOW,
            metadata = mutableMapOf(
                "elementId" to element.id,
                "elementType" to element.type.name,
                "language" to element.language,
                "isEmpty" to true
            )
        )
    }
}
