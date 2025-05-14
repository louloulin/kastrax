package ai.kastrax.codebase.semantic.flow

import ai.kastrax.codebase.semantic.model.CodeElement
import java.util.UUID

/**
 * 流图
 *
 * @property id 图ID
 * @property name 图名称
 * @property type 流类型
 * @property element 代码元素
 * @property nodes 节点列表
 * @property edges 边列表
 * @property entryNode 入口节点
 * @property exitNodes 出口节点列表
 * @property metadata 元数据
 */
data class FlowGraph(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "",
    val type: FlowType,
    val element: CodeElement,
    val nodes: MutableList<FlowNode> = mutableListOf(),
    val edges: MutableList<FlowEdge> = mutableListOf(),
    var entryNode: FlowNode? = null,
    val exitNodes: MutableList<FlowNode> = mutableListOf(),
    val metadata: MutableMap<String, String> = mutableMapOf()
) {
    /**
     * 添加节点
     *
     * @param node 节点
     * @return 是否成功添加
     */
    fun addNode(node: FlowNode): Boolean {
        return nodes.add(node)
    }

    /**
     * 添加边
     *
     * @param edge 边
     * @return 是否成功添加
     */
    fun addEdge(edge: FlowEdge): Boolean {
        return edges.add(edge)
    }

    /**
     * 添加边
     *
     * @param source 源节点
     * @param target 目标节点
     * @param type 边类型
     * @param label 边标签
     * @return 创建的边
     */
    fun addEdge(source: FlowNode, target: FlowNode, type: FlowEdgeType, label: String = ""): FlowEdge {
        val edge = FlowEdge(
            id = "${source.id}->${target.id}",
            type = type,
            source = source.id,
            target = target.id,
            label = label
        )
        edges.add(edge)
        return edge
    }

    /**
     * 设置入口节点
     *
     * @param node 入口节点
     */
    fun setGraphEntryNode(node: FlowNode) {
        entryNode = node
        nodes.add(node)
    }

    /**
     * 添加出口节点
     *
     * @param node 出口节点
     * @return 是否成功添加
     */
    fun addExitNode(node: FlowNode): Boolean {
        nodes.add(node)
        return exitNodes.add(node)
    }

    /**
     * 获取节点
     *
     * @param nodeId 节点ID
     * @return 节点，如果不存在则返回null
     */
    fun getNode(nodeId: String): FlowNode? {
        return nodes.find { it.id == nodeId }
    }

    /**
     * 获取出边
     *
     * @param nodeId 节点ID
     * @return 出边列表
     */
    fun getOutEdges(nodeId: String): List<FlowEdge> {
        return edges.filter { it.source == nodeId }
    }

    /**
     * 获取入边
     *
     * @param nodeId 节点ID
     * @return 入边列表
     */
    fun getInEdges(nodeId: String): List<FlowEdge> {
        return edges.filter { it.target == nodeId }
    }

    /**
     * 获取后继节点
     *
     * @param nodeId 节点ID
     * @return 后继节点列表
     */
    fun getSuccessors(nodeId: String): List<FlowNode> {
        return getOutEdges(nodeId).mapNotNull { getNode(it.target) }
    }

    /**
     * 获取前驱节点
     *
     * @param nodeId 节点ID
     * @return 前驱节点列表
     */
    fun getPredecessors(nodeId: String): List<FlowNode> {
        return getInEdges(nodeId).mapNotNull { getNode(it.source) }
    }
}
