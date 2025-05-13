package ai.kastrax.codebase.semantic.flow

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * 代码流分析器配置
 *
 * @property maxDepth 最大分析深度
 * @property analyzeControlFlow 是否分析控制流
 * @property analyzeDataFlow 是否分析数据流
 * @property includeLibraries 是否包含库代码
 * @property maxConcurrentTasks 最大并发任务数
 * @property enableParallelAnalysis 是否启用并行分析
 * @property maxMethodSize 最大方法大小（行数）
 * @property enableIncrementalAnalysis 是否启用增量分析
 * @property enableCaching 是否启用缓存
 */
data class CodeFlowAnalyzerConfig(
    val maxDepth: Int = 10,
    val analyzeControlFlow: Boolean = true,
    val analyzeDataFlow: Boolean = true,
    val includeLibraries: Boolean = false,
    val maxConcurrentTasks: Int = 10,
    val enableParallelAnalysis: Boolean = true,
    val maxMethodSize: Int = 1000,
    val enableIncrementalAnalysis: Boolean = true,
    val enableCaching: Boolean = true
)

/**
 * 代码流类型
 */
enum class FlowType {
    CONTROL_FLOW,
    DATA_FLOW
}

/**
 * 代码流节点类型
 */
enum class FlowNodeType {
    ENTRY,
    EXIT,
    STATEMENT,
    CONDITION,
    LOOP,
    BRANCH,
    MERGE,
    CALL,
    RETURN,
    THROW,
    CATCH,
    FINALLY,
    ASSIGNMENT,
    DECLARATION,
    REFERENCE,
    UNKNOWN
}

/**
 * 代码流节点
 *
 * @property id 唯一标识符
 * @property type 节点类型
 * @property element 关联的代码元素
 * @property metadata 元数据
 */
data class FlowNode(
    val id: String,
    val type: FlowNodeType,
    val element: CodeElement? = null,
    val metadata: MutableMap<String, Any> = mutableMapOf()
)

/**
 * 代码流边类型
 */
enum class FlowEdgeType {
    SEQUENTIAL,
    CONDITIONAL_TRUE,
    CONDITIONAL_FALSE,
    LOOP_BACK,
    EXCEPTION,
    CALL,
    RETURN,
    DATA_DEPENDENCY,
    CONTROL_DEPENDENCY,
    UNKNOWN
}

/**
 * 代码流边
 *
 * @property id 唯一标识符
 * @property sourceId 源节点ID
 * @property targetId 目标节点ID
 * @property type 边类型
 * @property metadata 元数据
 */
data class FlowEdge(
    val id: String,
    val sourceId: String,
    val targetId: String,
    val type: FlowEdgeType,
    val metadata: MutableMap<String, Any> = mutableMapOf()
)

/**
 * 代码流图
 *
 * @property id 唯一标识符
 * @property name 名称
 * @property type 流类型
 * @property nodes 节点集合
 * @property edges 边集合
 * @property entryNodeId 入口节点ID
 * @property exitNodeIds 出口节点ID集合
 * @property metadata 元数据
 */
