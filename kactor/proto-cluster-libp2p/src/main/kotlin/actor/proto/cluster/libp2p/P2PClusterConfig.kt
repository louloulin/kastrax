package actor.proto.cluster.libp2p

import java.time.Duration

/**
 * P2PClusterConfig 是基于 libp2p 的集群配置类
 */
data class P2PClusterConfig(
    /**
     * 集群名称，用于区分不同的集群
     */
    val clusterName: String,

    /**
     * 是否启用 mDNS 发现
     */
    val enableMDns: Boolean = true,

    /**
     * 是否启用 DHT 发现
     */
    val enableDHT: Boolean = true,

    /**
     * 种子节点列表，格式为 "PeerId@/ip4/address/tcp/port"
     */
    val seedNodes: List<String> = emptyList(),

    /**
     * 引导超时时间
     */
    val bootstrapTimeout: Duration = Duration.ofSeconds(30),

    /**
     * 心跳间隔时间
     */
    val heartbeatInterval: Duration = Duration.ofSeconds(5),

    /**
     * 监控间隔时间
     */
    val monitorInterval: Duration = Duration.ofSeconds(15),

    /**
     * Gossip 扇出数量，即每次 gossip 消息发送给多少个节点
     */
    val gossipFanout: Int = 3,

    /**
     * 监听地址，默认为 0.0.0.0
     */
    val listenAddress: String = "0.0.0.0",

    /**
     * 监听端口，默认为 0（随机端口）
     */
    val listenPort: Int = 0,

    /**
     * DHT 刷新间隔
     */
    val dhtRefreshInterval: Duration = Duration.ofMinutes(5),

    /**
     * DHT 查找超时
     */
    val dhtLookupTimeout: Duration = Duration.ofSeconds(10),

    /**
     * DHT 存储超时
     */
    val dhtPutTimeout: Duration = Duration.ofSeconds(10),

    /**
     * Actor 激活超时
     */
    val activationTimeout: Duration = Duration.ofSeconds(10)
)
