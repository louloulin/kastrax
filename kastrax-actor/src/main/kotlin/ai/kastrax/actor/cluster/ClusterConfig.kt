package ai.kastrax.actor.cluster

import actor.proto.cluster.ClusterConfig as KactorClusterConfig
import java.time.Duration

/**
 * 集群配置类
 *
 * @property hostname 主机名，默认为 "0.0.0.0"
 * @property port 端口号，默认为 8090
 * @property clusterName 集群名称，默认为 "kastrax-cluster"
 * @property seeds 种子节点列表，默认为空列表
 * @property minClusterSize 最小集群大小，默认为 1
 * @property gossipInterval Gossip 间隔，默认为 1 秒
 * @property heartbeatInterval 心跳间隔，默认为 1 秒
 * @property monitorInterval 监控间隔，默认为 1 秒
 * @property deathThreshold 死亡阈值，默认为 5 秒
 */
data class ClusterConfig(
    val hostname: String = "0.0.0.0",
    val port: Int = 8090,
    val clusterName: String = "kastrax-cluster",
    val seeds: List<String> = emptyList(),
    val minClusterSize: Int = 1,
    val gossipInterval: Duration = Duration.ofSeconds(1),
    val heartbeatInterval: Duration = Duration.ofSeconds(1),
    val monitorInterval: Duration = Duration.ofSeconds(1),
    val deathThreshold: Duration = Duration.ofSeconds(5)
) {
    /**
     * 转换为 kactor 的 ClusterConfig
     */
    fun toKactorClusterConfig(): KactorClusterConfig {
        return KactorClusterConfig(
            clusterName = clusterName,
            address = "$hostname:$port",
            seeds = seeds,
            minClusterSize = minClusterSize,
            gossipInterval = gossipInterval,
            heartbeatInterval = heartbeatInterval,
            monitorInterval = monitorInterval,
            deathThreshold = deathThreshold
        )
    }
}
