package actor.proto.cluster.informer

import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterTopology
import actor.proto.cluster.Member
import actor.proto.cluster.MemberStatus
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = KotlinLogging.logger {}

/**
 * DefaultInformer 是 Informer 接口的默认实现
 * 它使用内存存储集群成员信息，并通过事件流通知变化
 */
class DefaultInformer : Informer {
    private val lock = ReentrantReadWriteLock()
    private val members = ConcurrentHashMap<String, Member>()
    private lateinit var cluster: Cluster

    /**
     * 初始化Informer
     * @param cluster 集群实例
     */
    override fun initialize(cluster: Cluster) {
        this.cluster = cluster
        logger.info { "DefaultInformer initialized for cluster ${cluster.config.name}" }
    }

    /**
     * 获取集群成员列表
     * @return 集群成员列表
     */
    override fun getMembers(): List<Member> {
        return lock.read { members.values.toList() }
    }

    /**
     * 获取集群成员
     * @param id 成员ID
     * @return 成员信息，如果不存在则返回null
     */
    override fun getMember(id: String): Member? {
        return lock.read { members[id] }
    }

    /**
     * 添加集群成员
     * @param member 要添加的成员
     */
    override fun addMember(member: Member) {
        lock.write {
            val oldMember = members[member.id]
            members[member.id] = member

            // 发布成员加入事件
            if (oldMember == null) {
                val event = MemberJoinedEvent(member)
                cluster.actorSystem.eventStream().publish(event)
                
                // 发布拓扑变化事件
                val topology = ClusterTopology(
                    members = members.keys.toList(),
                    joined = listOf(member.id),
                    left = emptyList(),
                    blocked = emptyList()
                )
                cluster.actorSystem.eventStream().publish(TopologyChangedEvent(topology))
                
                logger.info { "Member joined: ${member.id} (${member.host}:${member.port})" }
            } else if (oldMember.status != member.status) {
                // 发布状态变化事件
                val event = MemberStatusChangedEvent(
                    memberId = member.id,
                    oldStatus = oldMember.status,
                    newStatus = member.status
                )
                cluster.actorSystem.eventStream().publish(event)
                
                logger.info { "Member status changed: ${member.id} ${oldMember.status} -> ${member.status}" }
            }
        }
    }

    /**
     * 移除集群成员
     * @param id 要移除的成员ID
     */
    override fun removeMember(id: String) {
        lock.write {
            val oldMember = members.remove(id)

            // 发布成员离开事件
            if (oldMember != null) {
                val event = MemberLeftEvent(id)
                cluster.actorSystem.eventStream().publish(event)
                
                // 发布拓扑变化事件
                val topology = ClusterTopology(
                    members = members.keys.toList(),
                    joined = emptyList(),
                    left = listOf(id),
                    blocked = emptyList()
                )
                cluster.actorSystem.eventStream().publish(TopologyChangedEvent(topology))
                
                logger.info { "Member left: $id" }
            }
        }
    }

    /**
     * 更新成员状态
     * @param id 成员ID
     * @param status 新的状态
     */
    override fun updateMemberStatus(id: String, status: MemberStatus) {
        lock.write {
            val member = members[id] ?: return@write
            val oldStatus = member.status
            
            if (oldStatus != status) {
                members[id] = member.copy(status = status)
                
                // 发布状态变化事件
                val event = MemberStatusChangedEvent(
                    memberId = id,
                    oldStatus = oldStatus,
                    newStatus = status
                )
                cluster.actorSystem.eventStream().publish(event)
                
                logger.info { "Member status changed: $id $oldStatus -> $status" }
            }
        }
    }

    /**
     * 关闭Informer
     */
    override fun shutdown() {
        lock.write {
            members.clear()
        }
        logger.info { "DefaultInformer shutdown" }
    }
}