data class FlowGraph(
    val id: String,
    val name: String,
    val type: FlowType,
    val nodes: MutableMap<String, FlowNode> = mutableMapOf(),
    val edges: MutableMap<String, FlowEdge> = mutableMapOf(),
    var entryNodeId: String? = null,
    val exitNodeIds: MutableSet<String> = mutableSetOf(),
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * 添加节点
     *
     * @param node 节点
     * @return 是否成功添加
     */
    fun addNode(node: FlowNode): Boolean {
        if (nodes.containsKey(node.id)) {
            return false
        }
        nodes[node.id] = node
        return true
    }

    /**
     * 添加边
     *
     * @param edge 边
     * @return 是否成功添加
     */
    fun addEdge(edge: FlowEdge): Boolean {
        if (edges.containsKey(edge.id)) {
            return false
        }
        if (!nodes.containsKey(edge.sourceId) || !nodes.containsKey(edge.targetId)) {
            return false
        }
        edges[edge.id] = edge
        return true
    }

    /**
     * 设置入口节点
     *
     * @param nodeId 节点ID
     * @return 是否成功设置
     */
    fun setEntryNode(nodeId: String): Boolean {
        if (!nodes.containsKey(nodeId)) {
            return false
        }
        entryNodeId = nodeId
        return true
    }

    /**
     * 添加出口节点
     *
     * @param nodeId 节点ID
     * @return 是否成功添加
     */
    fun addExitNode(nodeId: String): Boolean {
        if (!nodes.containsKey(nodeId)) {
            return false
        }
        exitNodeIds.add(nodeId)
        return true
    }

    /**
     * 获取节点的后继节点
     *
     * @param nodeId 节点ID
     * @return 后继节点ID集合
     */
    fun getSuccessors(nodeId: String): Set<String> {
        return edges.values
            .filter { it.sourceId == nodeId }
            .map { it.targetId }
            .toSet()
    }

    /**
     * 获取节点的前驱节点
     *
     * @param nodeId 节点ID
     * @return 前驱节点ID集合
     */
    fun getPredecessors(nodeId: String): Set<String> {
        return edges.values
            .filter { it.targetId == nodeId }
            .map { it.sourceId }
            .toSet()
    }

    /**
     * 获取节点间的边
     *
     * @param sourceId 源节点ID
     * @param targetId 目标节点ID
     * @return 边集合
     */
    fun getEdgesBetween(sourceId: String, targetId: String): List<FlowEdge> {
        return edges.values
            .filter { it.sourceId == sourceId && it.targetId == targetId }
            .toList()
    }

    /**
     * 获取节点的出边
     *
     * @param nodeId 节点ID
     * @return 出边集合
     */
    fun getOutgoingEdges(nodeId: String): List<FlowEdge> {
        return edges.values
            .filter { it.sourceId == nodeId }
            .toList()
    }

    /**
     * 获取节点的入边
     *
     * @param nodeId 节点ID
     * @return 入边集合
     */
    fun getIncomingEdges(nodeId: String): List<FlowEdge> {
        return edges.values
            .filter { it.targetId == nodeId }
            .toList()
    }
}

/**
 * 代码流分析器接口
 *
 * 定义代码流分析器的通用接口，用于分析代码的控制流和数据流
 */
interface CodeFlowAnalyzer {
    /**
     * 分析代码元素的流图
     *
     * @param element 代码元素
     * @return 流图
     */
    suspend fun analyzeFlow(element: CodeElement): FlowGraph

    /**
     * 分析指定类型的代码流
     *
     * @param element 代码元素
     * @param type 流类型
     * @return 流图
     */
    suspend fun analyzeFlow(element: CodeElement, type: FlowType): FlowGraph {
        // 默认实现，子类可以重写
        val graph = analyzeFlow(element)
        return if (graph.type == type) {
            graph
        } else {
            // 创建一个新的空图
            FlowGraph(
                id = java.util.UUID.randomUUID().toString(),
                name = "${element.name} ${type.name}",
                type = type
            )
        }
    }

    /**
     * 获取支持的代码元素类型
     *
     * @return 支持的代码元素类型集合
     */
    fun getSupportedElementTypes(): Set<CodeElementType>

    /**
     * 检查是否支持指定代码元素
     *
     * @param element 代码元素
     * @return 是否支持
     */
    fun supportsElement(element: CodeElement): Boolean {
        return element.type in getSupportedElementTypes()
    }

    /**
     * 获取支持的流类型
     *
     * @return 支持的流类型集合
     */
    fun getSupportedFlowTypes(): Set<FlowType> {
        // 默认支持所有流类型
        return FlowType.values().toSet()
    }

    /**
     * 检查是否支持指定流类型
     *
     * @param type 流类型
     * @return 是否支持
     */
    fun supportsFlowType(type: FlowType): Boolean {
        return type in getSupportedFlowTypes()
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        // 默认实现为空
    }
}
