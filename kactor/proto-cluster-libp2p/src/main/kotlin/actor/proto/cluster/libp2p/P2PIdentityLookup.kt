package actor.proto.cluster.libp2p

import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterIdentity
import actor.proto.cluster.IdentityLookup
import actor.proto.cluster.Kind
import actor.proto.cluster.MemberStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * P2PIdentityLookup 负责虚拟 Actor 定位
 * 注意: 当前实现是简化版本，仅用于演示
 */
class P2PIdentityLookup : IdentityLookup {
    private lateinit var cluster: Cluster
    private var isClient = false

    override suspend fun setup(cluster: Cluster, kinds: Map<String, Kind>, isClient: Boolean) {
        this.cluster = cluster
        this.isClient = isClient
        logger.info { "Setting up P2PIdentityLookup as ${if (isClient) "client" else "member"}" }
    }

    override suspend fun lookup(clusterIdentity: ClusterIdentity): PID {
        // 检查是否在缓存中
        val cachedPid = cluster.pidCache.get(clusterIdentity)
        if (cachedPid != null) {
            return cachedPid
        }

        // 确定应该托管 Actor 的节点
        val memberId = cluster.memberList.getPartitionMember(clusterIdentity)
            ?: throw Exception("No member available for kind ${clusterIdentity.kind}")

        // 如果是本地节点，激活 Actor
        if (memberId == cluster.actorSystem.address && !isClient) {
            return activateLocally(clusterIdentity)
        }

        // 否则创建远程 PID
        val pid = PID(memberId, "${clusterIdentity.kind}/${clusterIdentity.identity}")
        cluster.pidCache.add(clusterIdentity, pid)
        return pid
    }

    fun shutdown() {
        logger.info { "Shutting down P2PIdentityLookup" }
    }

    /**
     * 在本地激活 Actor
     */
    private suspend fun activateLocally(clusterIdentity: ClusterIdentity): PID {
        logger.debug { "Activating actor locally: $clusterIdentity" }

        // 获取 kind 的激活器
        // 验证 kind 是否存在
        if (!cluster.getClusterKinds().containsKey(clusterIdentity.kind)) {
            throw Exception("No cluster kind found for ${clusterIdentity.kind}")
        }

        // 激活 Actor
        val pid = PID(cluster.actorSystem.address, "${clusterIdentity.kind}/${clusterIdentity.identity}")

        // 添加到缓存
        cluster.pidCache.add(clusterIdentity, pid)

        return pid
    }


}
