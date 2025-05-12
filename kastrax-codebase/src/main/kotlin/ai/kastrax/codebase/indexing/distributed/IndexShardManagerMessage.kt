package ai.kastrax.codebase.indexing.distributed

/**
 * 索引分片管理器消息
 */
sealed class IndexShardManagerMessage {
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
     */
    data class GetShardResponse(val shardId: String) : IndexShardManagerMessage()

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
}

/**
 * 分片信息
 *
 * @property shardId 分片ID
 * @property nodeId 节点ID
 * @property status 状态
 * @property documentCount 文档数量
 */
data class ShardInfo(
    val shardId: String,
    val nodeId: String,
    val status: ShardStatus,
    val documentCount: Int
)

/**
 * 分片状态
 */
enum class ShardStatus {
    /**
     * 初始化中
     */
    INITIALIZING,
    
    /**
     * 活跃
     */
    ACTIVE,
    
    /**
     * 重新分配中
     */
    RELOCATING,
    
    /**
     * 不可用
     */
    UNAVAILABLE
}
