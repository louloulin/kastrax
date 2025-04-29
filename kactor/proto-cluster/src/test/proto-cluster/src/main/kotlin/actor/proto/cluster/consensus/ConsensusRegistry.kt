package actor.proto.cluster.consensus

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference

/**
 * ConsensusRegistry 类用于管理共识处理器
 */
class ConsensusRegistry {
    private val consensusMap = ConcurrentHashMap<String, DefaultConsensus>()
    
    /**
     * 注册共识处理器
     * @param key 共识处理器的键
     * @return 共识处理器
     */
    fun register(key: String): Consensus {
        return consensusMap.computeIfAbsent(key) { DefaultConsensus() }
    }
    
    /**
     * 获取共识处理器
     * @param key 共识处理器的键
     * @return 共识处理器，如果不存在则返回 null
     */
    fun get(key: String): Consensus? {
        return consensusMap[key]
    }
    
    /**
     * 移除共识处理器
     * @param key 共识处理器的键
     * @return 如果成功移除共识处理器，返回 true；否则返回 false
     */
    fun remove(key: String): Boolean {
        return consensusMap.remove(key) != null
    }
    
    /**
     * 清除所有共识处理器
     */
    fun clear() {
        consensusMap.clear()
    }
}

/**
 * DefaultConsensus 类是 Consensus 接口的默认实现
 */
class DefaultConsensus : Consensus {
    private val consensusValue = AtomicReference<Any?>(null)
    
    override fun trySetConsensus(value: Any?): Boolean {
        return consensusValue.compareAndSet(null, value)
    }
    
    override fun tryGetConsensus(): Pair<Any?, Boolean> {
        val value = consensusValue.get()
        return Pair(value, value != null)
    }
    
    override fun tryResetConsensus(): Boolean {
        return consensusValue.compareAndSet(consensusValue.get(), null)
    }
    
    override suspend fun waitForConsensus(timeoutMs: Long): Any? {
        val startTime = System.currentTimeMillis()
        while (System.currentTimeMillis() - startTime < timeoutMs) {
            val (value, reached) = tryGetConsensus()
            if (reached) {
                return value
            }
            kotlinx.coroutines.delay(100)
        }
        return null
    }
}
