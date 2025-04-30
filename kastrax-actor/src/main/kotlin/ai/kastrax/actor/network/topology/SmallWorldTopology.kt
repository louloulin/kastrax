package ai.kastrax.actor.network.topology

import ai.kastrax.actor.network.AgentRelation
import ai.kastrax.actor.network.NetworkTopology
import kotlin.math.ln
import kotlin.random.Random

/**
 * 小世界网络拓扑，创建具有高聚类性和短路径长度的网络
 *
 * 小世界网络拓扑实现了具有高聚类系数和短平均路径长度的网络结构，
 * 适用于需要高效信息传播和协作的场景，如社交网络、协作系统等
 */
class SmallWorldTopology {
    private val topology = NetworkTopology()

    /**
     * 创建小世界网络
     *
     * @param nodeIds 节点 ID 列表
     * @param k 每个节点连接到的近邻数量（必须是偶数）
     * @param beta 重连概率，范围 [0, 1]
     * @param relation 节点之间的关系类型
     */
    fun createSmallWorldNetwork(
        nodeIds: List<String>,
        k: Int = 4,
        beta: Double = 0.1,
        relation: AgentRelation = AgentRelation.PEER
    ) {
        require(k % 2 == 0) { "参数 k 必须是偶数" }
        require(k < nodeIds.size) { "参数 k 必须小于节点数量" }
        require(beta in 0.0..1.0) { "参数 beta 必须在 [0, 1] 范围内" }

        // 清除现有拓扑结构
        topology.clear()

        // 添加所有节点
        nodeIds.forEach { topology.addNode(it) }

        // 创建规则环形网络
        val n = nodeIds.size
        for (i in 0 until n) {
            for (j in 1..k / 2) {
                val node1 = nodeIds[i]
                val node2 = nodeIds[(i + j) % n]
                topology.addEdge(node1, node2, relation)
            }
        }

        // 根据概率重连边，创建小世界特性
        for (i in 0 until n) {
            for (j in 1..k / 2) {
                if (Random.nextDouble() < beta) {
                    val node1 = nodeIds[i]
                    val oldNode2 = nodeIds[(i + j) % n]

                    // 移除原有的边
                    topology.removeEdge(node1, oldNode2)

                    // 随机选择新的目标节点
                    var newNode2: String
                    do {
                        val randomIndex = Random.nextInt(n)
                        newNode2 = nodeIds[randomIndex]
                    } while (newNode2 == node1 || topology.getRelation(node1, newNode2) != null)

                    // 添加新的边
                    topology.addEdge(node1, newNode2, relation)
                }
            }
        }
    }

    /**
     * 计算网络的聚类系数
     *
     * @return 网络的平均聚类系数
     */
    fun calculateClusteringCoefficient(): Double {
        val nodes = topology.getAllNodes()
        if (nodes.isEmpty()) return 0.0

        var totalCoefficient = 0.0

        for (node in nodes) {
            val neighbors = topology.getConnectedNodes(node)
            val n = neighbors.size

            if (n <= 1) {
                // 节点的邻居少于 2 个，聚类系数为 0
                continue
            }

            // 计算邻居之间的连接数
            var connections = 0
            for (i in 0 until n - 1) {
                for (j in i + 1 until n) {
                    if (topology.getRelation(neighbors[i], neighbors[j]) != null) {
                        connections++
                    }
                }
            }

            // 计算节点的聚类系数
            val maxPossibleConnections = n * (n - 1) / 2
            val nodeCoefficient = connections.toDouble() / maxPossibleConnections
            totalCoefficient += nodeCoefficient
        }

        return totalCoefficient / nodes.size
    }

    /**
     * 计算网络的平均路径长度
     *
     * @return 网络的平均路径长度
     */
    fun calculateAveragePathLength(): Double {
        val nodes = topology.getAllNodes()
        val n = nodes.size
        if (n <= 1) return 0.0

        var totalPathLength = 0.0
        var pathCount = 0

        for (i in 0 until n - 1) {
            for (j in i + 1 until n) {
                val pathLength = findShortestPathLength(nodes[i], nodes[j])
                if (pathLength > 0) {
                    totalPathLength += pathLength
                    pathCount++
                }
            }
        }

        return if (pathCount > 0) totalPathLength / pathCount else Double.POSITIVE_INFINITY
    }

    /**
     * 找到两个节点之间的最短路径长度
     *
     * @param startNodeId 起始节点 ID
     * @param endNodeId 目标节点 ID
     * @return 最短路径长度，如果不存在路径则返回 -1
     */
    fun findShortestPathLength(startNodeId: String, endNodeId: String): Int {
        if (startNodeId == endNodeId) return 0

        // 使用广度优先搜索找到最短路径
        val visited = mutableSetOf<String>()
        val queue = ArrayDeque<Pair<String, Int>>() // 节点 ID 和距离

        queue.add(Pair(startNodeId, 0))
        visited.add(startNodeId)

        while (queue.isNotEmpty()) {
            val (currentNodeId, distance) = queue.removeFirst()

            // 获取当前节点的所有邻居
            val neighbors = topology.getConnectedNodes(currentNodeId)

            for (neighborId in neighbors) {
                if (neighborId == endNodeId) {
                    return distance + 1
                }

                if (neighborId !in visited) {
                    visited.add(neighborId)
                    queue.add(Pair(neighborId, distance + 1))
                }
            }
        }

        return -1 // 不存在路径
    }

