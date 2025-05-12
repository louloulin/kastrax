package ai.kastrax.codebase.indexing.distributed

/**
 * 索引分片管理器消息
 */
sealed class IndexShardManagerMessage {
    /**
     * 注册分片消息
     *
     * @property shardId 分片ID
     * @property nodeId 节点ID
     * @property pid 分片PID
     * @property metadata 元数据
     */
    data class RegisterShard(
        val shardId: String,
        val nodeId: String,
        val pid: actor.proto.PID,
        val metadata: Map<String, Any> = emptyMap()
    ) : IndexShardManagerMessage()

    /**
     * 注销分片消息
     *
     * @property shardId 分片ID
     * @property nodeId 节点ID
     */
    data class UnregisterShard(val shardId: String, val nodeId: String) : IndexShardManagerMessage()

    /**
     * 分片心跳消息
     *
     * @property shardId 分片ID
     * @property nodeId 节点ID
     * @property status 状态
     */
    data class ShardHeartbeat(
        val shardId: String,
        val nodeId: String,
        val status: ShardStatus
    ) : IndexShardManagerMessage()

    /**
     * 获取分片消息
     *
     * @property key 键
     */
    data class GetShard(val key: String) : IndexShardManagerMessage()

    /**
     * 获取分片响应消息
     *
     * @property shardId 分片ID
     * @property pid 分片PID
     */
    data class GetShardResponse(val shardId: String, val pid: actor.proto.PID? = null) : IndexShardManagerMessage()

    /**
     * 获取所有分片消息
     */
    object GetAllShards : IndexShardManagerMessage()

    /**
     * 获取所有分片响应消息
     *
     * @property shards 分片信息映射
     */
    data class GetAllShardsResponse(val shards: Map<String, ShardInfo>) : IndexShardManagerMessage()

    /**
     * 重新平衡分片消息
     */
    object RebalanceShards : IndexShardManagerMessage()
}

/**
 * 分片信息
 *
 * @property shardId 分片ID
 * @property nodeId 节点ID
 * @property pid 分片PID
 * @property status 状态
 * @property metadata 元数据
 * @property lastHeartbeat 最后心跳时间
 */
data class ShardInfo(
    val shardId: String,
    val nodeId: String,
    val pid: actor.proto.PID,
    var status: ShardStatus = ShardStatus(),
    val metadata: Map<String, Any> = emptyMap(),
    var lastHeartbeat: java.time.Instant = java.time.Instant.now()
)

/**
 * 分片状态
 *
 * @property documentCount 文档数量
 * @property indexSize 索引大小
 * @property cpuUsage CPU使用率
 * @property memoryUsage 内存使用率
 * @property isHealthy 是否健康
 */
data class ShardStatus(
    val documentCount: Long = 0,
    val indexSize: Long = 0,
    val cpuUsage: Double = 0.0,
    val memoryUsage: Double = 0.0,
    val isHealthy: Boolean = true
)
