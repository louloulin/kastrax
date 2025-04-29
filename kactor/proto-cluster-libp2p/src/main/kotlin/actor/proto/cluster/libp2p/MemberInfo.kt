package actor.proto.cluster.libp2p

/**
 * MemberInfo 表示集群成员的信息
 */
data class MemberInfo(
    /**
     * 成员 ID，通常是 ActorSystem 的地址
     */
    val id: String,
    
    /**
     * 主机地址，对于 libp2p 来说是 PeerId
     */
    val host: String,
    
    /**
     * 端口，对于 libp2p 来说不重要，设为 0
     */
    val port: Int,
    
    /**
     * 该成员支持的 Actor 类型
     */
    val kinds: List<String>
)
