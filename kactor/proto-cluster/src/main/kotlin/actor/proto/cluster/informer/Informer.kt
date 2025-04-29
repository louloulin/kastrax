package actor.proto.cluster.informer

import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterTopology
import actor.proto.cluster.Member
import actor.proto.cluster.MemberStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * Informer 接口定义了集群信息提供者的行为
 * 它负责监控集群成员的状态，并通知集群拓扑变化
 */
interface Informer {
    /**
     * 获取集群成员列表
     * @return 集群成员列表
     */
    fun getMembers(): List<Member>

    /**
     * 获取集群成员
     * @param id 成员ID
     * @return 成员信息，如果不存在则返回null
     */
    fun getMember(id: String): Member?

    /**
     * 添加集群成员
     * @param member 要添加的成员
     */
    fun addMember(member: Member)

    /**
     * 移除集群成员
     * @param id 要移除的成员ID
     */
    fun removeMember(id: String)

    /**
     * 更新成员状态
     * @param id 成员ID
     * @param status 新的状态
     */
    fun updateMemberStatus(id: String, status: MemberStatus)

    /**
     * 初始化Informer
     * @param cluster 集群实例
     */
    fun initialize(cluster: Cluster)

    /**
     * 关闭Informer
     */
    fun shutdown()
}

/**
 * InformerEvent 是Informer发出的事件基类
 */
sealed class InformerEvent

/**
 * MemberStatusChangedEvent 表示成员状态变化事件
 * @param memberId 成员ID
 * @param oldStatus 旧状态
 * @param newStatus 新状态
 */
data class MemberStatusChangedEvent(
    val memberId: String,
    val oldStatus: MemberStatus,
    val newStatus: MemberStatus
) : InformerEvent()

/**
 * MemberJoinedEvent 表示成员加入事件
 * @param member 加入的成员
 */
data class MemberJoinedEvent(val member: Member) : InformerEvent()

/**
 * MemberLeftEvent 表示成员离开事件
 * @param memberId 离开的成员ID
 */
data class MemberLeftEvent(val memberId: String) : InformerEvent()

/**
 * TopologyChangedEvent 表示拓扑变化事件
 * @param topology 新的拓扑
 */
data class TopologyChangedEvent(val topology: ClusterTopology) : InformerEvent()
