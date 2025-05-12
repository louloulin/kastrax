package ai.kastrax.codebase.semantic.flow.impl

import ai.kastrax.codebase.semantic.flow.*
import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import java.util.*

/**
 * 代码流分析器实现
 *
 * 集成控制流和数据流分析，提供统一的代码流分析接口
 *
 * @property controlFlowAnalyzer 控制流分析器
 * @property dataFlowAnalyzer 数据流分析器
 * @property config 配置
 */
class CodeFlowAnalyzerImpl(
    private val controlFlowAnalyzer: ControlFlowAnalyzerImpl = ControlFlowAnalyzerImpl(),
    private val dataFlowAnalyzer: DataFlowAnalyzerImpl = DataFlowAnalyzerImpl(),
    private val config: CodeFlowAnalyzerConfig = CodeFlowAnalyzerConfig()
) : CodeFlowAnalyzer {
    private val logger = KotlinLogging.logger {}

    override suspend fun analyzeFlow(element: CodeElement): FlowGraph = withContext(Dispatchers.Default) {
        logger.info { "开始综合分析代码流: ${element.qualifiedName}" }

        if (!supportsElement(element)) {
            logger.warn { "不支持的代码元素类型: ${element.type}" }
            return@withContext createEmptyFlowGraph(element)
        }

        try {
            // 根据配置决定分析哪些流类型
            if (config.analyzeControlFlow && config.analyzeDataFlow) {
                // 并行分析控制流和数据流
                val combinedGraph = coroutineScope {
                    val controlFlowDeferred = async { controlFlowAnalyzer.analyzeFlow(element) }
                    val dataFlowDeferred = async { dataFlowAnalyzer.analyzeFlow(element) }
                    
                    val controlFlowGraph = controlFlowDeferred.await()
                    val dataFlowGraph = dataFlowDeferred.await()
                    
                    // 合并两个流图
                    mergeFlowGraphs(controlFlowGraph, dataFlowGraph, element)
                }
                
                logger.info { "综合代码流分析完成: ${element.qualifiedName}, 节点数: ${combinedGraph.nodes.size}, 边数: ${combinedGraph.edges.size}" }
                return@withContext combinedGraph
            } else if (config.analyzeControlFlow) {
                // 只分析控制流
                return@withContext controlFlowAnalyzer.analyzeFlow(element)
            } else if (config.analyzeDataFlow) {
                // 只分析数据流
                return@withContext dataFlowAnalyzer.analyzeFlow(element)
            } else {
                logger.warn { "未启用任何流分析类型" }
                return@withContext createEmptyFlowGraph(element)
            }
        } catch (e: Exception) {
            logger.error(e) { "综合分析代码流时出错: ${element.qualifiedName}" }
            return@withContext createEmptyFlowGraph(element)
        }
    }

    override fun getSupportedElementTypes(): Set<CodeElementType> {
        // 取两个分析器支持的元素类型的交集
        return controlFlowAnalyzer.getSupportedElementTypes().intersect(dataFlowAnalyzer.getSupportedElementTypes())
    }

    /**
     * 合并控制流图和数据流图
     *
     * @param controlFlowGraph 控制流图
     * @param dataFlowGraph 数据流图
     * @param element 代码元素
     * @return 合并后的流图
     */
    private fun mergeFlowGraphs(controlFlowGraph: FlowGraph, dataFlowGraph: FlowGraph, element: CodeElement): FlowGraph {
        // 创建合并后的流图
        val mergedGraph = FlowGraph(
            id = UUID.randomUUID().toString(),
            name = "${element.name} 综合流图",
            type = FlowType.CONTROL_FLOW, // 主要类型设为控制流
            metadata = mutableMapOf(
                "elementId" to element.id,
                "elementType" to element.type.name,
                "language" to element.language,
                "hasControlFlow" to true,
                "hasDataFlow" to true
            )
        )
        
        // 节点ID映射 (原图ID -> 新图ID)
        val controlNodeIdMap = mutableMapOf<String, String>()
        val dataNodeIdMap = mutableMapOf<String, String>()
        
        // 复制控制流图的节点
        for ((nodeId, node) in controlFlowGraph.nodes) {
            val newNodeId = UUID.randomUUID().toString()
            val newNode = node.copy(
                id = newNodeId,
                metadata = node.metadata.toMutableMap().apply {
                    put("originalId", nodeId)
                    put("flowType", "CONTROL_FLOW")
                }
            )
            mergedGraph.addNode(newNode)
            controlNodeIdMap[nodeId] = newNodeId
            
            // 设置入口节点
            if (nodeId == controlFlowGraph.entryNodeId) {
                mergedGraph.setEntryNode(newNodeId)
            }
            
            // 设置出口节点
            if (nodeId in controlFlowGraph.exitNodeIds) {
                mergedGraph.addExitNode(newNodeId)
            }
        }
        
        // 复制数据流图的节点
        for ((nodeId, node) in dataFlowGraph.nodes) {
            // 检查是否已经有相同元素的节点
            val existingNodeId = mergedGraph.nodes.values
                .find { it.element?.id == node.element?.id && it.type == node.type }
                ?.id
            
            if (existingNodeId != null) {
                // 如果已经存在相同元素的节点，使用现有节点
                dataNodeIdMap[nodeId] = existingNodeId
            } else {
                // 否则创建新节点
                val newNodeId = UUID.randomUUID().toString()
                val newNode = node.copy(
                    id = newNodeId,
                    metadata = node.metadata.toMutableMap().apply {
                        put("originalId", nodeId)
                        put("flowType", "DATA_FLOW")
                    }
                )
                mergedGraph.addNode(newNode)
                dataNodeIdMap[nodeId] = newNodeId
            }
        }
        
        // 复制控制流图的边
        for ((edgeId, edge) in controlFlowGraph.edges) {
            val newSourceId = controlNodeIdMap[edge.sourceId] ?: continue
            val newTargetId = controlNodeIdMap[edge.targetId] ?: continue
            
            val newEdgeId = UUID.randomUUID().toString()
            val newEdge = edge.copy(
                id = newEdgeId,
                sourceId = newSourceId,
                targetId = newTargetId,
                metadata = edge.metadata.toMutableMap().apply {
                    put("originalId", edgeId)
                    put("flowType", "CONTROL_FLOW")
                }
            )
            mergedGraph.addEdge(newEdge)
        }
        
        // 复制数据流图的边
        for ((edgeId, edge) in dataFlowGraph.edges) {
            val newSourceId = dataNodeIdMap[edge.sourceId] ?: continue
            val newTargetId = dataNodeIdMap[edge.targetId] ?: continue
            
            // 避免重复边
            if (mergedGraph.getEdgesBetween(newSourceId, newTargetId).isNotEmpty()) {
                continue
            }
            
            val newEdgeId = UUID.randomUUID().toString()
            val newEdge = edge.copy(
                id = newEdgeId,
                sourceId = newSourceId,
                targetId = newTargetId,
                metadata = edge.metadata.toMutableMap().apply {
                    put("originalId", edgeId)
                    put("flowType", "DATA_FLOW")
                }
            )
            mergedGraph.addEdge(newEdge)
        }
        
        return mergedGraph
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
            name = "${element.name} 综合流图 (空)",
            type = FlowType.CONTROL_FLOW,
            metadata = mutableMapOf(
                "elementId" to element.id,
                "elementType" to element.type.name,
                "language" to element.language,
                "isEmpty" to true,
                "hasControlFlow" to config.analyzeControlFlow,
                "hasDataFlow" to config.analyzeDataFlow
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
}
