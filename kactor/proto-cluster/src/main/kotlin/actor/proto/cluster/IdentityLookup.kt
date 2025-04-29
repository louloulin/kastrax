package actor.proto.cluster

import actor.proto.PID

/**
 * IdentityLookup is responsible for looking up virtual actors in the cluster.
 */
interface IdentityLookup {
    /**
     * Set up the identity lookup.
     * @param cluster The cluster to set up for.
     * @param kinds The kinds to set up for.
     * @param isClient Whether this is a client.
     */
    suspend fun setup(cluster: Cluster, kinds: Map<String, Kind>, isClient: Boolean)
    
    /**
     * Look up a virtual actor by cluster identity.
     * @param clusterIdentity The cluster identity of the actor.
     * @return The PID of the actor.
     */
    suspend fun lookup(clusterIdentity: ClusterIdentity): PID
}
