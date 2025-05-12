package ai.kastrax.codebase.semantic.flow.viz

import ai.kastrax.codebase.semantic.flow.*
import ai.kastrax.codebase.semantic.model.CodeElement
import io.github.oshai.kotlinlogging.KotlinLogging
import java.util.*

/**
 * 流图探索器配置
 *
 * @property maxPathLength 最大路径长度
 * @property maxResults 最大结果数
 */
data class FlowGraphExplorerConfig(
    val maxPathLength: Int = 20,
    val maxResults: Int = 100
)

/**
 * 路径类型
 */
enum class PathType {
    CONTROL_FLOW,
    DATA_FLOW,
    ANY
}

/**
 * 路径
 *
 * @property nodes 路径上的节点
 * @property edges 路径上的边
 */
data class Path(
    val nodes: List<FlowNode>,
    val edges: List<FlowEdge>
) {
    /**
     * 路径长度
     */
    val length: Int
        get() = edges.size
    
    /**
     * 路径描述
     */
    fun getDescription(): String {
        val sb = StringBuilder()
        
        sb.appendLine("路径长度: $length")
        sb.appendLine("节点: ${nodes.size}")
        
        nodes.forEachIndexed { index, node ->
            val nodeLabel = node.metadata["label"]?.toString() ?: node.element?.name ?: node.id
            sb.append("  $index: $nodeLabel (${node.type})")
            
            if (index < edges.size) {
                val edge = edges[index]
                val edgeLabel = edge.metadata["label"]?.toString() ?: edge.type.name
                sb.append(" --[$edgeLabel]--> ")
            } else {
                sb.appendLine()
            }
        }
        
        return sb.toString()
    }
}

/**
 * 流图探索器
 *
 * 用于查询和分析流图
 *
 * @property config 配置
 */
