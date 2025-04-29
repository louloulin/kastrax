package actor.proto.cluster.providers

import actor.proto.Actor
import actor.proto.Context
import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterIdentity
import actor.proto.cluster.IdentityLookup
import actor.proto.cluster.Kind
import actor.proto.fromProducer
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeoutException

private val logger = KotlinLogging.logger {}

/**
 * DistributedHashIdentityLookup is an identity lookup that uses distributed hashing to locate actors.
 */
class DistributedHashIdentityLookup : IdentityLookup {
    private lateinit var cluster: Cluster
    private val kinds = ConcurrentHashMap<String, Kind>()
    private var isClient = false

    override suspend fun setup(cluster: Cluster, kinds: Map<String, Kind>, isClient: Boolean) {
        this.cluster = cluster
        this.kinds.putAll(kinds)
        this.isClient = isClient

        // Start the placement actor
        val props = fromProducer { PlacementActor(this) }
        cluster.actorSystem.actorOf(props, "placement")
    }

    override suspend fun lookup(clusterIdentity: ClusterIdentity): PID {
        // Check if the PID is in the cache
        val cachedPid = cluster.pidCache.get(clusterIdentity)
        if (cachedPid != null) {
            return cachedPid
        }

        // Find the member that should host this actor
        val memberId = cluster.memberList.getPartitionMember(clusterIdentity)
            ?: throw TimeoutException("No members available for kind ${clusterIdentity.kind}")

        // If we're the member that should host this actor, activate it locally
        if (memberId == cluster.actorSystem.address && !isClient) {
            return activateLocally(clusterIdentity)
        }

        // Otherwise, request activation from the member
        return requestActivation(memberId, clusterIdentity)
    }

    /**
     * Activate an actor locally.
     * @param clusterIdentity The cluster identity of the actor.
     * @return The PID of the activated actor.
     */
    private suspend fun activateLocally(clusterIdentity: ClusterIdentity): PID {
        val kind = kinds[clusterIdentity.kind]
            ?: throw IllegalArgumentException("Unknown kind ${clusterIdentity.kind}")

        // Create the actor
        val pid = cluster.actorSystem.actorOf(kind.props, clusterIdentity.identity)

        // Add to the cache
        cluster.pidCache.add(clusterIdentity, pid)

        return pid
    }

    /**
     * Request activation from a member.
     * @param memberId The ID of the member.
     * @param clusterIdentity The cluster identity of the actor.
     * @return The PID of the activated actor.
     */
    private suspend fun requestActivation(memberId: String, clusterIdentity: ClusterIdentity): PID {
        // TODO: Implement remote activation request
        // For now, just create a PID for the remote actor
        val pid = PID(memberId, clusterIdentity.identity)

        // Add to the cache
        cluster.pidCache.add(clusterIdentity, pid)

        return pid
    }
}

/**
 * PlacementActor handles placement-related messages.
 */
class PlacementActor(private val lookup: DistributedHashIdentityLookup) : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is ActivationRequest -> {
                try {
                    val pid = lookup.lookup(msg.clusterIdentity)
                    sender?.let { send(it, ActivationResponse(pid)) }
                } catch (e: Exception) {
                    logger.error("Error activating actor ${msg.clusterIdentity}", e)
                    sender?.let { send(it, ActivationResponse(null)) }
                }
            }
        }
    }
}

/**
 * ActivationRequest represents a request to activate an actor.
 */
data class ActivationRequest(
    val clusterIdentity: ClusterIdentity
)

/**
 * ActivationResponse represents a response to an activation request.
 */
data class ActivationResponse(
    val pid: PID?
)
