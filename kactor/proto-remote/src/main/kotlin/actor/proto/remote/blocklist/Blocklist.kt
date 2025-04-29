package actor.proto.remote.blocklist

import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Blocklist 接口定义了远程节点阻止列表的基本操作
 */
interface Blocklist {
    /**
     * 检查地址是否被阻止
     * @param address 要检查的地址
     * @return 如果地址被阻止则返回 true，否则返回 false
     */
    fun isBlocked(address: String): Boolean
    
    /**
     * 阻止地址
     * @param address 要阻止的地址
     * @param duration 阻止的持续时间，如果为 null 则永久阻止
     * @return 如果地址被成功阻止则返回 true，否则返回 false
     */
    fun block(address: String, duration: Duration? = null): Boolean
    
    /**
     * 解除地址的阻止
     * @param address 要解除阻止的地址
     * @return 如果地址被成功解除阻止则返回 true，否则返回 false
     */
    fun unblock(address: String): Boolean
    
    /**
     * 获取所有被阻止的地址
     * @return 被阻止的地址集合
     */
    fun getBlockedAddresses(): Set<String>
    
    /**
     * 获取地址的阻止信息
     * @param address 要获取信息的地址
     * @return 地址的阻止信息，如果地址未被阻止则返回 null
     */
    fun getBlockInfo(address: String): BlockInfo?
    
    /**
     * 清除所有过期的阻止
     * @return 被清除的阻止数量
     */
    fun clearExpired(): Int
    
    /**
     * 清除所有阻止
     */
    fun clearAll()
}

/**
 * 阻止信息
 * @param address 被阻止的地址
 * @param blockedAt 阻止的时间
 * @param duration 阻止的持续时间，如果为 null 则永久阻止
 * @param reason 阻止的原因
 * @param failureCount 失败次数
 */
data class BlockInfo(
    val address: String,
    val blockedAt: Instant,
    val duration: Duration? = null,
    val reason: String? = null,
    val failureCount: Int = 0
) {
    /**
     * 检查阻止是否已过期
     * @return 如果阻止已过期则返回 true，否则返回 false
     */
    fun isExpired(): Boolean {
        if (duration == null) {
            return false // 永久阻止
        }
        val expiresAt = blockedAt.plus(duration)
        return Instant.now().isAfter(expiresAt)
    }
    
    /**
     * 获取阻止的过期时间
     * @return 阻止的过期时间，如果是永久阻止则返回 null
     */
    fun expiresAt(): Instant? {
        return if (duration == null) null else blockedAt.plus(duration)
    }
}

/**
 * 默认的 Blocklist 实现
 */
class DefaultBlocklist : Blocklist {
    private val blockedAddresses = ConcurrentHashMap<String, BlockInfo>()
    private val lock = ReentrantReadWriteLock()
    
    override fun isBlocked(address: String): Boolean {
        return lock.read {
            val blockInfo = blockedAddresses[address] ?: return false
            if (blockInfo.isExpired()) {
                // 如果阻止已过期，则移除它
                blockedAddresses.remove(address)
                return false
            }
            true
        }
    }
    
    override fun block(address: String, duration: Duration?): Boolean {
        return lock.write {
            val existing = blockedAddresses[address]
            if (existing != null && !existing.isExpired()) {
                // 如果地址已经被阻止且未过期，则不做任何操作
                false
            } else {
                // 创建新的阻止信息
                val blockInfo = BlockInfo(
                    address = address,
                    blockedAt = Instant.now(),
                    duration = duration
                )
                blockedAddresses[address] = blockInfo
                true
            }
        }
    }
    
    override fun unblock(address: String): Boolean {
        return lock.write {
            blockedAddresses.remove(address) != null
        }
    }
    
    override fun getBlockedAddresses(): Set<String> {
        return lock.read {
            // 返回所有未过期的阻止地址
            blockedAddresses.entries
                .filter { !it.value.isExpired() }
                .map { it.key }
                .toSet()
        }
    }
    
    override fun getBlockInfo(address: String): BlockInfo? {
        return lock.read {
            val blockInfo = blockedAddresses[address] ?: return null
            if (blockInfo.isExpired()) {
                // 如果阻止已过期，则移除它
                blockedAddresses.remove(address)
                return null
            }
            blockInfo
        }
    }
    
