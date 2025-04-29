package actor.proto.remote.blocklist

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.fromProducer
import kotlinx.coroutines.delay
import mu.KotlinLogging
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * Blocklist 管理器，负责管理远程节点的阻止列表
 * @param actorSystem Actor 系统
 * @param cleanupInterval 清理过期阻止的间隔时间
 */
class BlocklistManager(
    private val actorSystem: ActorSystem,
    private val cleanupInterval: Duration = Duration.ofMinutes(1)
) {
    // 默认的阻止列表
    private val defaultBlocklist = FailureCountingBlocklist()
    
    // 按地址类型分组的阻止列表
    private val typedBlocklists = ConcurrentHashMap<String, Blocklist>()
    
    // 清理 Actor 的 PID
    private lateinit var cleanupActorPid: PID
    
    /**
     * 初始化 Blocklist 管理器
     */
    fun initialize() {
        // 启动清理 Actor
        val props = fromProducer { CleanupActor(this, cleanupInterval) }
        cleanupActorPid = actorSystem.actorOf(props)
        
        logger.info { "Blocklist manager initialized with cleanup interval: $cleanupInterval" }
    }
    
    /**
     * 关闭 Blocklist 管理器
     */
    fun shutdown() {
        // 停止清理 Actor
        actorSystem.stop(cleanupActorPid)
        
        logger.info { "Blocklist manager shutdown" }
    }
    
    /**
     * 获取默认的阻止列表
     * @return 默认的阻止列表
     */
    fun getDefaultBlocklist(): Blocklist {
        return defaultBlocklist
    }
    
    /**
     * 获取指定类型的阻止列表
     * @param addressType 地址类型
     * @return 指定类型的阻止列表
     */
    fun getBlocklist(addressType: String): Blocklist {
        return typedBlocklists.computeIfAbsent(addressType) { FailureCountingBlocklist() }
    }
    
    /**
     * 注册阻止列表
     * @param addressType 地址类型
     * @param blocklist 阻止列表
     */
    fun registerBlocklist(addressType: String, blocklist: Blocklist) {
        typedBlocklists[addressType] = blocklist
        logger.info { "Registered blocklist for address type: $addressType" }
    }
    
    /**
     * 检查地址是否被阻止
     * @param address 要检查的地址
     * @param addressType 地址类型，如果为 null 则使用默认阻止列表
     * @return 如果地址被阻止则返回 true，否则返回 false
     */
    fun isBlocked(address: String, addressType: String? = null): Boolean {
        val blocklist = if (addressType != null) {
            typedBlocklists[addressType] ?: defaultBlocklist
        } else {
            defaultBlocklist
        }
        
        return blocklist.isBlocked(address)
    }
    
    /**
     * 阻止地址
     * @param address 要阻止的地址
     * @param duration 阻止的持续时间，如果为 null 则永久阻止
     * @param addressType 地址类型，如果为 null 则使用默认阻止列表
     * @param reason 阻止的原因
     * @return 如果地址被成功阻止则返回 true，否则返回 false
     */
    fun block(
        address: String,
        duration: Duration? = null,
        addressType: String? = null,
        reason: String? = null
    ): Boolean {
        val blocklist = if (addressType != null) {
            typedBlocklists[addressType] ?: defaultBlocklist
        } else {
            defaultBlocklist
        }
        
        val result = blocklist.block(address, duration)
        if (result) {
            logger.info { "Blocked address: $address, type: $addressType, duration: $duration, reason: $reason" }
        }
        
        return result
    }
    
    /**
     * 解除地址的阻止
     * @param address 要解除阻止的地址
     * @param addressType 地址类型，如果为 null 则使用默认阻止列表
     * @return 如果地址被成功解除阻止则返回 true，否则返回 false
     */
    fun unblock(address: String, addressType: String? = null): Boolean {
        val blocklist = if (addressType != null) {
            typedBlocklists[addressType] ?: defaultBlocklist
        } else {
            defaultBlocklist
        }
        
        val result = blocklist.unblock(address)
        if (result) {
            logger.info { "Unblocked address: $address, type: $addressType" }
        }
        
        return result
    }
    
    /**
     * 记录地址的失败
     * @param address 失败的地址
     * @param addressType 地址类型，如果为 null 则使用默认阻止列表
     * @param reason 失败的原因
     * @return 如果地址被阻止则返回 true，否则返回 false
     */
    fun recordFailure(address: String, addressType: String? = null, reason: String? = null): Boolean {
        val blocklist = if (addressType != null) {
            typedBlocklists[addressType] ?: defaultBlocklist
        } else {
            defaultBlocklist
        }
        
        if (blocklist is FailureCountingBlocklist) {
            val result = blocklist.recordFailure(address, reason)
            if (result) {
                logger.info { "Address blocked due to failures: $address, type: $addressType, reason: $reason" }
            } else {
                logger.debug { "Recorded failure for address: $address, type: $addressType, reason: $reason" }
            }
            return result
        }
        
        return false
    }
    
    /**
     * 重置地址的失败计数
     * @param address 要重置的地址
     * @param addressType 地址类型，如果为 null 则使用默认阻止列表
     */
    fun resetFailureCount(address: String, addressType: String? = null) {
        val blocklist = if (addressType != null) {
            typedBlocklists[addressType] ?: defaultBlocklist
        } else {
            defaultBlocklist
        }
        
        if (blocklist is FailureCountingBlocklist) {
            blocklist.resetFailureCount(address)
            logger.debug { "Reset failure count for address: $address, type: $addressType" }
        }
    }
    
    /**
     * 获取地址的失败计数
     * @param address 要获取的地址
     * @param addressType 地址类型，如果为 null 则使用默认阻止列表
     * @return 地址的失败计数
     */
    fun getFailureCount(address: String, addressType: String? = null): Int {
        val blocklist = if (addressType != null) {
            typedBlocklists[addressType] ?: defaultBlocklist
        } else {
            defaultBlocklist
        }
        
        return if (blocklist is FailureCountingBlocklist) {
            blocklist.getFailureCount(address)
        } else {
            0
        }
    }
    
    /**
     * 获取所有被阻止的地址
     * @param addressType 地址类型，如果为 null 则使用默认阻止列表
     * @return 被阻止的地址集合
     */
    fun getBlockedAddresses(addressType: String? = null): Set<String> {
        val blocklist = if (addressType != null) {
            typedBlocklists[addressType] ?: defaultBlocklist
        } else {
            defaultBlocklist
        }
        
        return blocklist.getBlockedAddresses()
    }
    
    /**
     * 获取地址的阻止信息
     * @param address 要获取信息的地址
     * @param addressType 地址类型，如果为 null 则使用默认阻止列表
     * @return 地址的阻止信息，如果地址未被阻止则返回 null
     */
    fun getBlockInfo(address: String, addressType: String? = null): BlockInfo? {
        val blocklist = if (addressType != null) {
            typedBlocklists[addressType] ?: defaultBlocklist
        } else {
            defaultBlocklist
        }
        
        return blocklist.getBlockInfo(address)
    }
    
    /**
     * 清除所有过期的阻止
     * @return 被清除的阻止数量
     */
    fun clearExpired(): Int {
        var total = 0
        
        // 清除默认阻止列表中的过期阻止
        total += defaultBlocklist.clearExpired()
        
        // 清除所有类型的阻止列表中的过期阻止
        for (blocklist in typedBlocklists.values) {
            total += blocklist.clearExpired()
        }
        
        if (total > 0) {
            logger.debug { "Cleared $total expired blocks" }
        }
        
        return total
    }
    
    /**
     * 清除所有阻止
     */
    fun clearAll() {
        // 清除默认阻止列表
        defaultBlocklist.clearAll()
        
        // 清除所有类型的阻止列表
        for (blocklist in typedBlocklists.values) {
            blocklist.clearAll()
        }
        
        logger.info { "Cleared all blocks" }
    }
}

/**
 * 清理 Actor，定期清除过期的阻止
 * @param blocklistManager Blocklist 管理器
 * @param cleanupInterval 清理间隔时间
 */
class CleanupActor(
    private val blocklistManager: BlocklistManager,
    private val cleanupInterval: Duration
) : actor.proto.Actor {
    override suspend fun actor.proto.Context.receive(msg: Any) {
        when (msg) {
            is actor.proto.Started -> {
                // 启动定期清理任务
                startCleanupTask()
            }
        }
    }
    
    private suspend fun startCleanupTask() {
        while (true) {
            try {
                // 清除过期的阻止
                blocklistManager.clearExpired()
                
                // 等待下一次清理
                delay(cleanupInterval.toMillis())
            } catch (e: Exception) {
                logger.error(e) { "Error in blocklist cleanup task" }
                delay(1000) // 出错后等待一段时间再重试
            }
        }
    }
}
