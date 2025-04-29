package actor.proto.cluster

/**
 * ClusterProvider is responsible for managing cluster membership.
 * It handles node discovery, heartbeating, and membership events.
 */
interface ClusterProvider {
    /**
     * Start the cluster provider as a member.
     * @param cluster The cluster to start.
     */
    suspend fun startMember(cluster: Cluster): Boolean

    /**
     * Start the cluster provider as a client.
     * @param cluster The cluster to start.
     */
    suspend fun startClient(cluster: Cluster): Boolean

    /**
     * Shutdown the cluster provider.
     * @param graceful Whether to shutdown gracefully.
     */
    suspend fun shutdown(graceful: Boolean): Boolean
}
