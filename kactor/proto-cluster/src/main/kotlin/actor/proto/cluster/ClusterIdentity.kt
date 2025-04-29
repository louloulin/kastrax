package actor.proto.cluster

/**
 * ClusterIdentity uniquely identifies an actor in the cluster.
 */
data class ClusterIdentity(
    val identity: String,
    val kind: String
) {
    /**
     * Convert the cluster identity to a proto message.
     * @return The proto message.
     */
    fun toProto(): ClusterProtos.ClusterIdentity {
        return ClusterProtos.ClusterIdentity.newBuilder()
            .setIdentity(identity)
            .setKind(kind)
            .build()
    }
    
    companion object {
        /**
         * Create a cluster identity from a proto message.
         * @param proto The proto message.
         * @return The cluster identity.
         */
        fun fromProto(proto: ClusterProtos.ClusterIdentity): ClusterIdentity {
            return ClusterIdentity(
                identity = proto.identity,
                kind = proto.kind
            )
        }
    }
}
