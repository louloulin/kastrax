package ai.kastrax.actor.network.topology

import ai.kastrax.actor.network.AgentRelation
import ai.kastrax.actor.network.NetworkTopology
import java.util.concurrent.ConcurrentHashMap

/**
 * 层次网络拓扑，支持多层次的 Agent 组织结构
 *
 * 层次网络拓扑实现了多层次的 Agent 组织结构，支持父子关系和层级查询，
 * 适用于需要明确层级关系的场景，如组织结构、决策流程等
 */
class HierarchicalTopology {
    private val topology = NetworkTopology()

    /**
     * 节点层级映射，记录每个节点所在的层级
     */
    private val nodeLevels = ConcurrentHashMap<String, Int>()

    /**
     * 父节点映射，记录每个节点的父节点
     */
    private val parentNodes = ConcurrentHashMap<String, String>()

    /**
     * 子节点映射，记录每个节点的子节点列表
     */
    private val childNodes = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * 添加根节点
     *
     * @param nodeId 节点 ID
     */
    fun addRootNode(nodeId: String) {
        topology.addNode(nodeId)
        nodeLevels[nodeId] = 0
    }

    /**
     * 添加子节点
     *
     * @param parentId 父节点 ID
     * @param childId 子节点 ID
     * @return 是否成功添加
     */
    fun addChildNode(parentId: String, childId: String): Boolean {
        // 检查父节点是否存在
        if (!containsNode(parentId)) {
            return false
        }

        // 添加子节点
        topology.addNode(childId)

        // 设置父子关系
        val parentLevel = nodeLevels[parentId] ?: 0
        nodeLevels[childId] = parentLevel + 1
        parentNodes[childId] = parentId
        childNodes.getOrPut(parentId) { mutableSetOf() }.add(childId)

        // 添加边，父节点是主节点，子节点是从节点
        topology.addEdge(parentId, childId, AgentRelation.MASTER)

        return true
    }

    /**
     * 移除节点及其所有子节点
     *
     * @param nodeId 节点 ID
     */
    fun removeNode(nodeId: String) {
        // 递归移除所有子节点
        val children = getChildNodes(nodeId)
        children.forEach { removeNode(it) }

        // 从父节点的子节点列表中移除
        val parentId = parentNodes[nodeId]
        if (parentId != null) {
            childNodes[parentId]?.remove(nodeId)
            if (childNodes[parentId]?.isEmpty() == true) {
                childNodes.remove(parentId)
            }
        }

        // 移除节点相关的记录
        nodeLevels.remove(nodeId)
        parentNodes.remove(nodeId)
        childNodes.remove(nodeId)

        // 移除节点
        topology.removeNode(nodeId)
    }

    /**
     * 获取节点的层级
     *
     * @param nodeId 节点 ID
     * @return 层级，如果节点不存在则返回 -1
     */
    fun getNodeLevel(nodeId: String): Int {
        return nodeLevels[nodeId] ?: -1
    }

    /**
     * 获取节点的父节点
     *
     * @param nodeId 节点 ID
     * @return 父节点 ID，如果没有父节点则返回 null
     */
    fun getParentNode(nodeId: String): String? {
        return parentNodes[nodeId]
    }

    /**
     * 获取节点的子节点
     *
     * @param nodeId 节点 ID
     * @return 子节点 ID 列表
     */
    fun getChildNodes(nodeId: String): List<String> {
        return childNodes[nodeId]?.toList() ?: emptyList()
    }

    /**
     * 获取节点的所有后代节点
     *
     * @param nodeId 节点 ID
     * @return 后代节点 ID 列表
     */
    fun getDescendantNodes(nodeId: String): List<String> {
        val descendants = mutableListOf<String>()

        // 递归获取所有后代节点
        fun collectDescendants(currentNodeId: String) {
            val children = getChildNodes(currentNodeId)
            descendants.addAll(children)
            children.forEach { collectDescendants(it) }
        }

        collectDescendants(nodeId)
        return descendants
    }

    /**
     * 获取节点的所有祖先节点
     *
     * @param nodeId 节点 ID
     * @return 祖先节点 ID 列表，从近到远排序
     */
    fun getAncestorNodes(nodeId: String): List<String> {
        val ancestors = mutableListOf<String>()
        var currentNodeId = nodeId

        while (true) {
            val parentId = getParentNode(currentNodeId) ?: break
            ancestors.add(parentId)
            currentNodeId = parentId
        }

        return ancestors
    }

    /**
     * 获取指定层级的所有节点
     *
     * @param level 层级
     * @return 该层级的节点 ID 列表
     */
    fun getNodesAtLevel(level: Int): List<String> {
        return nodeLevels.entries
            .filter { it.value == level }
            .map { it.key }
    }

