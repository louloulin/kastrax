package actor.proto.cluster

/**
 * GossipMemberState 表示八卦成员的状态
 */
data class GossipMemberState(
    /**
     * 成员 ID
     */
    val memberId: String,

    /**
     * 成员值映射
     */
    val values: Map<String, Any> = emptyMap(),

    /**
     * 成员版本映射
     */
    val versions: Map<String, Long> = emptyMap()
)

/**
 * MemberGossipState 表示成员的八卦状态
 */
data class MemberGossipState(
    /**
     * 成员状态映射
     */
    val members: Map<String, GossipMemberState> = emptyMap()
)
