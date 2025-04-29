package actor.proto.cluster.libp2p

import actor.proto.cluster.Cluster
import actor.proto.cluster.MemberStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

private val logger = KotlinLogging.logger {}

/**
 * P2PFailureDetector 负责故障检测和恢复
 */
class P2PFailureDetector(
    private val cluster: Cluster,
    private val config: P2PClusterConfig
) {
    companion object {
        private const val DEFAULT_SUSPECT_TIMEOUT_FACTOR = 5 // 心跳间隔的倍数
        private const val DEFAULT_DEAD_TIMEOUT_FACTOR = 10 // 心跳间隔的倍数
    }
    
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isRunning = false
    
    // 最后一次心跳时间
    private val lastHeartbeats = ConcurrentHashMap<String, Long>()
    
    // 可疑节点
    private val suspectedNodes = ConcurrentHashMap<String, Long>()
    
    // 统计信息
    private val heartbeatsReceived = AtomicInteger(0)
    private val pingsSent = AtomicInteger(0)
    private val pingsReceived = AtomicInteger(0)
    private val nodesMarkedSuspected = AtomicInteger(0)
    private val nodesMarkedDead = AtomicInteger(0)
    private val nodesRecovered = AtomicInteger(0)
    
    /**
     * 启动故障检测器
     */
    fun start() {
        if (isRunning) return
        
        logger.info { "Starting P2P failure detector" }
        
        isRunning = true
        
        // 启动监控任务
        startMonitoringTask()
    }
    
    /**
     * 停止故障检测器
     */
    fun stop() {
        if (!isRunning) return
        
        logger.info { "Stopping P2P failure detector" }
        
        isRunning = false
        
        // 清理状态
        lastHeartbeats.clear()
        suspectedNodes.clear()
    }
    
    /**
     * 启动监控任务
     */
    private fun startMonitoringTask() {
        scope.launch {
            while (isRunning) {
                try {
                    // 检查节点状态
                    checkNodeStatus()
                    
                    // 等待下一个监控间隔
                    delay(config.monitorInterval.toMillis())
                } catch (e: Exception) {
                    logger.error(e) { "Error monitoring node status" }
                    delay(1000) // 出错后等待一段时间再重试
                }
            }
        }
    }
    
    /**
     * 检查节点状态
     */
    private fun checkNodeStatus() {
        val now = System.currentTimeMillis()
        val members = cluster.memberList.getMembers()
        
        // 检查每个成员
        members.forEach { member ->
            // 跳过自己
            if (member.id == cluster.actorSystem.address) return@forEach
            
            // 获取最后一次心跳时间
            val lastHeartbeat = lastHeartbeats[member.id] ?: 0
            
            // 计算超时阈值
            val suspectTimeout = config.heartbeatInterval.toMillis() * DEFAULT_SUSPECT_TIMEOUT_FACTOR
            val deadTimeout = config.heartbeatInterval.toMillis() * DEFAULT_DEAD_TIMEOUT_FACTOR
            
            when {
                // 如果节点已经标记为离开或不可用，跳过
                member.status == MemberStatus.LEAVING || member.status == MemberStatus.UNAVAILABLE -> {
                    // 不处理
                }
                
                // 如果节点已经可疑，检查是否应该标记为死亡
                suspectedNodes.containsKey(member.id) -> {
                    val suspectedTime = suspectedNodes[member.id] ?: 0
                    
                    if (now - suspectedTime > deadTimeout) {
                        // 标记为不可用
                        markNodeAsUnavailable(member.id)
                    } else {
                        // 尝试 ping 节点
                        pingNode(member.id)
                    }
                }
                
                // 如果节点心跳超时，标记为可疑
                now - lastHeartbeat > suspectTimeout -> {
                    // 标记为可疑
                    markNodeAsSuspected(member.id)
                    
                    // 尝试 ping 节点
                    pingNode(member.id)
                }
                
                // 否则节点正常
                else -> {
                    // 如果节点之前被标记为可疑，现在恢复了
                    if (suspectedNodes.containsKey(member.id)) {
                        // 标记为恢复
                        markNodeAsRecovered(member.id)
                    }
                }
            }
        }
    }
    
    /**
     * 标记节点为可疑
     */
    private fun markNodeAsSuspected(nodeId: String) {
        // 如果节点已经被标记为可疑，不重复标记
        if (suspectedNodes.containsKey(nodeId)) return
        
        logger.info { "Marking node as suspected: $nodeId" }
        
        // 记录可疑时间
        suspectedNodes[nodeId] = System.currentTimeMillis()
        
        // 更新统计信息
        nodesMarkedSuspected.incrementAndGet()
    }
    
    /**
     * 标记节点为不可用
     */
    private fun markNodeAsUnavailable(nodeId: String) {
        logger.info { "Marking node as unavailable: $nodeId" }
        
        // 从可疑节点列表中移除
        suspectedNodes.remove(nodeId)
        
        // 更新成员状态
        cluster.memberList.updateMemberStatus(nodeId, MemberStatus.UNAVAILABLE)
        
        // 更新统计信息
        nodesMarkedDead.incrementAndGet()
    }
    
    /**
     * 标记节点为恢复
     */
    private fun markNodeAsRecovered(nodeId: String) {
        logger.info { "Node recovered: $nodeId" }
        
        // 从可疑节点列表中移除
        suspectedNodes.remove(nodeId)
        
        // 更新成员状态
        cluster.memberList.updateMemberStatus(nodeId, MemberStatus.ALIVE)
        
        // 更新统计信息
        nodesRecovered.incrementAndGet()
    }
    
    /**
     * Ping 节点
     */
    private fun pingNode(nodeId: String) {
        // 更新统计信息
        pingsSent.incrementAndGet()
        
        // 在实际实现中，这里会使用 libp2p 发送 ping 消息
        // 这里使用模拟实现
        logger.debug { "Pinging node: $nodeId" }
    }
    
    /**
     * 处理 ping 响应
     */
    fun handlePingResponse(nodeId: String) {
        // 更新统计信息
        pingsReceived.incrementAndGet()
        
        // 更新最后一次心跳时间
        recordHeartbeat(nodeId)
    }
    
    /**
     * 记录心跳
     */
    fun recordHeartbeat(nodeId: String) {
        // 更新最后一次心跳时间
        lastHeartbeats[nodeId] = System.currentTimeMillis()
        
        // 更新统计信息
        heartbeatsReceived.incrementAndGet()
        
        // 如果节点之前被标记为可疑，现在恢复了
        if (suspectedNodes.containsKey(nodeId)) {
            // 标记为恢复
            markNodeAsRecovered(nodeId)
        }
    }
    
    /**
     * 获取统计信息
     */
    fun getStats(): FailureDetectorStats {
        return FailureDetectorStats(
            heartbeatsReceived = heartbeatsReceived.get(),
            pingsSent = pingsSent.get(),
            pingsReceived = pingsReceived.get(),
            nodesMarkedSuspected = nodesMarkedSuspected.get(),
            nodesMarkedDead = nodesMarkedDead.get(),
            nodesRecovered = nodesRecovered.get(),
            suspectedNodesCount = suspectedNodes.size
        )
    }
}

/**
 * 故障检测器统计信息
 */
data class FailureDetectorStats(
    val heartbeatsReceived: Int,
    val pingsSent: Int,
    val pingsReceived: Int,
    val nodesMarkedSuspected: Int,
    val nodesMarkedDead: Int,
    val nodesRecovered: Int,
    val suspectedNodesCount: Int
)
