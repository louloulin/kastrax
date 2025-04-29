package actor.proto.cluster.consensus

import actor.proto.cluster.Cluster
import actor.proto.cluster.Member
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean

private val logger = KotlinLogging.logger {}

/**
 * ConsensusManager 类用于管理共识检查和处理共识结果
 * @param cluster 集群实例
 */
class ConsensusManager(private val cluster: Cluster) {
    private val consensusRegistry = ConsensusRegistry()
    private val consensusChecks = ConcurrentHashMap<String, ConsensusCheck>()
    private val isRunning = AtomicBoolean(false)
    private val scope = CoroutineScope(Dispatchers.Default)
    private var job: Job? = null
    private val checkIntervalMs = 1000L

    /**
     * 启动共识管理器
     */
    fun start() {
        if (isRunning.compareAndSet(false, true)) {
            job = scope.launch {
                while (isActive && isRunning.get()) {
                    try {
                        checkConsensus()
                    } catch (e: Exception) {
                        logger.error(e) { "Error checking consensus" }
                    }
                    delay(checkIntervalMs)
                }
            }
            logger.info { "ConsensusManager started" }
        }
    }

    /**
     * 停止共识管理器
     */
    fun stop() {
        if (isRunning.compareAndSet(true, false)) {
            job?.cancel()
            job = null
            logger.info { "ConsensusManager stopped" }
        }
    }

    /**
     * 注册共识检查
     * @param key 共识检查的键
     * @param check 共识检查
     * @return 共识处理器
     */
    fun registerCheck(key: String, check: ConsensusCheck): Consensus {
        consensusChecks[key] = check
        return consensusRegistry.register(key)
    }

    /**
     * 获取共识处理器
     * @param key 共识处理器的键
     * @return 共识处理器，如果不存在则返回 null
     */
    fun getConsensus(key: String): Consensus? {
        return consensusRegistry.get(key)
    }

    /**
     * 移除共识检查
     * @param key 共识检查的键
     * @return 如果成功移除共识检查，返回 true；否则返回 false
     */
    fun removeCheck(key: String): Boolean {
        consensusChecks.remove(key)
        return consensusRegistry.remove(key)
    }

    /**
     * 清除所有共识检查
     */
    fun clearChecks() {
        consensusChecks.clear()
        consensusRegistry.clear()
    }

    /**
     * 检查共识
     */
    fun checkConsensus() {
        val members = cluster.memberList.getMembers()

        for ((key, check) in consensusChecks) {
            val consensus = consensusRegistry.get(key) ?: continue
            val result = check.check(members)

            if (result != null) {
                // 达成共识
                val (_, reached) = consensus.tryGetConsensus()
                if (!reached) {
                    // 设置共识值
                    consensus.trySetConsensus(result)
                    logger.info { "Consensus reached for $key: $result" }
                }
            } else {
                // 未达成共识
                val (_, reached) = consensus.tryGetConsensus()
                if (reached) {
                    // 重置共识状态
                    consensus.tryResetConsensus()
                    logger.info { "Consensus lost for $key" }
                }
            }
        }
    }
}
