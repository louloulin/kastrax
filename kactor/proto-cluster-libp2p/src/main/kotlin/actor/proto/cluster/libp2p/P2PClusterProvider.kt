package actor.proto.cluster.libp2p

import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterProvider
import actor.proto.cluster.Member
import actor.proto.cluster.MemberStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * P2PClusterProvider 是基于 libp2p 的集群提供者实现
 * 注意: 当前实现是简化版本，仅用于演示
 */
class P2PClusterProvider(
    val config: P2PClusterConfig
) : ClusterProvider {
    private lateinit var cluster: Cluster
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var isRunning = false

    private lateinit var discovery: P2PDiscovery
    private lateinit var protocol: P2PClusterProtocol
    private lateinit var identityLookup: P2PIdentityLookup

    override suspend fun startMember(cluster: Cluster): Boolean {
        this.cluster = cluster

        logger.info { "Starting P2P cluster member at ${cluster.actorSystem.address}" }

        // 初始化组件
        discovery = P2PDiscovery(config, cluster)
        protocol = P2PClusterProtocol(this)
        identityLookup = P2PIdentityLookup()

        // 启动组件
        discovery.start()
        protocol.start()
        identityLookup.setup(cluster, cluster.getClusterKinds(), false)

        // 注册集群成员
        registerMember()

        isRunning = true

        // 启动心跳
        startHeartbeat()

        return true
    }

    override suspend fun startClient(cluster: Cluster): Boolean {
        this.cluster = cluster

        logger.info { "Starting P2P cluster client at ${cluster.actorSystem.address}" }

        // 初始化组件
        discovery = P2PDiscovery(config, cluster)
        protocol = P2PClusterProtocol(this)
        identityLookup = P2PIdentityLookup()

        // 启动组件
        discovery.start()
        protocol.start()
        identityLookup.setup(cluster, cluster.getClusterKinds(), true)

        isRunning = true

        return true
    }

    override suspend fun shutdown(graceful: Boolean): Boolean {
        if (!isRunning) return true

        logger.info { "Shutting down P2P cluster provider" }

        // 停止组件
        discovery.stop()
        protocol.stop()
        identityLookup.shutdown()

        isRunning = false

        return true
    }

    /**
     * 注册集群成员
     */
    private fun registerMember() {
        // 将自己注册为集群成员
        val member = createMemberInfo()
        cluster.memberList.addMember(member)
    }

    /**
     * 获取身份查找服务
     */
    fun getIdentityLookup(): P2PIdentityLookup = identityLookup

    /**
     * 创建成员信息
     */
    private fun createMemberInfo(): Member {
        return Member(
            id = cluster.actorSystem.address,
            host = cluster.actorSystem.address,
            port = config.listenPort,
            labels = mapOf("kinds" to cluster.getClusterKinds().keys.joinToString(",")),
            status = MemberStatus.ALIVE
        )
    }

    /**
     * 启动心跳
     */
    private fun startHeartbeat() {
        scope.launch {
            while (isRunning) {
                try {
                    // 发送心跳
                    val member = createMemberInfo()
                    cluster.memberList.addMember(member)

                    // 等待下一个心跳间隔
                    delay(config.heartbeatInterval.toMillis())
                } catch (e: Exception) {
                    logger.error(e) { "Error in heartbeat" }
                }
            }
        }
    }
}
