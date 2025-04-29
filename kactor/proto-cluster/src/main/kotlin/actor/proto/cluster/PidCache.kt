package actor.proto.cluster

import actor.proto.PID
import java.util.concurrent.ConcurrentHashMap

/**
 * PidCache caches PIDs for virtual actors.
 */
class PidCache {
    private val cache = ConcurrentHashMap<String, ConcurrentHashMap<String, PID>>()
    private val reverseCache = ConcurrentHashMap<String, ClusterIdentity>()
    
    /**
     * Add a PID to the cache.
     * @param clusterIdentity The cluster identity of the actor.
     * @param pid The PID of the actor.
     */
    fun add(clusterIdentity: ClusterIdentity, pid: PID) {
        val (identity, kind) = clusterIdentity
        
        // Add to forward cache
        val kindCache = cache.computeIfAbsent(kind) { ConcurrentHashMap() }
        kindCache[identity] = pid
        
        // Add to reverse cache
        reverseCache[pid.id] = clusterIdentity
    }
    
    /**
     * Get a PID from the cache.
     * @param identity The identity of the actor.
     * @param kind The kind of the actor.
     * @return The PID of the actor, or null if not found.
     */
    fun get(identity: String, kind: String): PID? {
        return cache[kind]?.get(identity)
    }
    
    /**
     * Get a PID from the cache.
     * @param clusterIdentity The cluster identity of the actor.
     * @return The PID of the actor, or null if not found.
     */
    fun get(clusterIdentity: ClusterIdentity): PID? {
        return get(clusterIdentity.identity, clusterIdentity.kind)
    }
    
    /**
     * Remove a PID from the cache.
     * @param identity The identity of the actor.
     * @param kind The kind of the actor.
     * @return The removed PID, or null if not found.
     */
    fun remove(identity: String, kind: String): PID? {
        val kindCache = cache[kind] ?: return null
        val pid = kindCache.remove(identity) ?: return null
        
        // Remove from reverse cache
        reverseCache.remove(pid.id)
        
        return pid
    }
    
    /**
     * Remove a PID from the cache.
     * @param clusterIdentity The cluster identity of the actor.
     * @return The removed PID, or null if not found.
     */
    fun remove(clusterIdentity: ClusterIdentity): PID? {
        return remove(clusterIdentity.identity, clusterIdentity.kind)
    }
    
    /**
     * Remove a PID from the cache by value.
     * @param identity The identity of the actor.
     * @param kind The kind of the actor.
     * @param pid The PID to remove.
     * @return True if the PID was removed, false otherwise.
     */
    fun removeByValue(identity: String, kind: String, pid: PID): Boolean {
        val kindCache = cache[kind] ?: return false
        val cachedPid = kindCache[identity] ?: return false
        
        if (cachedPid == pid) {
            kindCache.remove(identity)
            reverseCache.remove(pid.id)
            return true
        }
        
        return false
    }
    
    /**
     * Remove all PIDs for a member from the cache.
     * @param memberId The ID of the member.
     */
    fun removeByMember(memberId: String) {
        // Find all PIDs for the member
        val pidsToRemove = mutableListOf<Pair<String, String>>()
        
        for ((kind, kindCache) in cache) {
            for ((identity, pid) in kindCache) {
                if (pid.address == memberId) {
                    pidsToRemove.add(identity to kind)
                    reverseCache.remove(pid.id)
                }
            }
        }
        
        // Remove all PIDs
        for ((identity, kind) in pidsToRemove) {
            cache[kind]?.remove(identity)
        }
    }
    
    /**
     * Get the cluster identity for a PID.
     * @param pid The PID of the actor.
     * @return The cluster identity of the actor, or null if not found.
     */
    fun getClusterIdentity(pid: PID): ClusterIdentity? {
        return reverseCache[pid.id]
    }
    
    /**
     * Clear the cache.
     */
    fun clear() {
        cache.clear()
        reverseCache.clear()
    }
}