    override fun clearExpired(): Int {
        return lock.write {
            val expiredAddresses = blockedAddresses.entries
                .filter { it.value.isExpired() }
                .map { it.key }
            
            expiredAddresses.forEach { blockedAddresses.remove(it) }
            expiredAddresses.size
        }
    }
    
    override fun clearAll() {
        lock.write {
            blockedAddresses.clear()
        }
    }
}

/**
 * 带失败计数的 Blocklist 实现
 * @param maxFailures 允许的最大失败次数，超过此次数将阻止地址
 * @param blockDuration 阻止的持续时间，如果为 null 则永久阻止
 */
class FailureCountingBlocklist(
    private val maxFailures: Int = 3,
    private val blockDuration: Duration = Duration.ofMinutes(5)
) : Blocklist {
    private val blockedAddresses = ConcurrentHashMap<String, BlockInfo>()
    private val failureCounts = ConcurrentHashMap<String, AtomicInteger>()
    private val lock = ReentrantReadWriteLock()
    
    /**
     * 记录地址的失败
     * @param address 失败的地址
     * @param reason 失败的原因
     * @return 如果地址被阻止则返回 true，否则返回 false
     */
    fun recordFailure(address: String, reason: String? = null): Boolean {
        return lock.write {
            // 如果地址已经被阻止，则不做任何操作
            if (isBlocked(address)) {
                return false
            }
            
            // 增加失败计数
            val count = failureCounts.computeIfAbsent(address) { AtomicInteger(0) }.incrementAndGet()
            
            // 如果失败次数超过最大值，则阻止地址
            if (count >= maxFailures) {
                val blockInfo = BlockInfo(
                    address = address,
                    blockedAt = Instant.now(),
                    duration = blockDuration,
                    reason = reason,
                    failureCount = count
                )
                blockedAddresses[address] = blockInfo
                failureCounts.remove(address) // 重置失败计数
                return true
            }
            
            false
        }
    }
    
    /**
     * 重置地址的失败计数
     * @param address 要重置的地址
     */
    fun resetFailureCount(address: String) {
        lock.write {
            failureCounts.remove(address)
        }
    }
    
    /**
     * 获取地址的失败计数
     * @param address 要获取的地址
     * @return 地址的失败计数
     */
    fun getFailureCount(address: String): Int {
        return lock.read {
            failureCounts[address]?.get() ?: 0
        }
    }
    
    override fun isBlocked(address: String): Boolean {
        return lock.read {
            val blockInfo = blockedAddresses[address] ?: return false
            if (blockInfo.isExpired()) {
                // 如果阻止已过期，则移除它
                blockedAddresses.remove(address)
                return false
            }
            true
        }
    }
    
    override fun block(address: String, duration: Duration?): Boolean {
        return lock.write {
            val existing = blockedAddresses[address]
            if (existing != null && !existing.isExpired()) {
                // 如果地址已经被阻止且未过期，则不做任何操作
                false
            } else {
                // 创建新的阻止信息
                val blockInfo = BlockInfo(
                    address = address,
                    blockedAt = Instant.now(),
                    duration = duration ?: blockDuration,
                    failureCount = failureCounts[address]?.get() ?: 0
                )
                blockedAddresses[address] = blockInfo
                failureCounts.remove(address) // 重置失败计数
                true
            }
        }
    }
    
    override fun unblock(address: String): Boolean {
        return lock.write {
            val removed = blockedAddresses.remove(address) != null
            if (removed) {
                failureCounts.remove(address) // 重置失败计数
            }
            removed
        }
    }
    
    override fun getBlockedAddresses(): Set<String> {
        return lock.read {
            // 返回所有未过期的阻止地址
            blockedAddresses.entries
                .filter { !it.value.isExpired() }
                .map { it.key }
                .toSet()
        }
    }
    
    override fun getBlockInfo(address: String): BlockInfo? {
        return lock.read {
            val blockInfo = blockedAddresses[address] ?: return null
            if (blockInfo.isExpired()) {
                // 如果阻止已过期，则移除它
                blockedAddresses.remove(address)
                return null
            }
            blockInfo
        }
    }
    
    override fun clearExpired(): Int {
        return lock.write {
            val expiredAddresses = blockedAddresses.entries
                .filter { it.value.isExpired() }
                .map { it.key }
            
            expiredAddresses.forEach { blockedAddresses.remove(it) }
            expiredAddresses.size
        }
    }
    
    override fun clearAll() {
        lock.write {
            blockedAddresses.clear()
            failureCounts.clear()
        }
    }
}
