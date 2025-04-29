package actor.proto.cluster.grain

import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterIdentity
import actor.proto.persistence.Provider
import actor.proto.request
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.concurrent.CompletableFuture

/**
 * Grain is the base interface for all grains.
 */
interface Grain {
    /**
     * Get the identity of the grain.
     * @return The identity of the grain.
     */
    fun getIdentity(): String

    /**
     * Get the kind of the grain.
     * @return The kind of the grain.
     */
    fun getKind(): String
}

/**
 * GrainBase is the base class for all grains.
 */
abstract class GrainBase(
    private val cluster: Cluster,
    private val identity: String,
    private val kind: String
) : Grain {
    private var pid: PID? = null

    override fun getIdentity(): String = identity

    override fun getKind(): String = kind

    /**
     * Get the PID of the grain.
     * @return The PID of the grain.
     */
    suspend fun getPID(): PID {
        return pid ?: cluster.get(identity, kind).also { pid = it }
    }

    /**
     * Send a message to the grain.
     * @param message The message to send.
     */
    suspend fun send(message: Any) {
        val pid = getPID()
        cluster.actorSystem.send(pid, message)
    }

    /**
     * Request a response from the grain.
     * @param message The message to send.
     * @param timeout The timeout in milliseconds.
     * @return The response.
     */
    suspend fun <T> request(message: Any, timeout: Long): T {
        val pid = getPID()
        return cluster.actorSystem.requestAsync<T>(pid, message, java.time.Duration.ofMillis(timeout))
    }

    /**
     * Request a response from the grain asynchronously.
     * @param message The message to send.
     * @param timeout The timeout in milliseconds.
     * @return A CompletableFuture that will complete with the response.
     */
    fun <T> requestAsync(message: Any, timeout: Long): CompletableFuture<T> {
        val future = CompletableFuture<T>()
        GlobalScope.launch {
            try {
                val result = request<T>(message, timeout)
                future.complete(result)
            } catch (e: Exception) {
                future.completeExceptionally(e)
            }
        }
        return future
    }

    /**
     * Initialize the grain with a persistence provider.
     * @param provider The persistence provider.
     */
    suspend fun initPersistence(provider: Provider) {
        val pid = getPID()
        cluster.actorSystem.send(pid, provider)
    }
}
