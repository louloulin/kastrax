package actor.proto.cluster.grain

import actor.proto.cluster.Cluster
import actor.proto.persistence.Provider
import java.lang.reflect.Proxy
import kotlin.reflect.KClass

/**
 * GrainFactory is responsible for creating grain instances.
 */
object GrainFactory {
    /**
     * Get a grain instance.
     * @param grainInterface The interface of the grain.
     * @param identity The identity of the grain.
     * @param cluster The cluster to use.
     * @return The grain instance.
     */
    fun <T : Grain> getGrain(grainInterface: KClass<T>, identity: String, cluster: Cluster): T {
        val kind = grainInterface.simpleName ?: throw IllegalArgumentException("Grain interface must have a name")
        
        @Suppress("UNCHECKED_CAST")
        return Proxy.newProxyInstance(
            grainInterface.java.classLoader,
            arrayOf(grainInterface.java)
        ) { _, method, args ->
            when (method.name) {
                "getIdentity" -> identity
                "getKind" -> kind
                else -> {
                    // Create a grain reference
                    val grainRef = GrainReference(cluster, identity, kind)
                    
                    // Call the method on the grain reference
                    val result = method.invoke(grainRef, *args ?: emptyArray())
                    
                    // Return the result
                    result
                }
            }
        } as T
    }
    
    /**
     * Get a grain instance with persistence.
     * @param grainInterface The interface of the grain.
     * @param identity The identity of the grain.
     * @param cluster The cluster to use.
     * @param provider The persistence provider to use.
     * @return The grain instance.
     */
    suspend fun <T : Grain> getGrainWithPersistence(
        grainInterface: KClass<T>,
        identity: String,
        cluster: Cluster,
        provider: Provider
    ): T {
        val grain = getGrain(grainInterface, identity, cluster)
        
        // Initialize the grain with the persistence provider
        if (grain is GrainBase) {
            grain.initPersistence(provider)
        }
        
        return grain
    }
}

/**
 * GrainReference is a reference to a grain.
 */
class GrainReference(
    cluster: Cluster,
    identity: String,
    kind: String
) : GrainBase(cluster, identity, kind)
