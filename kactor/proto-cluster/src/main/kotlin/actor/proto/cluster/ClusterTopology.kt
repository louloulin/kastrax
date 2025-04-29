package actor.proto.cluster

/**
 * ClusterTopology represents the current state of the cluster.
 */
data class ClusterTopology(
    val members: List<String>,
    val joined: List<String>,
    val left: List<String>,
    val blocked: List<String> = emptyList(),
    val topologyHash: ULong = 0u
) {
    /**
     * Convert the cluster topology to a proto message.
     * @return The proto message.
     */
    fun toProto(): ClusterProtos.ClusterTopology {
        return ClusterProtos.ClusterTopology.newBuilder()
            .addAllMembers(members)
            .addAllJoined(joined)
            .addAllLeft(left)
            .addAllBlocked(blocked)
            .setTopologyHash(topologyHash.toLong())
            .build()
    }
    
    companion object {
        /**
         * Create a cluster topology from a proto message.
         * @param proto The proto message.
         * @return The cluster topology.
         */
        fun fromProto(proto: ClusterProtos.ClusterTopology): ClusterTopology {
            return ClusterTopology(
                members = proto.membersList,
                joined = proto.joinedList,
                left = proto.leftList,
                blocked = proto.blockedList,
                topologyHash = proto.topologyHash.toULong()
            )
        }
    }
}
