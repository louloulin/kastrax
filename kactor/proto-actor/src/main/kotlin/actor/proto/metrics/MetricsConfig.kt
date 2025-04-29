package actor.proto.metrics

/**
 * 度量系统配置
 */
data class MetricsConfig(
    /**
     * 是否启用度量系统
     */
    val enabled: Boolean = true,
    
    /**
     * 是否启用 Actor 创建度量
     */
    val enableActorCreationMetrics: Boolean = true,
    
    /**
     * 是否启用 Actor 停止度量
     */
    val enableActorStopMetrics: Boolean = true,
    
    /**
     * 是否启用消息发送度量
     */
    val enableMessageSendMetrics: Boolean = true,
    
    /**
     * 是否启用消息接收度量
     */
    val enableMessageReceiveMetrics: Boolean = true,
    
    /**
     * 是否启用 Mailbox 度量
     */
    val enableMailboxMetrics: Boolean = true,
    
    /**
     * 是否启用 Dispatcher 度量
     */
    val enableDispatcherMetrics: Boolean = true,
    
    /**
     * 是否启用 Remote 度量
     */
    val enableRemoteMetrics: Boolean = true,
    
    /**
     * 是否启用 Cluster 度量
     */
    val enableClusterMetrics: Boolean = true,
    
    /**
     * 是否启用 Router 度量
     */
    val enableRouterMetrics: Boolean = true,
    
    /**
     * 是否启用 Persistence 度量
     */
    val enablePersistenceMetrics: Boolean = true,
    
    /**
     * 是否启用 JVM 度量
     */
    val enableJvmMetrics: Boolean = true,
    
    /**
     * 度量前缀
     */
    val prefix: String = "protoactor"
)
