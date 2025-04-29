package actor.proto

import actor.proto.util.MurmurHash3
import java.util.concurrent.ConcurrentHashMap

/**
 * A sharded map for storing processes.
 * This implementation reduces lock contention by distributing processes across multiple shards.
 * 
 * @param shardCount The number of shards to use (default: 1024)
 */
class ShardedProcessMap(private val shardCount: Int = 1024) {
    private val shards = Array(shardCount) { ConcurrentHashMap<String, Process>() }
    
    /**
     * Get the shard for the given key
     * @param key The key to get the shard for
     * @return The shard for the key
     */
    private fun getShard(key: String): ConcurrentHashMap<String, Process> {
        val hash = MurmurHash3.hash32(key.toByteArray())
        val index = Math.abs(hash % shardCount)
        return shards[index]
    }
    
    /**
     * Put a process in the map
     * @param key The key to store the process under
     * @param process The process to store
     * @return The previous process associated with the key, or null if there was no mapping
     */
    fun put(key: String, process: Process): Process? = getShard(key).put(key, process)
    
    /**
     * Put a process in the map if the key is not already present
     * @param key The key to store the process under
     * @param process The process to store
     * @return null if the key was not present, or the current process if the key was present
     */
    fun putIfAbsent(key: String, process: Process): Process? = getShard(key).putIfAbsent(key, process)
    
    /**
     * Get a process from the map
     * @param key The key to get the process for
     * @return The process associated with the key, or null if there is no mapping
     */
    fun get(key: String): Process? = getShard(key).get(key)
    
    /**
     * Get a process from the map, or return the default value if there is no mapping
     * @param key The key to get the process for
     * @param defaultValue The default value to return if there is no mapping
     * @return The process associated with the key, or the default value if there is no mapping
     */
    fun getOrDefault(key: String, defaultValue: Process): Process = getShard(key).getOrDefault(key, defaultValue)
    
    /**
     * Remove a process from the map
     * @param key The key to remove the process for
     * @return The process that was removed, or null if there was no mapping
     */
    fun remove(key: String): Process? = getShard(key).remove(key)
    
    /**
     * Get all keys in the map
     * @return A sequence of all keys in the map
     */
    fun keys(): Sequence<String> = shards.asSequence().flatMap { it.keys.asSequence() }
    
    /**
     * Get the number of processes in the map
     * @return The number of processes in the map
     */
    fun size(): Int = shards.sumOf { it.size }
    
    /**
     * Check if the map is empty
     * @return true if the map is empty, false otherwise
     */
    fun isEmpty(): Boolean = shards.all { it.isEmpty() }
    
    /**
     * Clear the map
     */
    fun clear() {
        shards.forEach { it.clear() }
    }
}
