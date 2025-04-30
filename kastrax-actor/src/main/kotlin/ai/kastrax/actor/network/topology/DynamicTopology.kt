package ai.kastrax.actor.network.topology

import ai.kastrax.actor.network.AgentRelation
import ai.kastrax.actor.network.NetworkTopology
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * 动态网络拓扑，支持根据任务需求动态调整网络结构
 *
 * 动态网络拓扑允许在运行时根据任务需求动态调整 Agent 之间的关系，
 * 支持添加和移除节点、边，以及根据任务类型自动调整网络结构
 */
class DynamicTopology {
    private val topology = NetworkTopology()

    /**
     * 拓扑配置，定义不同任务类型对应的网络结构
     */
    private val topologyConfigurations = ConcurrentHashMap<String, TopologyConfiguration>()

    /**
     * 当前活动的拓扑配置
     */
    private var activeConfiguration: String? = null

    /**
     * 读写锁，用于保护拓扑结构的并发访问
     */
    private val rwLock = ReentrantReadWriteLock()

    /**
     * 添加拓扑配置
     *
     * @param name 配置名称
     * @param configuration 拓扑配置
     */
    fun addTopologyConfiguration(name: String, configuration: TopologyConfiguration) {
        topologyConfigurations[name] = configuration
    }

    /**
     * 移除拓扑配置
     *
     * @param name 配置名称
     */
    fun removeTopologyConfiguration(name: String) {
        topologyConfigurations.remove(name)
        if (activeConfiguration == name) {
            activeConfiguration = null
        }
    }

    /**
     * 激活拓扑配置
     *
     * @param name 配置名称
     * @return 是否成功激活
     */
    fun activateTopologyConfiguration(name: String): Boolean {
        val configuration = topologyConfigurations[name] ?: return false

        rwLock.write {
            // 清除当前拓扑结构
            topology.clear()

            // 应用新的拓扑结构
            configuration.nodes.forEach { topology.addNode(it) }

            configuration.edges.forEach { (fromId, toId, relation) ->
                topology.addEdge(fromId, toId, relation)
            }

            activeConfiguration = name
        }

        return true
    }

    /**
     * 根据任务类型自动选择合适的拓扑配置
     *
     * @param taskType 任务类型
     * @return 是否成功激活
     */
    fun selectTopologyForTask(taskType: String): Boolean {
        // 查找匹配的拓扑配置
        val matchingConfig = topologyConfigurations.entries.find { (name, config) ->
            config.taskTypes.contains(taskType)
        }

        return if (matchingConfig != null) {
            activateTopologyConfiguration(matchingConfig.key)
        } else {
            false
        }
    }

    /**
     * 获取当前活动的拓扑配置名称
     *
     * @return 配置名称，如果没有活动配置则返回 null
     */
    fun getActiveConfiguration(): String? {
        return activeConfiguration
    }

    /**
     * 获取所有可用的拓扑配置名称
     *
     * @return 配置名称列表
     */
    fun getAvailableConfigurations(): List<String> {
        return topologyConfigurations.keys.toList()
    }

    /**
     * 获取拓扑配置
     *
     * @param name 配置名称
     * @return 拓扑配置，如果不存在则返回 null
     */
    fun getTopologyConfiguration(name: String): TopologyConfiguration? {
        return topologyConfigurations[name]
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

    /**
     * 创建拓扑配置的快照
     *
     * @return 当前拓扑结构的快照
     */
    fun createSnapshot(): TopologyConfiguration {
        val nodes = topology.getAllNodes()
        val edges = topology.getAllEdges()

        return TopologyConfiguration(
            nodes = nodes,
            edges = edges,
            taskTypes = emptyList()
        )
    }

    /**
     * 从快照恢复拓扑结构
     *
     * @param snapshot 拓扑结构快照
     */
    fun restoreFromSnapshot(snapshot: TopologyConfiguration) {
        rwLock.write {
            // 清除当前拓扑结构
            topology.clear()

            // 恢复节点
            snapshot.nodes.forEach { topology.addNode(it) }

            // 恢复边
            snapshot.edges.forEach { (fromId, toId, relation) ->
                topology.addEdge(fromId, toId, relation)
            }
        }
    }
}

/**
 * 拓扑配置，定义网络结构
 *
 * @property nodes 节点列表
 * @property edges 边列表，每条边由源节点 ID、目标节点 ID 和关系类型组成
 * @property taskTypes 适用的任务类型列表
 */
data class TopologyConfiguration(
    val nodes: List<String>,
    val edges: List<Triple<String, String, AgentRelation>>,
    val taskTypes: List<String>
)
