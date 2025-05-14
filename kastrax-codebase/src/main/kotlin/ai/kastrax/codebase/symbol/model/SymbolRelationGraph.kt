package ai.kastrax.codebase.symbol.model

import ai.kastrax.codebase.semantic.model.CodeElement
import ai.kastrax.codebase.semantic.model.CodeElementType
import ai.kastrax.codebase.semantic.model.CodeElementTypeConstants.VARIABLE
import ai.kastrax.codebase.semantic.model.CodeElementTypeConstants.NAMESPACE
import ai.kastrax.codebase.semantic.model.CodeElementTypeConstants.MODULE
import ai.kastrax.codebase.semantic.relation.CodeRelation
import ai.kastrax.codebase.semantic.relation.RelationType as SemanticRelationType
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.LinkedList
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * 符号关系图
 *
 * 表示代码元素之间的关系图
 *
 * @property id 唯一标识符
 * @property name 名称
 * @property nodes 节点集合
 * @property edges 边集合
 * @property metadata 元数据
 */
class SymbolRelationGraph(
    val id: String = UUID.randomUUID().toString(),
    val name: String = "Symbol Relation Graph",
    val nodes: MutableMap<String, SymbolNode> = ConcurrentHashMap(),
    val edges: MutableMap<String, SymbolRelation> = ConcurrentHashMap(),
    val metadata: MutableMap<String, Any> = ConcurrentHashMap()
) {
    /**
     * 添加节点
     *
     * @param node 节点
     * @return 是否成功添加
     */
    fun addNode(node: SymbolNode): Boolean {
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
    fun addEdge(edge: SymbolRelation): Boolean {
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
     * 获取节点
     *
     * @param nodeId 节点ID
     * @return 节点，如果找不到则返回 null
     */
    fun getNode(nodeId: String): SymbolNode? {
        return nodes[nodeId]
    }

    /**
     * 获取节点（别名）
     *
     * @param nodeId 节点ID
     * @return 节点，如果找不到则返回 null
     */
    fun getNodeById(nodeId: String): SymbolNode? {
        return getNode(nodeId)
    }

    /**
     * 获取边
     *
     * @param edgeId 边ID
     * @return 边，如果找不到则返回 null
     */
    fun getEdge(edgeId: String): SymbolRelation? {
        return edges[edgeId]
    }

    /**
     * 获取节点的出边
     *
     * @param nodeId 节点ID
     * @return 出边集合
     */
    fun getOutgoingEdges(nodeId: String): List<SymbolRelation> {
        return edges.values.filter { it.sourceId == nodeId }
    }

    /**
     * 获取两个节点之间的关系
     *
     * @param sourceId 源节点ID
     * @param targetId 目标节点ID
     * @return 符号关系，如果不存在则返回 null
     */
    fun getRelation(sourceId: String, targetId: String): SymbolRelation? {
        return edges.values.find { it.sourceId == sourceId && it.targetId == targetId }
    }

    /**
     * 获取节点的入边
     *
     * @param nodeId 节点ID
     * @return 入边集合
     */
    fun getIncomingEdges(nodeId: String): List<SymbolRelation> {
        return edges.values.filter { it.targetId == nodeId }
    }

    /**
     * 获取节点的相邻节点
     *
     * @param nodeId 节点ID
     * @return 相邻节点ID集合
     */
    fun getAdjacentNodes(nodeId: String): Set<String> {
        val outgoing = getOutgoingEdges(nodeId).map { it.targetId }
        val incoming = getIncomingEdges(nodeId).map { it.sourceId }
        return (outgoing + incoming).toSet()
    }

    /**
     * 获取节点的后继节点
     *
     * @param nodeId 节点ID
     * @return 后继节点ID集合
     */
    fun getSuccessors(nodeId: String): Set<String> {
        return getOutgoingEdges(nodeId).map { it.targetId }.toSet()
    }

    /**
     * 获取节点的前驱节点
     *
     * @param nodeId 节点ID
     * @return 前驱节点ID集合
     */
    fun getPredecessors(nodeId: String): Set<String> {
        return getIncomingEdges(nodeId).map { it.sourceId }.toSet()
    }

    /**
     * 获取两个节点之间的最短路径
     *
     * @param sourceId 源节点ID
     * @param targetId 目标节点ID
     * @return 路径上的节点ID列表，如果不存在路径则返回空列表
     */
    fun getShortestPath(sourceId: String, targetId: String): List<String> {
        if (!nodes.containsKey(sourceId) || !nodes.containsKey(targetId)) {
            return emptyList()
        }

        if (sourceId == targetId) {
            return listOf(sourceId)
        }

        // 广度优先搜索
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<String>()
        val parent = mutableMapOf<String, String>()

        visited.add(sourceId)
        queue.add(sourceId)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()

            if (current == targetId) {
                // 找到目标节点，构建路径
                val path = mutableListOf<String>()
                var node = current
                while (node != sourceId) {
                    path.add(0, node)
                    node = parent[node]!!
                }
                path.add(0, sourceId)
                return path
            }

            // 遍历相邻节点
            for (neighbor in getSuccessors(current)) {
                if (neighbor !in visited) {
                    visited.add(neighbor)
                    queue.add(neighbor)
                    parent[neighbor] = current
                }
            }
        }

        return emptyList() // 没有找到路径
    }

    /**
     * 获取特定类型的关系
     *
     * @param type 关系类型
     * @return 关系集合
     */
    fun getRelationsByType(type: SymbolRelationType): List<SymbolRelation> {
        return edges.values.filter { it.type == type }
    }

    /**
     * 获取节点的特定类型的出边
     *
     * @param nodeId 节点ID
     * @param type 关系类型
     * @return 出边集合
     */
    fun getOutgoingEdgesByType(nodeId: String, type: SymbolRelationType): List<SymbolRelation> {
        return getOutgoingEdges(nodeId).filter { it.type == type }
    }

    /**
     * 获取节点的特定类型的入边
     *
     * @param nodeId 节点ID
     * @param type 关系类型
     * @return 入边集合
     */
    fun getIncomingEdgesByType(nodeId: String, type: SymbolRelationType): List<SymbolRelation> {
        return getIncomingEdges(nodeId).filter { it.type == type }
    }

    /**
     * 获取节点的继承关系
     *
     * @param nodeId 节点ID
     * @return 继承关系集合
     */
    fun getInheritanceRelations(nodeId: String): List<SymbolRelation> {
        return getOutgoingEdgesByType(nodeId, SymbolRelationType.EXTENDS)
    }

    /**
     * 获取节点的实现关系
     *
     * @param nodeId 节点ID
     * @return 实现关系集合
     */
    fun getImplementationRelations(nodeId: String): List<SymbolRelation> {
        return getOutgoingEdgesByType(nodeId, SymbolRelationType.IMPLEMENTS)
    }

    /**
     * 获取节点的使用关系
     *
     * @param nodeId 节点ID
     * @return 使用关系集合
     */
    fun getUsageRelations(nodeId: String): List<SymbolRelation> {
        return getOutgoingEdgesByType(nodeId, SymbolRelationType.USES)
    }

    /**
     * 获取节点的依赖关系
     *
     * @param nodeId 节点ID
     * @return 依赖关系集合
     */
    fun getDependencyRelations(nodeId: String): List<SymbolRelation> {
        return getOutgoingEdgesByType(nodeId, SymbolRelationType.DEPENDS_ON)
    }

    /**
     * 获取节点的重写关系
     *
     * @param nodeId 节点ID
     * @return 重写关系集合
     */
    fun getOverrideRelations(nodeId: String): List<SymbolRelation> {
        return getOutgoingEdgesByType(nodeId, SymbolRelationType.OVERRIDES)
    }

    /**
     * 获取节点的引用关系
     *
     * @param nodeId 节点ID
     * @return 引用关系集合
     */
    fun getReferenceRelations(nodeId: String): List<SymbolRelation> {
        return getOutgoingEdgesByType(nodeId, SymbolRelationType.REFERENCES)
    }

    /**
     * 获取节点的导入关系
     *
     * @param nodeId 节点ID
     * @return 导入关系集合
     */
    fun getImportRelations(nodeId: String): List<SymbolRelation> {
        return getOutgoingEdgesByType(nodeId, SymbolRelationType.IMPORTS)
    }

    /**
     * 获取节点的所有关系
     *
     * @param nodeId 节点ID
     * @return 关系集合
     */
    fun getAllRelations(nodeId: String): List<SymbolRelation> {
        val outgoing = getOutgoingEdges(nodeId)
        val incoming = getIncomingEdges(nodeId)
        return outgoing + incoming
    }

    /**
     * 获取节点的相关节点
     *
     * @param nodeId 节点ID
     * @param maxDepth 最大深度
     * @param relationTypes 关系类型集合
     * @return 相关节点ID集合
     */
    fun getRelatedNodes(
        nodeId: String,
        maxDepth: Int = 1,
        relationTypes: Set<SymbolRelationType> = SymbolRelationType.values().toSet()
    ): Set<String> {
        if (!nodes.containsKey(nodeId) || maxDepth <= 0) {
            return emptySet()
        }

        val result = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<Pair<String, Int>>() // 节点ID和深度

        visited.add(nodeId)
        queue.add(nodeId to 0)

        while (queue.isNotEmpty()) {
            val (current, depth) = queue.removeFirst()

            if (current != nodeId) {
                result.add(current)
            }

            if (depth < maxDepth) {
                // 获取符合条件的相邻节点
                val neighbors = getAllRelations(current)
                    .filter { it.type in relationTypes }
                    .map { if (it.sourceId == current) it.targetId else it.sourceId }

                for (neighbor in neighbors) {
                    if (neighbor !in visited) {
                        visited.add(neighbor)
                        queue.add(neighbor to depth + 1)
                    }
                }
            }
        }

        return result
    }

    /**
     * 从代码关系构建符号关系图
     *
     * @param codeElements 代码元素集合
     * @param codeRelations 代码关系集合
     * @param config 配置
     * @return 符号关系图
     */
    companion object {
        /**
         * 从代码关系构建符号关系图
         *
         * @param codeElements 代码元素集合
         * @param codeRelations 代码关系集合
         * @param config 配置
         * @return 符号关系图
         */
        fun fromCodeRelations(
            codeElements: List<CodeElement>,
            codeRelations: List<CodeRelation>,
            config: SymbolRelationGraphConfig = SymbolRelationGraphConfig()
        ): SymbolRelationGraph {
            val graph = SymbolRelationGraph(
                name = "Symbol Relation Graph from Code Relations"
            )

            // 添加节点
            for (element in codeElements) {
                val node = SymbolNode(
                    id = element.id,
                    name = element.name,
                    qualifiedName = element.qualifiedName,
                    type = when (element.type) {
                        CodeElementType.FILE -> SymbolType.FILE
                        CodeElementType.PACKAGE -> SymbolType.PACKAGE
                        CodeElementType.CLASS -> SymbolType.CLASS
                        CodeElementType.INTERFACE -> SymbolType.INTERFACE
                        CodeElementType.ENUM -> SymbolType.ENUM
                        CodeElementType.ANNOTATION -> SymbolType.ANNOTATION
                        CodeElementType.METHOD -> SymbolType.METHOD
                        CodeElementType.CONSTRUCTOR -> SymbolType.CONSTRUCTOR
                        CodeElementType.FIELD -> SymbolType.FIELD
                        CodeElementType.PROPERTY -> SymbolType.PROPERTY
                        CodeElementType.PARAMETER -> SymbolType.PARAMETER
                        CodeElementType.FUNCTION -> SymbolType.FUNCTION
                        CodeElementType.VARIABLE -> SymbolType.LOCAL_VARIABLE
                        CodeElementType.LOCAL_VARIABLE -> SymbolType.LOCAL_VARIABLE
                        CodeElementType.IMPORT -> SymbolType.IMPORT
                        CodeElementType.NAMESPACE -> SymbolType.NAMESPACE
                        CodeElementType.MODULE -> SymbolType.MODULE
                        CodeElementType.LAMBDA -> SymbolType.LAMBDA
                        CodeElementType.BLOCK -> SymbolType.BLOCK
                        CodeElementType.STATEMENT -> SymbolType.STATEMENT
                        CodeElementType.EXPRESSION -> SymbolType.EXPRESSION
                        CodeElementType.COMMENT -> SymbolType.COMMENT
                        CodeElementType.UNKNOWN -> SymbolType.UNKNOWN
                        else -> SymbolType.UNKNOWN
                    },
                    kind = "DEFINITION",
                    location = element.location,
                    codeElement = element,
                    metadata = element.metadata.toMutableMap()
                )
                graph.addNode(node)
            }

            // 添加边
            for (relation in codeRelations) {
                // 根据配置过滤关系类型
                if (!shouldIncludeRelation(relation.type, config)) {
                    continue
                }

                val symbolRelationType = when (relation.type) {
                    SemanticRelationType.INHERITANCE -> SymbolRelationType.EXTENDS
                    SemanticRelationType.IMPLEMENTATION -> SymbolRelationType.IMPLEMENTS
                    SemanticRelationType.USAGE -> SymbolRelationType.USES
                    SemanticRelationType.DEPENDENCY -> SymbolRelationType.DEPENDS_ON
                    SemanticRelationType.OVERRIDE -> SymbolRelationType.OVERRIDES
                    SemanticRelationType.REFERENCE -> SymbolRelationType.REFERENCES
                    SemanticRelationType.IMPORT -> SymbolRelationType.IMPORTS
                    else -> SymbolRelationType.UNKNOWN
                }

                val edge = SymbolRelation(
                    id = relation.id,
                    sourceId = relation.sourceId,
                    targetId = relation.targetId,
                    type = symbolRelationType,
                    metadata = relation.metadata.toMutableMap()
                )
                graph.addEdge(edge)
            }

            return graph
        }

        /**
         * 检查是否应该包含关系
         *
         * @param type 关系类型
         * @param config 配置
         * @return 是否应该包含
         */
        private fun shouldIncludeRelation(type: SemanticRelationType, config: SymbolRelationGraphConfig): Boolean {
            return when (type) {
                SemanticRelationType.INHERITANCE -> config.includeInheritance
                SemanticRelationType.IMPLEMENTATION -> config.includeImplementation
                SemanticRelationType.USAGE -> config.includeUsage
                SemanticRelationType.DEPENDENCY -> config.includeDependency
                SemanticRelationType.OVERRIDE -> config.includeOverride
                SemanticRelationType.REFERENCE -> config.includeReference
                SemanticRelationType.IMPORT -> config.includeImport
                else -> false
            }
        }
    }

    /**
     * 将代码元素类型转换为符号类型
     *
     * @param codeElementType 代码元素类型
     * @return 符号类型
     */
    private fun convertToSymbolType(codeElementType: CodeElementType): SymbolType {
        return when (codeElementType) {
            CodeElementType.FILE -> SymbolType.FILE
            CodeElementType.PACKAGE -> SymbolType.PACKAGE
            CodeElementType.CLASS -> SymbolType.CLASS
            CodeElementType.INTERFACE -> SymbolType.INTERFACE
            CodeElementType.ENUM -> SymbolType.ENUM
            CodeElementType.ANNOTATION -> SymbolType.ANNOTATION
            CodeElementType.METHOD -> SymbolType.METHOD
            CodeElementType.CONSTRUCTOR -> SymbolType.CONSTRUCTOR
            CodeElementType.FIELD -> SymbolType.FIELD
            CodeElementType.PROPERTY -> SymbolType.PROPERTY
            CodeElementType.PARAMETER -> SymbolType.PARAMETER
            CodeElementType.FUNCTION -> SymbolType.FUNCTION
            CodeElementType.VARIABLE -> SymbolType.LOCAL_VARIABLE
            CodeElementType.LOCAL_VARIABLE -> SymbolType.LOCAL_VARIABLE
            CodeElementType.IMPORT -> SymbolType.IMPORT
            CodeElementType.NAMESPACE -> SymbolType.NAMESPACE
            CodeElementType.MODULE -> SymbolType.MODULE
            CodeElementType.LAMBDA -> SymbolType.LAMBDA
            CodeElementType.BLOCK -> SymbolType.BLOCK
            CodeElementType.STATEMENT -> SymbolType.STATEMENT
            CodeElementType.EXPRESSION -> SymbolType.EXPRESSION
            CodeElementType.COMMENT -> SymbolType.COMMENT
            CodeElementType.UNKNOWN -> SymbolType.UNKNOWN
            else -> SymbolType.UNKNOWN
        }
    }

    /**
     * 获取所有节点
     *
     * @return 所有节点的列表
     */
    fun getAllNodes(): List<SymbolNode> {
        return nodes.values.toList()
    }

    /**
     * 根据名称获取节点
     *
     * @param name 节点名称
     * @return 符合条件的节点列表
     */
    fun getNodesByName(name: String): List<SymbolNode> {
        return nodes.values.filter { it.name == name }
    }

    /**
     * 根据类型获取节点
     *
     * @param type 节点类型
     * @return 符合条件的节点列表
     */
    fun getNodesByType(type: SymbolType): List<SymbolNode> {
        return nodes.values.filter { it.type == type }
    }

    /**
     * 获取节点数量
     *
     * @return 节点数量
     */
    fun getNodeCount(): Int {
        return nodes.size
    }

    /**
     * 获取相邻节点
     *
     * @param nodeId 节点ID
     * @return 相邻节点列表
     */
    fun getNeighbors(nodeId: String): List<SymbolNode> {
        val neighborIds = getAdjacentNodes(nodeId)
        return neighborIds.mapNotNull { getNode(it) }
    }

    /**
     * 获取所有符号关系
     *
     * @return 符号关系列表
     */
    fun getAllRelations(): List<SymbolRelation> {
        return edges.values.toList()
    }

    /**
     * 将代码元素类型转换为符号类型
     *
     * @param elementType 代码元素类型
     * @return 符号类型
     */


    /**
     * 获取最短路径关系
     *
     * @param sourceId 源节点ID
     * @param targetId 目标节点ID
     * @return 符号关系列表
     */
    fun getShortestPathAsRelations(sourceId: String, targetId: String): List<SymbolRelation> {
        // 使用广度优先搜索找到最短路径
        val visited = mutableSetOf<String>()
        val queue = LinkedList<Pair<String, List<String>>>()
        queue.add(sourceId to listOf(sourceId))
        visited.add(sourceId)

        while (queue.isNotEmpty()) {
            val (currentId, path) = queue.poll()

            if (currentId == targetId) {
                // 找到目标节点，返回路径上的关系
                val relations = mutableListOf<SymbolRelation>()
                for (i in 0 until path.size - 1) {
                    val fromId = path[i]
                    val toId = path[i + 1]
                    val relation = getRelation(fromId, toId)
                    if (relation != null) {
                        relations.add(relation)
                    }
                }
                return relations
            }

            // 遍历相邻节点
            val neighbors = getAdjacentNodes(currentId)
            for (neighborId in neighbors) {
                if (!visited.contains(neighborId)) {
                    visited.add(neighborId)
                    queue.add(neighborId to path + neighborId)
                }
            }
        }

        return emptyList()
    }

    /**
     * 获取与节点相关的所有节点对象
     *
     * @param nodeId 节点ID
     * @param maxDepth 最大深度
     * @param relationTypes 关系类型集合
     * @return 符号节点列表
     */
    fun getRelatedNodesAsSymbols(
        nodeId: String,
        maxDepth: Int = 1,
        relationTypes: Set<SymbolRelationType> = SymbolRelationType.values().toSet()
    ): List<SymbolNode> {
        if (maxDepth <= 0) {
            return emptyList()
        }

        val visited = mutableSetOf<String>()
        val result = mutableSetOf<String>()
        val queue = LinkedList<Pair<String, Int>>()

        queue.add(nodeId to 0)
        visited.add(nodeId)

        while (queue.isNotEmpty()) {
            val (currentId, depth) = queue.poll()

            if (currentId != nodeId) {
                result.add(currentId)
            }

            if (depth < maxDepth) {
                // 获取指定类型的关系
                val outgoingRelations = getOutgoingEdges(currentId).filter { relationTypes.contains(it.type) }
                val incomingRelations = getIncomingEdges(currentId).filter { relationTypes.contains(it.type) }

                // 遍历出度关系
                for (relation in outgoingRelations) {
                    if (!visited.contains(relation.targetId)) {
                        visited.add(relation.targetId)
                        queue.add(relation.targetId to depth + 1)
                    }
                }

                // 遍历入度关系
                for (relation in incomingRelations) {
                    if (!visited.contains(relation.sourceId)) {
                        visited.add(relation.sourceId)
                        queue.add(relation.sourceId to depth + 1)
                    }
                }
            }
        }

        return result.mapNotNull { getNode(it) }
    }
}