class FlowGraphExplorer(
    private val config: FlowGraphExplorerConfig = FlowGraphExplorerConfig()
) {
    private val logger = KotlinLogging.logger {}

    /**
     * 查找从源节点到目标节点的所有路径
     *
     * @param graph 流图
     * @param sourceNodeId 源节点ID
     * @param targetNodeId 目标节点ID
     * @param pathType 路径类型
     * @return 路径列表
     */
    fun findPaths(
        graph: FlowGraph,
        sourceNodeId: String,
        targetNodeId: String,
        pathType: PathType = PathType.ANY
    ): List<Path> {
        logger.info { "查找从 $sourceNodeId 到 $targetNodeId 的路径，类型: $pathType" }
        
        // 检查节点是否存在
        if (!graph.nodes.containsKey(sourceNodeId) || !graph.nodes.containsKey(targetNodeId)) {
            logger.warn { "源节点或目标节点不存在" }
            return emptyList()
        }
        
        // 使用 DFS 查找所有路径
        val paths = mutableListOf<Path>()
        val visited = mutableSetOf<String>()
        val currentPath = mutableListOf<String>()
        val currentEdges = mutableListOf<FlowEdge>()
        
        dfs(graph, sourceNodeId, targetNodeId, visited, currentPath, currentEdges, paths, pathType)
        
        // 限制结果数量
        val limitedPaths = paths.take(config.maxResults)
        
        logger.info { "找到 ${limitedPaths.size} 条路径" }
        return limitedPaths
    }

    /**
     * 查找与指定代码元素相关的所有节点
     *
     * @param graph 流图
     * @param element 代码元素
     * @return 节点列表
     */
    fun findNodesForElement(graph: FlowGraph, element: CodeElement): List<FlowNode> {
        return graph.nodes.values.filter { it.element?.id == element.id }.toList()
    }

    /**
     * 查找与指定节点相关的所有边
     *
     * @param graph 流图
     * @param nodeId 节点ID
     * @param direction 方向（入边、出边或两者）
     * @return 边列表
     */
    fun findEdgesForNode(
        graph: FlowGraph,
        nodeId: String,
        direction: EdgeDirection = EdgeDirection.BOTH
    ): List<FlowEdge> {
        return when (direction) {
            EdgeDirection.INCOMING -> graph.getIncomingEdges(nodeId)
            EdgeDirection.OUTGOING -> graph.getOutgoingEdges(nodeId)
            EdgeDirection.BOTH -> graph.getIncomingEdges(nodeId) + graph.getOutgoingEdges(nodeId)
        }
    }

    /**
     * 查找特定类型的所有节点
     *
     * @param graph 流图
     * @param nodeType 节点类型
     * @return 节点列表
     */
    fun findNodesByType(graph: FlowGraph, nodeType: FlowNodeType): List<FlowNode> {
        return graph.nodes.values.filter { it.type == nodeType }.toList()
    }

    /**
     * 查找特定类型的所有边
     *
     * @param graph 流图
     * @param edgeType 边类型
     * @return 边列表
     */
    fun findEdgesByType(graph: FlowGraph, edgeType: FlowEdgeType): List<FlowEdge> {
        return graph.edges.values.filter { it.type == edgeType }.toList()
    }

    /**
     * 查找循环
     *
     * @param graph 流图
     * @return 循环路径列表
     */
    fun findCycles(graph: FlowGraph): List<Path> {
        logger.info { "查找循环" }
        
        val cycles = mutableListOf<Path>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        
        // 对每个未访问的节点进行 DFS
        for (nodeId in graph.nodes.keys) {
            if (nodeId !in visited) {
                val currentPath = mutableListOf<String>()
                val currentEdges = mutableListOf<FlowEdge>()
                findCyclesDfs(graph, nodeId, visited, recursionStack, currentPath, currentEdges, cycles)
            }
        }
        
        // 限制结果数量
        val limitedCycles = cycles.take(config.maxResults)
        
        logger.info { "找到 ${limitedCycles.size} 个循环" }
        return limitedCycles
    }

    /**
     * 查找孤立节点
     *
     * @param graph 流图
     * @return 孤立节点列表
     */
    fun findIsolatedNodes(graph: FlowGraph): List<FlowNode> {
        return graph.nodes.values.filter { node ->
            graph.getIncomingEdges(node.id).isEmpty() && graph.getOutgoingEdges(node.id).isEmpty()
        }.toList()
    }

    /**
     * 查找未使用的变量
     *
     * @param graph 流图
     * @return 未使用的变量节点列表
     */
    fun findUnusedVariables(graph: FlowGraph): List<FlowNode> {
        // 查找声明类型的节点
        val declarationNodes = graph.nodes.values.filter { it.type == FlowNodeType.DECLARATION }
        
        // 过滤出没有出边的声明节点（未被使用的变量）
        return declarationNodes.filter { node ->
            graph.getOutgoingEdges(node.id).isEmpty()
        }.toList()
    }

    /**
     * 查找不可达节点
     *
     * @param graph 流图
     * @return 不可达节点列表
     */
    fun findUnreachableNodes(graph: FlowGraph): List<FlowNode> {
        // 如果没有入口节点，则无法确定不可达节点
        val entryNodeId = graph.entryNodeId ?: return emptyList()
        
        // 使用 BFS 找出所有可达节点
        val reachable = mutableSetOf<String>()
        val queue = LinkedList<String>()
        
        queue.add(entryNodeId)
        reachable.add(entryNodeId)
        
        while (queue.isNotEmpty()) {
            val nodeId = queue.poll()
            
            for (successorId in graph.getSuccessors(nodeId)) {
                if (successorId !in reachable) {
                    reachable.add(successorId)
                    queue.add(successorId)
                }
            }
        }
        
        // 返回所有不可达节点
        return graph.nodes.values.filter { it.id !in reachable }.toList()
    }

    /**
     * 查找死代码
     *
     * @param graph 流图
     * @return 死代码节点列表
     */
    fun findDeadCode(graph: FlowGraph): List<FlowNode> {
        // 查找不可达节点
        val unreachableNodes = findUnreachableNodes(graph)
        
        // 查找没有出边的非出口节点
        val noExitPathNodes = graph.nodes.values.filter { node ->
            node.id !in graph.exitNodeIds && // 不是出口节点
            graph.getOutgoingEdges(node.id).isEmpty() && // 没有出边
            node.type != FlowNodeType.RETURN && // 不是返回语句
            node.type != FlowNodeType.THROW // 不是抛出异常语句
        }
        
        return (unreachableNodes + noExitPathNodes).distinct()
    }

    /**
     * 深度优先搜索查找路径
     */
    private fun dfs(
        graph: FlowGraph,
        currentNodeId: String,
        targetNodeId: String,
        visited: MutableSet<String>,
        currentPath: MutableList<String>,
        currentEdges: MutableList<FlowEdge>,
        paths: MutableList<Path>,
        pathType: PathType
    ) {
        // 如果路径过长，则停止搜索
        if (currentPath.size > config.maxPathLength) {
            return
        }
        
        // 标记当前节点为已访问
        visited.add(currentNodeId)
        currentPath.add(currentNodeId)
        
        // 如果找到目标节点，则添加路径
        if (currentNodeId == targetNodeId) {
            val pathNodes = currentPath.mapNotNull { graph.nodes[it] }
            val path = Path(pathNodes, currentEdges.toList())
            paths.add(path)
        } else {
            // 获取当前节点的所有出边
            val outgoingEdges = graph.getOutgoingEdges(currentNodeId)
            
            // 根据路径类型过滤边
            val filteredEdges = when (pathType) {
                PathType.CONTROL_FLOW -> outgoingEdges.filter { 
                    it.type != FlowEdgeType.DATA_DEPENDENCY 
                }
                PathType.DATA_FLOW -> outgoingEdges.filter { 
                    it.type == FlowEdgeType.DATA_DEPENDENCY 
                }
                PathType.ANY -> outgoingEdges
            }
            
            // 继续搜索
            for (edge in filteredEdges) {
                val nextNodeId = edge.targetId
                if (nextNodeId !in visited) {
                    currentEdges.add(edge)
                    dfs(graph, nextNodeId, targetNodeId, visited, currentPath, currentEdges, paths, pathType)
                    currentEdges.removeAt(currentEdges.size - 1)
                }
            }
        }
        
        // 回溯
        currentPath.removeAt(currentPath.size - 1)
        visited.remove(currentNodeId)
    }

    /**
     * 深度优先搜索查找循环
     */
    private fun findCyclesDfs(
        graph: FlowGraph,
        currentNodeId: String,
        visited: MutableSet<String>,
        recursionStack: MutableSet<String>,
        currentPath: MutableList<String>,
        currentEdges: MutableList<FlowEdge>,
        cycles: MutableList<Path>
    ) {
        // 标记当前节点为已访问
        visited.add(currentNodeId)
        recursionStack.add(currentNodeId)
        currentPath.add(currentNodeId)
        
        // 获取当前节点的所有出边
        val outgoingEdges = graph.getOutgoingEdges(currentNodeId)
        
        // 检查每个相邻节点
        for (edge in outgoingEdges) {
            val nextNodeId = edge.targetId
            
            // 如果相邻节点在递归栈中，则找到了一个循环
            if (nextNodeId in recursionStack) {
                // 找到循环的起始位置
                val cycleStartIndex = currentPath.indexOf(nextNodeId)
                if (cycleStartIndex != -1) {
                    // 提取循环路径
                    val cyclePath = currentPath.subList(cycleStartIndex, currentPath.size)
                    val cycleNodes = cyclePath.mapNotNull { graph.nodes[it] }
                    
                    // 提取循环边
                    val cycleEdges = mutableListOf<FlowEdge>()
                    for (i in 0 until cyclePath.size - 1) {
                        val sourceId = cyclePath[i]
                        val targetId = cyclePath[i + 1]
                        val cycleEdge = graph.getEdgesBetween(sourceId, targetId).firstOrNull()
                        if (cycleEdge != null) {
                            cycleEdges.add(cycleEdge)
                        }
                    }
                    
                    // 添加最后一条边（从最后一个节点到循环起始节点）
                    val lastEdge = graph.getEdgesBetween(currentNodeId, nextNodeId).firstOrNull()
                    if (lastEdge != null) {
                        cycleEdges.add(lastEdge)
                    }
                    
                    // 创建循环路径
                    val cycle = Path(cycleNodes, cycleEdges)
                    cycles.add(cycle)
                }
            } else if (nextNodeId !in visited) {
                // 如果相邻节点未访问，则继续搜索
                currentEdges.add(edge)
                findCyclesDfs(graph, nextNodeId, visited, recursionStack, currentPath, currentEdges, cycles)
                currentEdges.removeAt(currentEdges.size - 1)
            }
        }
        
        // 回溯
        currentPath.removeAt(currentPath.size - 1)
        recursionStack.remove(currentNodeId)
    }
}

/**
 * 边方向
 */
enum class EdgeDirection {
    INCOMING,
    OUTGOING,
    BOTH
}
