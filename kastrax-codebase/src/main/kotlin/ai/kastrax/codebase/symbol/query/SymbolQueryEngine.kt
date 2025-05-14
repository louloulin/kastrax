package ai.kastrax.codebase.symbol.query

import ai.kastrax.codebase.symbol.model.SymbolNode
import ai.kastrax.codebase.symbol.model.SymbolRelation
import ai.kastrax.codebase.symbol.model.SymbolRelationGraph
import ai.kastrax.codebase.symbol.model.SymbolRelationType
import ai.kastrax.codebase.symbol.model.SymbolType

/**
 * 符号查询引擎
 *
 * 用于查询符号图中的节点和关系
 */
class SymbolQueryEngine(private val graph: SymbolRelationGraph) {

    /**
     * 获取所有符号节点
     *
     * @return 符号节点列表
     */
    fun getAllNodes(): List<SymbolNode> {
        return graph.getAllNodes()
    }

    /**
     * 根据ID获取符号节点
     *
     * @param id 节点ID
     * @return 符号节点，如果不存在则返回null
     */
    fun getNodeById(id: String): SymbolNode? {
        return graph.getNodeById(id)
    }

    /**
     * 根据名称获取符号节点
     *
     * @param name 节点名称
     * @return 符号节点列表
     */
    fun getNodesByName(name: String): List<SymbolNode> {
        return graph.getNodesByName(name)
    }

    /**
     * 根据类型获取符号节点
     *
     * @param type 节点类型
     * @return 符号节点列表
     */
    fun getNodesByType(type: SymbolType): List<SymbolNode> {
        return graph.getNodesByType(type)
    }

    /**
     * 获取所有符号关系
     *
     * @return 符号关系列表
     */
    fun getAllRelations(): List<SymbolRelation> {
        return graph.getAllRelations()
    }

    /**
     * 根据类型获取符号关系
     *
     * @param type 关系类型
     * @return 符号关系列表
     */
    fun getRelationsByType(type: SymbolRelationType): List<SymbolRelation> {
        return graph.getRelationsByType(type)
    }

    /**
     * 获取节点的所有出边
     *
     * @param nodeId 节点ID
     * @return 符号关系列表
     */
    fun getOutgoingEdges(nodeId: String): List<SymbolRelation> {
        return graph.getOutgoingEdges(nodeId)
    }

    /**
     * 获取节点的所有入边
     *
     * @param nodeId 节点ID
     * @return 符号关系列表
     */
    fun getIncomingEdges(nodeId: String): List<SymbolRelation> {
        return graph.getIncomingEdges(nodeId)
    }

    /**
     * 获取节点的所有邻居节点
     *
     * @param nodeId 节点ID
     * @return 符号节点列表
     */
    fun getNeighbors(nodeId: String): List<SymbolNode> {
        return graph.getNeighbors(nodeId)
    }

    /**
     * 获取节点的所有继承关系
     *
     * @param nodeId 节点ID
     * @return 符号关系列表
     */
    fun getInheritanceRelations(nodeId: String): List<SymbolRelation> {
        return graph.getInheritanceRelations(nodeId)
    }

    /**
     * 获取节点的所有实现关系
     *
     * @param nodeId 节点ID
     * @return 符号关系列表
     */
    fun getImplementationRelations(nodeId: String): List<SymbolRelation> {
        return graph.getImplementationRelations(nodeId)
    }

    /**
     * 获取节点的所有使用关系
     *
     * @param nodeId 节点ID
     * @return 符号关系列表
     */
    fun getUsageRelations(nodeId: String): List<SymbolRelation> {
        return graph.getUsageRelations(nodeId)
    }

    /**
     * 获取节点的所有依赖关系
     *
     * @param nodeId 节点ID
     * @return 符号关系列表
     */
    fun getDependencyRelations(nodeId: String): List<SymbolRelation> {
        return graph.getDependencyRelations(nodeId)
    }

    /**
     * 获取节点的所有重写关系
     *
     * @param nodeId 节点ID
     * @return 符号关系列表
     */
    fun getOverrideRelations(nodeId: String): List<SymbolRelation> {
        return graph.getOverrideRelations(nodeId)
    }

    /**
     * 获取节点的所有引用关系
     *
     * @param nodeId 节点ID
     * @return 符号关系列表
     */
    fun getReferenceRelations(nodeId: String): List<SymbolRelation> {
        return graph.getReferenceRelations(nodeId)
    }

    /**
     * 获取节点的所有导入关系
     *
     * @param nodeId 节点ID
     * @return 符号关系列表
     */
    fun getImportRelations(nodeId: String): List<SymbolRelation> {
        return graph.getImportRelations(nodeId)
    }

    /**
     * 获取两个节点之间的最短路径
     *
     * @param sourceId 源节点ID
     * @param targetId 目标节点ID
     * @return 符号关系列表，表示从源节点到目标节点的路径
     */
    fun getShortestPath(sourceId: String, targetId: String): List<SymbolRelation> {
        return graph.getShortestPathAsRelations(sourceId, targetId)
    }

    /**
     * 获取与节点相关的所有节点
     *
     * @param nodeId 节点ID
     * @param maxDepth 最大深度
     * @param relationTypes 关系类型集合
     * @return 符号节点列表
     */
    fun getRelatedNodes(
        nodeId: String,
        maxDepth: Int = 1,
        relationTypes: Set<SymbolRelationType> = SymbolRelationType.values().toSet()
    ): List<SymbolNode> {
        return graph.getRelatedNodesAsSymbols(nodeId, maxDepth, relationTypes)
    }

    /**
     * 搜索符号节点
     *
     * @param query 查询字符串
     * @param limit 限制数量
     * @return 符号节点列表
     */
    fun searchNodes(query: String, limit: Int = 10): List<SymbolNode> {
        val lowerQuery = query.lowercase()
        return graph.getAllNodes()
            .filter { it.name.lowercase().contains(lowerQuery) || it.qualifiedName.lowercase().contains(lowerQuery) }
            .take(limit)
    }

    /**
     * 获取符号图的统计信息
     *
     * @return 统计信息映射
     */
    fun getGraphStatistics(): Map<String, Any> {
        val nodes = graph.getAllNodes()
        val relations = graph.getAllRelations()

        val nodeTypeCount = nodes.groupBy { it.type }.mapValues { it.value.size }
        val relationTypeCount = relations.groupBy { it.type }.mapValues { it.value.size }

        return mapOf(
            "nodeCount" to nodes.size,
            "relationCount" to relations.size,
            "nodeTypeCount" to nodeTypeCount,
            "relationTypeCount" to relationTypeCount
        )
    }
}
