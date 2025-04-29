package ai.kastrax.actor.network

import java.util.concurrent.ConcurrentHashMap

/**
 * 网络拓扑结构，定义 Agent 之间的连接关系
 * 
 * 网络拓扑结构使用邻接表表示 Agent 之间的关系，支持添加、移除节点和边，以及查询节点关系
 */
class NetworkTopology {
    /**
     * 邻接表，存储节点之间的连接关系
     */
    private val adjacencyList = ConcurrentHashMap<String, ConcurrentHashMap<String, AgentRelation>>()
    
    /**
     * 添加节点
     *
     * @param nodeId 节点 ID
     */
    fun addNode(nodeId: String) {
        adjacencyList.putIfAbsent(nodeId, ConcurrentHashMap())
    }
    
    /**
     * 移除节点
     *
     * @param nodeId 节点 ID
     */
    fun removeNode(nodeId: String) {
        // 移除节点
        adjacencyList.remove(nodeId)
        
        // 移除其他节点到该节点的连接
        adjacencyList.forEach { (_, edges) ->
            edges.remove(nodeId)
        }
    }
    
    /**
     * 添加边
     *
     * @param fromId 源节点 ID
     * @param toId 目标节点 ID
     * @param relation 关系类型
     */
    fun addEdge(fromId: String, toId: String, relation: AgentRelation) {
        // 确保节点存在
        addNode(fromId)
        addNode(toId)
        
        // 添加边
        adjacencyList[fromId]!![toId] = relation
        
        // 添加反向边，使用对应的关系类型
        val reverseRelation = when (relation) {
            AgentRelation.MASTER -> AgentRelation.SLAVE
            AgentRelation.SLAVE -> AgentRelation.MASTER
            AgentRelation.SUPERVISOR -> AgentRelation.SUPERVISED
            AgentRelation.SUPERVISED -> AgentRelation.SUPERVISOR
            else -> relation // 对等关系和协作关系是对称的
        }
        adjacencyList[toId]!![fromId] = reverseRelation
    }
    
    /**
     * 移除边
     *
     * @param fromId 源节点 ID
     * @param toId 目标节点 ID
     */
    fun removeEdge(fromId: String, toId: String) {
        // 移除边
        adjacencyList[fromId]?.remove(toId)
        adjacencyList[toId]?.remove(fromId)
    }
    
    /**
     * 获取与指定节点相连的所有节点
     *
     * @param nodeId 节点 ID
     * @return 相连的节点 ID 列表
     */
    fun getConnectedNodes(nodeId: String): List<String> {
        return adjacencyList[nodeId]?.keys?.toList() ?: emptyList()
    }
    
    /**
     * 获取与指定节点具有特定关系的所有节点
     *
     * @param nodeId 节点 ID
     * @param relation 关系类型
     * @return 具有特定关系的节点 ID 列表
     */
    fun getNodesByRelation(nodeId: String, relation: AgentRelation): List<String> {
        return adjacencyList[nodeId]?.entries
            ?.filter { it.value == relation }
            ?.map { it.key }
            ?.toList() ?: emptyList()
    }
    
    /**
     * 获取两个节点之间的关系
     *
     * @param fromId 源节点 ID
     * @param toId 目标节点 ID
     * @return 关系类型，如果不存在则返回 null
     */
    fun getRelation(fromId: String, toId: String): AgentRelation? {
        return adjacencyList[fromId]?.get(toId)
    }
    
    /**
     * 获取所有节点
     *
     * @return 所有节点 ID 列表
     */
    fun getAllNodes(): List<String> {
        return adjacencyList.keys.toList()
    }
    
    /**
     * 获取所有边
     *
     * @return 所有边的列表，每条边由源节点 ID、目标节点 ID 和关系类型组成
     */
    fun getAllEdges(): List<Triple<String, String, AgentRelation>> {
        val edges = mutableListOf<Triple<String, String, AgentRelation>>()
        
        adjacencyList.forEach { (fromId, toMap) ->
            toMap.forEach { (toId, relation) ->
                edges.add(Triple(fromId, toId, relation))
            }
        }
        
        return edges
    }
    
    /**
     * 清空拓扑结构
     */
    fun clear() {
        adjacencyList.clear()
    }
}
