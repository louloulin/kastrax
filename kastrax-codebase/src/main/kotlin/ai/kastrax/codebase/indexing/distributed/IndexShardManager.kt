package ai.kastrax.codebase.indexing.distributed

import actor.proto.Actor
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private val logger = KotlinLogging.logger {}

// 索引分片管理器消息已移至 IndexShardManagerMessage.kt

/**
 * 分片状态
 *
 * @property documentCount 文档数量
 * @property indexSize 索引大小
 * @property cpuUsage CPU使用率
 * @property memoryUsage 内存使用率
 * @property isHealthy 是否健康
 */
// 分片状态和分片信息已移至 IndexShardManagerMessage.kt

// 索引分片管理器配置已移至 DistributedIndexSystemConfig.kt

/**
 * 索引分片管理器
 *
 * 负责管理索引分片的分配和平衡
 *
 * @property config 配置
 */
class IndexShardManager(private val config: IndexShardManagerConfig = IndexShardManagerConfig()) : Actor {
    // 分片信息
    private val shards = ConcurrentHashMap<String, ShardInfo>()

    // 节点到分片的映射
    private val nodeShards = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * 接收消息
     */
    // 删除重复的 receive 方法，使用下面的实现

    /**
     * 处理注册分片消息
     *
     * @param shardId 分片ID
     * @param nodeId 节点ID
     * @param pid 分片PID
     * @param metadata 元数据
     */
    private suspend fun Context.handleRegisterShard(
        shardId: String,
        nodeId: String,
        pid: PID,
        metadata: Map<String, Any>
    ) {
        logger.info("注册分片: $shardId, 节点: $nodeId")

        // 创建分片信息
        val shardInfo = ShardInfo(
            shardId = shardId,
            nodeId = nodeId,
            pid = pid,
            metadata = metadata,
            lastHeartbeat = Instant.now()
        )

        // 添加到分片映射
        shards[shardId] = shardInfo

        // 更新节点到分片的映射
        nodeShards.computeIfAbsent(nodeId) { mutableSetOf() }.add(shardId)

        // 响应注册成功
        respond(true)
    }

    /**
     * 处理注销分片消息
     *
     * @param shardId 分片ID
     * @param nodeId 节点ID
     */
    private suspend fun Context.handleUnregisterShard(shardId: String, nodeId: String) {
        logger.info("注销分片: $shardId, 节点: $nodeId")

        // 从分片映射中移除
        val shardInfo = shards.remove(shardId)

        if (shardInfo != null) {
            // 从节点到分片的映射中移除
            nodeShards[nodeId]?.remove(shardId)

            // 如果节点没有分片了，移除节点
            if (nodeShards[nodeId]?.isEmpty() == true) {
                nodeShards.remove(nodeId)
            }

            // 响应注销成功
            respond(true)
        } else {
            logger.warning("尝试注销不存在的分片: $shardId")

            // 响应注销失败
            respond(false)
        }
    }

    /**
     * 处理分片心跳消息
     *
     * @param shardId 分片ID
     * @param nodeId 节点ID
     * @param status 状态
     */
    private suspend fun Context.handleShardHeartbeat(shardId: String, nodeId: String, status: ShardStatus) {
        val shardInfo = shards[shardId]

        if (shardInfo != null && shardInfo.nodeId == nodeId) {
            // 更新分片状态
            shardInfo.status = status
            shardInfo.lastHeartbeat = Instant.now()

            logger.debug("分片心跳: $shardId, 节点: $nodeId, 健康: ${status.isHealthy}")
        } else {
            logger.warning("收到未注册分片的心跳: $shardId, 节点: $nodeId")
        }
    }

    /**
     * 处理获取分片消息
     *
     * @param key 键
     */
    private suspend fun Context.handleGetShard(key: String) {
        // 计算键的哈希值
        val hash = abs(key.hashCode())

        // 确定分片ID
        val shardId = "shard-${hash % config.shardCount}"

        // 查找分片信息
        val shardInfo = shards[shardId]

        // 响应分片信息
        respond(IndexShardManagerMessage.GetShardResponse(
            shardId = shardId,
            pid = shardInfo?.pid
        ))
    }

    /**
     * 处理获取所有分片消息
     */
    private suspend fun Context.handleGetAllShards() {
        respond(IndexShardManagerMessage.GetAllShardsResponse(shards.toMap()))
    }