    /**
     * 获取两个节点的最近公共祖先
     *
     * @param nodeId1 节点 ID 1
     * @param nodeId2 节点 ID 2
     * @return 最近公共祖先的节点 ID，如果不存在则返回 null
     */
    fun getLowestCommonAncestor(nodeId1: String, nodeId2: String): String? {
        val ancestors1 = getAncestorNodes(nodeId1) + nodeId1
        val ancestors2 = getAncestorNodes(nodeId2) + nodeId2

        val commonAncestors = ancestors1.intersect(ancestors2.toSet())
        return commonAncestors.firstOrNull()
    }

    /**
     * 获取层次结构的最大深度
     *
     * @return 最大深度
     */
    fun getMaxDepth(): Int {
        return nodeLevels.values.maxOrNull() ?: 0
    }

    /**
     * 获取层次结构的根节点
     *
     * @return 根节点 ID 列表
     */
    fun getRootNodes(): List<String> {
        return getNodesAtLevel(0)
    }

    /**
     * 检查节点是否是叶子节点（没有子节点）
     *
     * @param nodeId 节点 ID
     * @return 是否是叶子节点
     */
    fun isLeafNode(nodeId: String): Boolean {
        return getChildNodes(nodeId).isEmpty()
    }

    /**
     * 获取所有叶子节点
     *
     * @return 叶子节点 ID 列表
     */
    fun getLeafNodes(): List<String> {
        return getAllNodes().filter { isLeafNode(it) }
    }

    /**
     * 移动节点到新的父节点
     *
     * @param nodeId 要移动的节点 ID
     * @param newParentId 新父节点 ID
     * @return 是否成功移动
     */
    fun moveNode(nodeId: String, newParentId: String): Boolean {
        // 检查节点是否存在
        if (!containsNode(nodeId) || !containsNode(newParentId)) {
            return false
        }

        // 检查是否形成循环
        if (getDescendantNodes(nodeId).contains(newParentId)) {
            return false
        }

        // 从原父节点移除
        val oldParentId = getParentNode(nodeId)
        if (oldParentId != null) {
            childNodes[oldParentId]?.remove(nodeId)
            if (childNodes[oldParentId]?.isEmpty() == true) {
                childNodes.remove(oldParentId)
            }

            // 移除原有的边
            topology.removeEdge(oldParentId, nodeId)
        }

        // 添加到新父节点
        val newParentLevel = nodeLevels[newParentId] ?: 0
        nodeLevels[nodeId] = newParentLevel + 1
        parentNodes[nodeId] = newParentId
        childNodes.getOrPut(newParentId) { mutableSetOf() }.add(nodeId)

        // 添加新的边
        topology.addEdge(newParentId, nodeId, AgentRelation.MASTER)

        // 更新所有后代节点的层级
        updateDescendantLevels(nodeId)

        return true
    }

    /**
     * 更新节点及其所有后代节点的层级
     *
     * @param nodeId 节点 ID
     */
    private fun updateDescendantLevels(nodeId: String) {
        val nodeLevel = nodeLevels[nodeId] ?: return

        // 递归更新所有后代节点的层级
        fun updateLevels(currentNodeId: String, currentLevel: Int) {
            nodeLevels[currentNodeId] = currentLevel

            val children = getChildNodes(currentNodeId)
            children.forEach { updateLevels(it, currentLevel + 1) }
        }

        getChildNodes(nodeId).forEach { updateLevels(it, nodeLevel + 1) }
    }

    /**
     * 检查节点是否存在
     *
     * @param nodeId 节点 ID
     * @return 是否存在
     */
    fun containsNode(nodeId: String): Boolean {
        return topology.getAllNodes().contains(nodeId)
    }

    // 委托方法
    fun addNode(nodeId: String) = topology.addNode(nodeId)
    fun addEdge(fromId: String, toId: String, relation: AgentRelation) = topology.addEdge(fromId, toId, relation)
    fun removeEdge(fromId: String, toId: String) = topology.removeEdge(fromId, toId)
    fun getConnectedNodes(nodeId: String) = topology.getConnectedNodes(nodeId)
    fun getNodesByRelation(nodeId: String, relation: AgentRelation) = topology.getNodesByRelation(nodeId, relation)
    fun getRelation(fromId: String, toId: String) = topology.getRelation(fromId, toId)
    fun getAllNodes() = topology.getAllNodes()
    fun getAllEdges() = topology.getAllEdges()
    fun clear() = topology.clear()
}
