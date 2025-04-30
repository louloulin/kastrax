package actor.proto

import actor.proto.util.MurmurHash3
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

object ProcessRegistry {
    const val noHost: String = "nonhost"
    private val hostResolvers: MutableList<(PID) -> Process?> = mutableListOf()
    private val processLookup = ShardedMap(1024)
    private val sequenceId = AtomicLong(0)
    var address: String = noHost

    /**
     * Get all registered processes
     * @return A sequence of PIDs for all registered processes
     */
    fun processes(): Sequence<PID> = processLookup.keys().map { PID(address, it) }

    fun registerHostResolver(resolver: (PID) -> Process?) {
        hostResolvers.add(resolver)
    }

    fun get(localId: String): Process = processLookup.getOrDefault(localId, DeadLetterProcess)
    fun get(pid: PID): Process = when {
        pid.isLocal() -> processLookup.getOrDefault(pid.id, DeadLetterProcess)
        else -> resolveProcess(pid)
    }

    private fun resolveProcess(pid: PID): Process {
        hostResolvers
                .mapNotNull { it(pid) }
                .forEach { return it }

        return DeadLetterProcess
    }

    fun put(id: String, process: Process): PID {
        val originalId = id
        var currentId = originalId
        var attempt = 0

        while (true) {
            val pid = PID(address, currentId)
            pid.cachedProcess_ = process // we know what pid points to what process here

            if (processLookup.putIfAbsent(pid.id, process) == null) {
                return pid
            }

            // 如果进程名称已存在，生成一个新的唯一名称
            attempt++
            currentId = "$originalId-${System.currentTimeMillis()}-${System.nanoTime() % 10000}-$attempt"
        }
    }

    fun remove(pid: PID) {
        processLookup.remove(pid.id)
    }

    fun nextId(): String = "$${sequenceId.incrementAndGet()}"
}

/**
 * 分片 Map 实现，用于减少锁竞争
 * @param shardCount 分片数量
 */
private class ShardedMap(private val shardCount: Int) {
    private val shards = Array(shardCount) { ConcurrentHashMap<String, Process>() }

    /**
     * 获取给定键的分片
     * @param key 键
     * @return 分片
     */
    private fun getShard(key: String): ConcurrentHashMap<String, Process> {
        val hash = MurmurHash3.hash32(key.toByteArray())
        val index = Math.abs(hash % shardCount)
        return shards[index]
    }

    /**
     * 将进程放入 Map
     * @param key 键
     * @param process 进程
     * @return 之前与键关联的进程，如果没有则为 null
     */
    fun put(key: String, process: Process): Process? = getShard(key).put(key, process)

    /**
     * 如果键不存在，则将进程放入 Map
     * @param key 键
     * @param process 进程
     * @return 如果键不存在则为 null，否则为当前进程
     */
    fun putIfAbsent(key: String, process: Process): Process? = getShard(key).putIfAbsent(key, process)

    /**
     * 从 Map 中获取进程
     * @param key 键
     * @return 与键关联的进程，如果没有则为 null
     */
    fun get(key: String): Process? = getShard(key).get(key)

    /**
     * 从 Map 中获取进程，如果没有则返回默认值
     * @param key 键
     * @param defaultValue 默认值
     * @return 与键关联的进程，如果没有则为默认值
     */
    fun getOrDefault(key: String, defaultValue: Process): Process = getShard(key).getOrDefault(key, defaultValue)

    /**
     * 从 Map 中移除进程
     * @param key 键
     * @return 被移除的进程，如果没有则为 null
     */
    fun remove(key: String): Process? = getShard(key).remove(key)

    /**
     * 获取 Map 中的所有键
     * @return Map 中的所有键的序列
     */
    fun keys(): Sequence<String> = shards.asSequence().flatMap { it.keys.asSequence() }

    /**
     * 获取 Map 中的进程数量
     * @return Map 中的进程数量
     */
    fun size(): Int = shards.sumOf { it.size }

    /**
     * 检查 Map 是否为空
     * @return 如果 Map 为空则为 true，否则为 false
     */
    fun isEmpty(): Boolean = shards.all { it.isEmpty() }

    /**
     * 清空 Map
     */
    fun clear() {
        shards.forEach { it.clear() }
    }
}