    /**
     * 处理重新平衡分片消息
     */
    private suspend fun Context.handleRebalanceShards() {
        logger.info("开始重新平衡分片")

        // 检查是否有足够的节点
        if (nodeShards.size <= 1) {
            logger.info("节点数量不足，无需重新平衡")
            return
        }

        // 计算每个节点应该有的分片数量
        val totalShards = shards.size
        val targetShardsPerNode = totalShards / nodeShards.size

        // 找出分片过多和过少的节点
        val nodeShardCounts = nodeShards.mapValues { it.value.size }
        val overloadedNodes = nodeShardCounts.filter { it.value > targetShardsPerNode + 1 }
        val underloadedNodes = nodeShardCounts.filter { it.value < targetShardsPerNode }

        if (overloadedNodes.isEmpty() || underloadedNodes.isEmpty()) {
            logger.info("分片已经平衡，无需重新平衡")
            return
        }

        // 重新分配分片
        for ((overloadedNodeId, overloadCount) in overloadedNodes) {
            val excessShards = overloadCount - targetShardsPerNode

            if (excessShards <= 0) continue

            // 获取该节点的分片
            val nodeShardsSet = nodeShards[overloadedNodeId] ?: continue
            val nodeShardsToMove = nodeShardsSet.take(excessShards)

            // 为每个过载的分片找一个负载较低的节点
            for (shardId in nodeShardsToMove) {
                // 找出负载最低的节点
                val targetNodeId = underloadedNodes.keys.minByOrNull { nodeShardCounts[it] ?: Int.MAX_VALUE }
                    ?: continue

                // 移动分片
                val shardInfo = shards[shardId] ?: continue

                logger.info("移动分片 $shardId 从节点 $overloadedNodeId 到节点 $targetNodeId")

                // 通知相关节点
                // 注意：这里只是模拟，实际实现需要更复杂的协议

                // 更新映射
                nodeShardsSet.remove(shardId)
                nodeShards.computeIfAbsent(targetNodeId) { mutableSetOf() }.add(shardId)

                // 更新分片信息
                shards[shardId] = shardInfo.copy(nodeId = targetNodeId)

                // 更新节点分片计数
                val overloadedCount = nodeShardCounts.getOrDefault(overloadedNodeId, 0)
                val updatedOverloadedCount = overloadedCount - 1
                val mutableNodeShardCounts = nodeShardCounts.toMutableMap()
                mutableNodeShardCounts[overloadedNodeId] = updatedOverloadedCount

                val targetCount = nodeShardCounts.getOrDefault(targetNodeId, 0)
                val updatedTargetCount = targetCount + 1
                mutableNodeShardCounts[targetNodeId] = updatedTargetCount
            }
        }

        logger.info("分片重新平衡完成")
    }

    /**
     * 检查分片健康状态
     */
    private suspend fun Context.checkShardHealth() {
        val now = Instant.now()
        val timeoutThreshold = now.minusMillis(config.shardTimeoutDuration.inWholeMilliseconds)

        // 查找超时的分片
        val timedOutShards = shards.entries
            .filter { it.value.lastHeartbeat.isBefore(timeoutThreshold) }
            .map { it.key to it.value }

        for ((shardId, shardInfo) in timedOutShards) {
            logger.warning("分片超时: $shardId, 节点: ${shardInfo.nodeId}")

            // 从分片映射中移除
            shards.remove(shardId)

            // 从节点到分片的映射中移除
            nodeShards[shardInfo.nodeId]?.remove(shardId)

            // 如果节点没有分片了，移除节点
            if (nodeShards[shardInfo.nodeId]?.isEmpty() == true) {
                nodeShards.remove(shardInfo.nodeId)
            }
        }
    }

    /**
     * 启动定时任务
     */
    private suspend fun Context.startPeriodicTasks() {
        // 定期检查分片健康状态
        val healthCheckerProps = fromProducer {
            object : Actor {
                override suspend fun Context.receive(msg: Any) {
                    checkShardHealth()
                    delay(config.heartbeatInterval.inWholeMilliseconds)
                    send(self, "continue")
                }
            }
        }
        val healthChecker = spawnNamed(healthCheckerProps, "health-checker")
        send(healthChecker, "continue")

        // 定期重新平衡分片
        val rebalancerProps = fromProducer {
            object : Actor {
                override suspend fun Context.receive(msg: Any) {
                    send(self, IndexShardManagerMessage.RebalanceShards)
                    delay(config.rebalanceInterval.inWholeMilliseconds)
                    send(self, "continue")
                }
            }
        }
        val rebalancer = spawnNamed(rebalancerProps, "rebalancer")
        send(rebalancer, "continue")
    }

    /**
     * 处理启动消息
     */
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is IndexShardManagerMessage.RegisterShard -> handleRegisterShard(
                msg.shardId,
                msg.nodeId,
                msg.pid,
                msg.metadata
            )
            is IndexShardManagerMessage.UnregisterShard -> handleUnregisterShard(msg.shardId, msg.nodeId)
            is IndexShardManagerMessage.ShardHeartbeat -> handleShardHeartbeat(msg.shardId, msg.nodeId, msg.status)
            is IndexShardManagerMessage.GetShard -> handleGetShard(msg.key)
            is IndexShardManagerMessage.RebalanceShards -> handleRebalanceShards()
            "started" -> {
                logger.info("索引分片管理器已启动")
                startPeriodicTasks()
            }
            else -> logger.warning("未知消息类型: ${msg::class.simpleName}")
        }
    }

    companion object {
        /**
         * 创建 Props
         *
         * @param config 配置
         * @return Props
         */
        fun props(config: IndexShardManagerConfig = IndexShardManagerConfig()): Props {
            return fromProducer { IndexShardManager(config) }
        }
    }
}
