package actor.proto.cluster.consensus

import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.future.await
import kotlinx.coroutines.withTimeout

/**
 * Consensus 接口定义了集群共识机制的基本操作
 */
interface Consensus {
    /**
     * 获取共识处理器的唯一标识符
     * @return 共识处理器的唯一标识符
     */
    fun getId(): String

    /**
     * 尝试获取共识值
     * @return 如果达成共识，返回共识值和 true；否则返回 null 和 false
     */
    fun tryGetConsensus(): Pair<Any?, Boolean>

    /**
     * 尝试设置共识值
     * @param value 要设置的共识值
     * @return 如果成功设置共识值，返回 true；否则返回 false
     */
    fun trySetConsensus(value: Any?): Boolean

    /**
     * 尝试重置共识状态
     * @return 如果成功重置共识状态，返回 true；否则返回 false
     */
    fun tryResetConsensus(): Boolean

    /**
     * 等待达成共识
     * @param timeoutMs 超时时间（毫秒）
     * @return 如果在超时时间内达成共识，返回共识值；否则返回 null
     */
    suspend fun waitForConsensus(timeoutMs: Long): Any?
}

/**
 * ConsensusResult 类用于存储共识结果
 */
class ConsensusResult {
    private val consensusReached = AtomicBoolean(false)
    private val consensusValue = AtomicReference<Any?>(null)
    private val future = CompletableFuture<Any?>()

    /**
     * 尝试设置共识值
     * @param value 要设置的共识值
     * @return 如果成功设置共识值，返回 true；否则返回 false
     */
    fun trySetConsensus(value: Any?): Boolean {
        if (consensusReached.compareAndSet(false, true)) {
            consensusValue.set(value)
            future.complete(value)
            return true
        }
        return false
    }

    /**
     * 尝试重置共识状态
     * @return 如果成功重置共识状态，返回 true；否则返回 false
     */
    fun tryResetConsensus(): Boolean {
        if (consensusReached.compareAndSet(true, false)) {
            consensusValue.set(null)
            return true
        }
        return false
    }

    /**
     * 获取共识值
     * @return 如果达成共识，返回共识值和 true；否则返回 null 和 false
     */
    fun getConsensus(): Pair<Any?, Boolean> {
        return consensusValue.get() to consensusReached.get()
    }

    /**
     * 获取共识 Future
     * @return 共识 Future
     */
    fun getFuture(): CompletableFuture<Any?> {
        return future
    }
}

/**
 * DefaultConsensus 类是 Consensus 接口的默认实现
 */
class DefaultConsensus(private val id: String) : Consensus {
    private val result = ConsensusResult()

    override fun getId(): String = id

    override fun tryGetConsensus(): Pair<Any?, Boolean> = result.getConsensus()

    override fun trySetConsensus(value: Any?): Boolean = result.trySetConsensus(value)

    override fun tryResetConsensus(): Boolean = result.tryResetConsensus()

    override suspend fun waitForConsensus(timeoutMs: Long): Any? {
        val (value, reached) = tryGetConsensus()
        if (reached) {
            return value
        }

        return try {
            withTimeout(timeoutMs) {
                result.getFuture().await()
            }
        } catch (e: TimeoutCancellationException) {
            null
        }
    }
}

/**
 * ConsensusRegistry 类用于管理多个共识处理器
 */
class ConsensusRegistry {
    private val consensusHandlers = ConcurrentHashMap<String, Consensus>()

    /**
     * 注册共识处理器
     * @param key 共识处理器的键
     * @return 共识处理器
     */
    fun register(key: String): Consensus {
        return consensusHandlers.computeIfAbsent(key) { DefaultConsensus(it) }
    }

    /**
     * 获取共识处理器
     * @param key 共识处理器的键
     * @return 共识处理器，如果不存在则返回 null
     */
    fun get(key: String): Consensus? {
        return consensusHandlers[key]
    }

    /**
     * 移除共识处理器
     * @param key 共识处理器的键
     * @return 如果成功移除共识处理器，返回 true；否则返回 false
     */
    fun remove(key: String): Boolean {
        return consensusHandlers.remove(key) != null
    }

    /**
     * 清除所有共识处理器
     */
    fun clear() {
        consensusHandlers.clear()
    }
}
