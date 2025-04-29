package ai.kastrax.actor.cluster

import actor.proto.cluster.ClusterConfig as KactorClusterConfig
import actor.proto.cluster.ClusterProvider
import actor.proto.cluster.IdentityLookup
import actor.proto.cluster.providers.AutoManagedClusterProvider
import actor.proto.cluster.providers.DistributedHashIdentityLookup
import actor.proto.remote.RemoteConfig
import java.time.Duration

/**
 * 集群配置类
 *
 * @property hostname 主机名，默认为 "0.0.0.0"
 * @property port 端口号，默认为 8090
 * @property clusterName 集群名称，默认为 "kastrax-cluster"
 * @property seeds 种子节点列表，默认为空列表
 * @property requestTimeout 请求超时时间，默认为 5 秒
 * @property gossipInterval Gossip 间隔，默认为 300 毫秒
 */
data class ClusterConfig(
    val hostname: String = "0.0.0.0",
    val port: Int = 8090,
    val clusterName: String = "kastrax-cluster",
    val seeds: List<String> = emptyList(),
    val requestTimeout: Duration = Duration.ofSeconds(5),
    val gossipInterval: Duration = Duration.ofMillis(300)
) {
    /**
     * 转换为 kactor 的 ClusterConfig
     */
    fun toKactorClusterConfig(): KactorClusterConfig {
        // 创建远程配置
        val remoteConfig = RemoteConfig.create(hostname, port)

        // 创建集群提供者
        val clusterProvider = AutoManagedClusterProvider(
            port = port,
            seedNodes = seeds
        )

        // 创建身份查找
        val identityLookup = DistributedHashIdentityLookup()

        // 创建集群配置
        return KactorClusterConfig.create(
            name = clusterName,
            clusterProvider = clusterProvider,
            identityLookup = identityLookup,
            remoteConfig = remoteConfig,
            actor.proto.cluster.WithRequestTimeout(requestTimeout),
            actor.proto.cluster.WithGossipInterval(gossipInterval)
        )
    }
}
