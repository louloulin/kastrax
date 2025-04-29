package actor.proto.cluster

/**
 * Member represents a node in the cluster.
 */
data class Member(
    val id: String,
    val host: String,
    val port: Int,
    val labels: Map<String, String> = emptyMap(),
    val status: MemberStatus = MemberStatus.ALIVE,
    val heartbeat: ULong = 0u
) {
    /**
     * Get the address of the member.
     * @return The address of the member in the format "host:port".
     */
    fun address(): String = "$host:$port"
    
    /**
     * Convert the member to a proto message.
     * @return The proto message.
     */
    fun toProto(): ClusterProtos.Member {
        return ClusterProtos.Member.newBuilder()
            .setId(id)
            .setHost(host)
            .setPort(port)
            .putAllLabels(labels)
            .setStatus(status.toProto())
            .setHeartbeat(heartbeat.toLong())
            .build()
    }
    
    companion object {
        /**
         * Create a member from a proto message.
         * @param proto The proto message.
         * @return The member.
         */
        fun fromProto(proto: ClusterProtos.Member): Member {
            return Member(
                id = proto.id,
                host = proto.host,
                port = proto.port,
                labels = proto.labelsMap,
                status = MemberStatus.fromProto(proto.status),
                heartbeat = proto.heartbeat.toULong()
            )
        }
    }
}

/**
 * MemberStatus represents the status of a member.
 */
enum class MemberStatus {
    ALIVE,
    LEAVING,
    UNAVAILABLE;
    
    /**
     * Convert the member status to a proto enum.
     * @return The proto enum.
     */
    fun toProto(): ClusterProtos.MemberStatus {
        return when (this) {
            ALIVE -> ClusterProtos.MemberStatus.ALIVE
            LEAVING -> ClusterProtos.MemberStatus.LEAVING
            UNAVAILABLE -> ClusterProtos.MemberStatus.UNAVAILABLE
        }
    }
    
    companion object {
        /**
         * Create a member status from a proto enum.
         * @param proto The proto enum.
         * @return The member status.
         */
        fun fromProto(proto: ClusterProtos.MemberStatus): MemberStatus {
            return when (proto) {
                ClusterProtos.MemberStatus.ALIVE -> ALIVE
                ClusterProtos.MemberStatus.LEAVING -> LEAVING
                ClusterProtos.MemberStatus.UNAVAILABLE -> UNAVAILABLE
                else -> UNAVAILABLE
            }
        }
    }
}