    /**
     * 计算网络的小世界系数
     *
     * 小世界系数 = 实际聚类系数 / 随机网络的聚类系数 / (实际平均路径长度 / 随机网络的平均路径长度)
     *
     * @return 小世界系数，大于 1 表示具有小世界特性
     */
    fun calculateSmallWorldness(): Double {
        val nodes = topology.getAllNodes()
        val n = nodes.size
        if (n <= 1) return 0.0

        // 计算实际网络的聚类系数和平均路径长度
        val actualClusteringCoefficient = calculateClusteringCoefficient()
        val actualPathLength = calculateAveragePathLength()

        // 计算等价随机网络的理论值
        // 随机网络的聚类系数 ≈ k/n，其中 k 是平均度
        val averageDegree = nodes.sumOf { topology.getConnectedNodes(it).size }.toDouble() / n
        val randomClusteringCoefficient = averageDegree / n

        // 随机网络的平均路径长度 ≈ ln(n) / ln(k)
        val randomPathLength = ln(n.toDouble()) / ln(averageDegree)

        // 计算小世界系数
        return (actualClusteringCoefficient / randomClusteringCoefficient) / (actualPathLength / randomPathLength)
    }

    /**
     * 添加桥接节点，连接网络中的不同社区
     *
     * @param bridgeNodeId 桥接节点 ID
     * @param targetNodeIds 要连接的目标节点 ID 列表
     * @param relation 连接关系类型
     */
    fun addBridgeNode(
        bridgeNodeId: String,
        targetNodeIds: List<String>,
        relation: AgentRelation = AgentRelation.PEER
    ) {
        // 添加桥接节点
        topology.addNode(bridgeNodeId)

        // 连接到目标节点
        for (targetNodeId in targetNodeIds) {
            if (topology.getAllNodes().contains(targetNodeId)) {
                topology.addEdge(bridgeNodeId, targetNodeId, relation)
            }
        }
    }

    /**
     * 识别网络中的社区结构
     *
     * 使用简化的标签传播算法识别社区
     *
     * @param maxIterations 最大迭代次数
     * @return 社区映射，键为社区 ID，值为该社区中的节点 ID 列表
     */
    fun detectCommunities(maxIterations: Int = 10): Map<Int, List<String>> {
        val nodes = topology.getAllNodes()
        if (nodes.isEmpty()) return emptyMap()

        // 初始化：每个节点分配一个唯一的社区标签
        val nodeLabels = nodes.mapIndexed { index, nodeId -> nodeId to index }.toMap().toMutableMap()

        // 迭代更新标签
        for (iteration in 0 until maxIterations) {
            var changed = false

            // 随机打乱节点顺序
            val shuffledNodes = nodes.shuffled()

            for (nodeId in shuffledNodes) {
                val neighbors = topology.getConnectedNodes(nodeId)
                if (neighbors.isEmpty()) continue

                // 统计邻居的标签
                val labelCounts = mutableMapOf<Int, Int>()
                for (neighborId in neighbors) {
                    val label = nodeLabels[neighborId] ?: continue
                    labelCounts[label] = (labelCounts[label] ?: 0) + 1
                }

                // 找出最常见的标签
                val (mostCommonLabel, _) = labelCounts.maxByOrNull { it.value } ?: continue

                // 更新节点的标签
                if (nodeLabels[nodeId] != mostCommonLabel) {
                    nodeLabels[nodeId] = mostCommonLabel
                    changed = true
                }
            }

            // 如果没有标签变化，提前结束
            if (!changed) break
        }

        // 将节点按社区分组
        return nodes.groupBy { nodeLabels[it] ?: -1 }
    }

    // 委托方法
    fun addNode(nodeId: String) = topology.addNode(nodeId)
    fun removeNode(nodeId: String) = topology.removeNode(nodeId)
    fun addEdge(fromId: String, toId: String, relation: AgentRelation) = topology.addEdge(fromId, toId, relation)
    fun removeEdge(fromId: String, toId: String) = topology.removeEdge(fromId, toId)
    fun getConnectedNodes(nodeId: String) = topology.getConnectedNodes(nodeId)
    fun getNodesByRelation(nodeId: String, relation: AgentRelation) = topology.getNodesByRelation(nodeId, relation)
    fun getRelation(fromId: String, toId: String) = topology.getRelation(fromId, toId)
    fun getAllNodes() = topology.getAllNodes()
    fun getAllEdges() = topology.getAllEdges()
    fun clear() = topology.clear()
}
