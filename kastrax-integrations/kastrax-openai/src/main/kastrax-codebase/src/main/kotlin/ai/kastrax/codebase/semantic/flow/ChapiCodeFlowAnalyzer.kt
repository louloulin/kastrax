package ai.kastrax.codebase.semantic.flow

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * 基于 Chapi 的代码流分析器
 *
 * 使用 Chapi 的代码结构分析控制流和数据流
 */
class ChapiCodeFlowAnalyzer : CodeFlowAnalyzer {
    private val nodeIdCounter = AtomicInteger(0)
    
    /**
     * 分析代码元素的流图
     *
     * @param element 代码元素
     * @return 流图
     */
    override suspend fun analyzeFlow(element: CodeElement): FlowGraph = withContext(Dispatchers.Default) {
        logger.debug { "开始分析代码流: ${element.qualifiedName}" }
        
        val flowGraph = FlowGraph(id = "flow-${element.id}")
        
        try {
            when (element.type) {
                CodeElementType.METHOD, CodeElementType.CONSTRUCTOR -> analyzeMethodFlow(element, flowGraph)
                CodeElementType.CLASS, CodeElementType.INTERFACE, CodeElementType.ENUM -> analyzeClassFlow(element, flowGraph)
                CodeElementType.FILE -> analyzeFileFlow(element, flowGraph)
                else -> {
                    logger.warn { "不支持分析的代码元素类型: ${element.type}" }
                    // 创建一个简单的流图，只包含入口和出口节点
                    val entryNode = createNode(FlowNodeType.ENTRY, element)
                    val exitNode = createNode(FlowNodeType.EXIT, element)
                    
                    flowGraph.addNode(entryNode)
                    flowGraph.addNode(exitNode)
                    flowGraph.addEdge(FlowEdge(
                        id = "edge-${entryNode.id}-${exitNode.id}",
                        source = entryNode.id,
                        target = exitNode.id,
                        type = FlowEdgeType.SEQUENTIAL
                    ))
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "分析代码流时发生错误: ${element.qualifiedName}" }
            // 确保流图至少包含入口和出口节点
            if (flowGraph.nodes.isEmpty()) {
                val entryNode = createNode(FlowNodeType.ENTRY, element)
                val exitNode = createNode(FlowNodeType.EXIT, element)
                
                flowGraph.addNode(entryNode)
                flowGraph.addNode(exitNode)
                flowGraph.addEdge(FlowEdge(
                    id = "edge-${entryNode.id}-${exitNode.id}",
                    source = entryNode.id,
                    target = exitNode.id,
                    type = FlowEdgeType.SEQUENTIAL
                ))
            }
        }
        
        logger.debug { "代码流分析完成: ${element.qualifiedName}, 节点数: ${flowGraph.nodes.size}, 边数: ${flowGraph.edges.size}" }
        return@withContext flowGraph
    }
    
    /**
     * 获取支持的代码元素类型
     *
     * @return 支持的代码元素类型集合
     */
    override fun getSupportedElementTypes(): Set<CodeElementType> {
        return setOf(
            CodeElementType.METHOD,
            CodeElementType.CONSTRUCTOR,
            CodeElementType.CLASS,
            CodeElementType.INTERFACE,
            CodeElementType.ENUM,
            CodeElementType.FILE
        )
    }
    
    /**
     * 分析方法的流图
     *
     * @param method 方法元素
     * @param flowGraph 流图
     */
    private fun analyzeMethodFlow(method: CodeElement, flowGraph: FlowGraph) {
        // 创建入口和出口节点
        val entryNode = createNode(FlowNodeType.ENTRY, method)
        val exitNode = createNode(FlowNodeType.EXIT, method)
        
        flowGraph.addNode(entryNode)
        flowGraph.addNode(exitNode)
        
        // 处理参数
        val paramNodes = method.findChildren { it.type == CodeElementType.PARAMETER }
            .map { createNode(FlowNodeType.DECLARATION, it) }
        
        paramNodes.forEach { flowGraph.addNode(it) }
        
        // 如果有参数节点，则连接入口节点到第一个参数节点
        if (paramNodes.isNotEmpty()) {
            flowGraph.addEdge(FlowEdge(
                id = "edge-${entryNode.id}-${paramNodes.first().id}",
                source = entryNode.id,
                target = paramNodes.first().id,
                type = FlowEdgeType.SEQUENTIAL
            ))
            
            // 连接参数节点
            for (i in 0 until paramNodes.size - 1) {
                flowGraph.addEdge(FlowEdge(
                    id = "edge-${paramNodes[i].id}-${paramNodes[i + 1].id}",
                    source = paramNodes[i].id,
                    target = paramNodes[i + 1].id,
                    type = FlowEdgeType.SEQUENTIAL
                ))
            }
            
            // 连接最后一个参数节点到出口节点
            flowGraph.addEdge(FlowEdge(
                id = "edge-${paramNodes.last().id}-${exitNode.id}",
                source = paramNodes.last().id,
                target = exitNode.id,
                type = FlowEdgeType.SEQUENTIAL
            ))
        } else {
            // 如果没有参数节点，则直接连接入口节点到出口节点
            flowGraph.addEdge(FlowEdge(
                id = "edge-${entryNode.id}-${exitNode.id}",
                source = entryNode.id,
                target = exitNode.id,
                type = FlowEdgeType.SEQUENTIAL
            ))
        }
        
        // 添加方法调用关系
        // 注意：这里只是一个简化的实现，实际上需要更复杂的静态分析
        val methodName = method.name
        val methodCalls = findMethodCalls(method)
        
        methodCalls.forEach { calledMethod ->
            val callNode = createNode(FlowNodeType.CALL, calledMethod)
            flowGraph.addNode(callNode)
            
            // 添加数据依赖边
            flowGraph.addEdge(FlowEdge(
                id = "edge-data-${method.id}-${calledMethod.id}",
                source = entryNode.id,
                target = callNode.id,
                type = FlowEdgeType.DATA_DEPENDENCY
            ))
        }
    }
    
    /**
     * 分析类的流图
     *
     * @param clazz 类元素
     * @param flowGraph 流图
     */
    private fun analyzeClassFlow(clazz: CodeElement, flowGraph: FlowGraph) {
        // 创建类节点
        val classNode = createNode(FlowNodeType.DECLARATION, clazz)
        flowGraph.addNode(classNode)
        
        // 处理字段
        val fieldNodes = clazz.findChildren { it.type == CodeElementType.FIELD }
            .map { createNode(FlowNodeType.DECLARATION, it) }
        
        fieldNodes.forEach { flowGraph.addNode(it) }
        
        // 处理方法
        val methodNodes = clazz.findChildren { it.type == CodeElementType.METHOD || it.type == CodeElementType.CONSTRUCTOR }
            .map { createNode(FlowNodeType.DECLARATION, it) }
        
        methodNodes.forEach { flowGraph.addNode(it) }
        
        // 连接类节点到字段节点
        fieldNodes.forEach { fieldNode ->
            flowGraph.addEdge(FlowEdge(
                id = "edge-${classNode.id}-${fieldNode.id}",
                source = classNode.id,
                target = fieldNode.id,
                type = FlowEdgeType.SEQUENTIAL
            ))
        }
        
        // 连接类节点到方法节点
        methodNodes.forEach { methodNode ->
            flowGraph.addEdge(FlowEdge(
                id = "edge-${classNode.id}-${methodNode.id}",
                source = classNode.id,
                target = methodNode.id,
                type = FlowEdgeType.SEQUENTIAL
            ))
        }
        
        // 分析方法之间的调用关系
        val methods = clazz.findChildren { it.type == CodeElementType.METHOD || it.type == CodeElementType.CONSTRUCTOR }
        
        for (method in methods) {
            val methodCalls = findMethodCalls(method)
            
            for (calledMethod in methodCalls) {
                // 如果被调用的方法也在这个类中
                if (calledMethod.parent?.id == clazz.id) {
                    val sourceNode = methodNodes.find { it.element?.id == method.id }
                    val targetNode = methodNodes.find { it.element?.id == calledMethod.id }
                    
                    if (sourceNode != null && targetNode != null) {
                        flowGraph.addEdge(FlowEdge(
                            id = "edge-call-${sourceNode.id}-${targetNode.id}",
                            source = sourceNode.id,
                            target = targetNode.id,
                            type = FlowEdgeType.CALL
                        ))
                    }
                }
            }
        }
    }
    
    /**
     * 分析文件的流图
     *
     * @param file 文件元素
     * @param flowGraph 流图
     */
    private fun analyzeFileFlow(file: CodeElement, flowGraph: FlowGraph) {
        // 创建文件节点
        val fileNode = createNode(FlowNodeType.ENTRY, file)
        flowGraph.addNode(fileNode)
        
        // 处理导入语句
        val importNodes = file.findChildren { it.type == CodeElementType.IMPORT }
            .map { createNode(FlowNodeType.DECLARATION, it) }
        
        importNodes.forEach { flowGraph.addNode(it) }
        
        // 处理类、接口和枚举
        val typeNodes = file.findChildren { 
            it.type == CodeElementType.CLASS || 
            it.type == CodeElementType.INTERFACE || 
            it.type == CodeElementType.ENUM 
        }.map { createNode(FlowNodeType.DECLARATION, it) }
        
        typeNodes.forEach { flowGraph.addNode(it) }
        
        // 连接文件节点到导入节点
        importNodes.forEach { importNode ->
            flowGraph.addEdge(FlowEdge(
                id = "edge-${fileNode.id}-${importNode.id}",
                source = fileNode.id,
                target = importNode.id,
                type = FlowEdgeType.SEQUENTIAL
            ))
        }
        
        // 连接文件节点到类型节点
        typeNodes.forEach { typeNode ->
            flowGraph.addEdge(FlowEdge(
                id = "edge-${fileNode.id}-${typeNode.id}",
                source = fileNode.id,
                target = typeNode.id,
                type = FlowEdgeType.SEQUENTIAL
            ))
        }
        
        // 分析类之间的依赖关系
        val types = file.findChildren { 
            it.type == CodeElementType.CLASS || 
            it.type == CodeElementType.INTERFACE || 
            it.type == CodeElementType.ENUM 
        }
        
        for (type in types) {
            // 查找字段类型依赖
            val fields = type.findChildren { it.type == CodeElementType.FIELD }
            for (field in fields) {
                val fieldType = field.metadata["type"] as? String
                if (fieldType != null) {
                    // 查找是否有匹配的类型
                    val dependentType = types.find { it.name == fieldType }
                    if (dependentType != null) {
                        val sourceNode = typeNodes.find { it.element?.id == type.id }
                        val targetNode = typeNodes.find { it.element?.id == dependentType.id }
                        
                        if (sourceNode != null && targetNode != null) {
                            flowGraph.addEdge(FlowEdge(
                                id = "edge-dep-${sourceNode.id}-${targetNode.id}",
                                source = sourceNode.id,
                                target = targetNode.id,
                                type = FlowEdgeType.DATA_DEPENDENCY
                            ))
                        }
                    }
                }
            }
        }
    }
    
    /**
     * 查找方法调用
     *
     * @param method 方法元素
     * @return 被调用的方法列表
     */
    private fun findMethodCalls(method: CodeElement): List<CodeElement> {
        // 注意：这里只是一个简化的实现，实际上需要更复杂的静态分析
        // 在实际实现中，我们需要解析方法体，识别方法调用
        return emptyList()
    }
    
    /**
     * 创建流节点
     *
     * @param type 节点类型
     * @param element 关联的代码元素
     * @return 流节点
     */
    private fun createNode(type: FlowNodeType, element: CodeElement?): FlowNode {
        val id = "node-${nodeIdCounter.incrementAndGet()}"
        return FlowNode(
            id = id,
            type = type,
            element = element
        )
    }
}
