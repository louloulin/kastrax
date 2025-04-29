package actor.proto.cluster.consensus

import actor.proto.cluster.MemberGossipState
import actor.proto.cluster.GossipMemberState
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap

private val logger = KotlinLogging.logger {}

/**
 * ConsensusCheckImpl 类用于检查集群中的共识
 */
open class ConsensusCheckImpl(
    val affectedKeys: Set<String>,
    val check: (MemberGossipState, Set<String>) -> Unit
)

/**
 * ConsensusChecker 是一个函数类型，用于检查共识
 */
typealias ConsensusChecker = (MemberGossipState, Set<String>) -> Pair<Boolean, Any?>

/**
 * ConsensusValueExtractor 是一个函数类型，用于从消息中提取共识值
 */
typealias ConsensusValueExtractor = (Any) -> Any?

/**
 * ConsensusMemberValue 类表示成员的共识值
 */
data class ConsensusMemberValue(
    val memberId: String,
    val key: String,
    val value: Any
)

/**
 * ConsensusCheckBuilder 类用于构建共识检查
 */
class ConsensusCheckBuilder(
    private val key: String,
    private val valueExtractor: ConsensusValueExtractor
) {
    private val consensusValues = mutableListOf(ConsensusValue(key, valueExtractor))
    private lateinit var checker: ConsensusChecker

    init {
        checker = buildChecker()
    }

    /**
     * 构建共识检查列表
     * @return 共识检查列表
     */
    fun buildChecks(): List<ConsensusCheck> {
        // 创建一个适配器，将 ConsensusCheckImpl 转换为 ConsensusCheck
        val checkAdapter = object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                // 实现检查逻辑
                return members.takeIf { it.isNotEmpty() }
            }
        }
        return listOf(checkAdapter)
    }

    /**
     * 构建共识检查
     * @return 共识检查器和共识检查
     */
    fun build(): Pair<Consensus, ConsensusCheck> {
        val consensus = DefaultConsensus(key)

        // 创建一个适配器，将内部检查逻辑转换为 ConsensusCheck 接口
        val consensusCheck = object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                // 实现检查逻辑
                return if (members.isNotEmpty()) {
                    // 这里只是一个简单的实现，实际中应该使用更复杂的逻辑
                    members
                } else {
                    null
                }
            }
        }

        return consensus to consensusCheck
    }

    /**
     * 获取受影响的键
     * @return 受影响的键集合
     */
    fun affectedKeys(): Set<String> {
        return consensusValues.flatMap { it.key.split('.') }.toSet()
    }

    /**
     * 检查共识
     * @return 共识检查器
     */
    fun check(): ConsensusChecker = checker

    /**
     * 构建共识检查器
     * @return 共识检查器
     */
    private fun buildChecker(): ConsensusChecker {
        return { state, members ->
            val memberStates = getValidMemberStates(state, members)

            if (memberStates.size < members.size) {
                // 不是所有成员都有状态
                Pair(false, null)
            } else {

            val valueTuples = mutableListOf<ConsensusMemberValue>()
            for (consensusValue in consensusValues) {
                val mapToValue = mapToValue(consensusValue)
                for ((memberId, memberState) in memberStates) {
                    val (member, key, value) = mapToValue(memberId, memberState)
                    valueTuples.add(ConsensusMemberValue(member, key, value))
                }
            }

                val (hasConsensus, consensusValue) = hasConsensus(valueTuples)
                logConsensusStatus(hasConsensus, consensusValue, valueTuples)

                Pair(hasConsensus, consensusValue)
            }
        }
    }

    /**
     * 获取有效的成员状态
     * @param state 八卦状态
     * @param members 成员集合
     * @return 有效的成员状态映射
     */
    private fun getValidMemberStates(state: MemberGossipState, members: Set<String>): Map<String, GossipMemberState> {
        val result = mutableMapOf<String, GossipMemberState>()
        for ((member, memberState) in state.members) {
            if (member in members) {
                result[member] = memberState
            }
        }
        return result
    }

    /**
     * 映射到值
     * @param consensusValue 共识值
     * @return 映射函数
     */
    private fun mapToValue(consensusValue: ConsensusValue): (String, GossipMemberState) -> Triple<String, String, Any> {
        return { memberId, memberState ->
            val value = memberState.values[consensusValue.key]
            val extractedValue = value?.let { consensusValue.valueExtractor(it) } ?: 0
            Triple(memberId, consensusValue.key, extractedValue)
        }
    }

    /**
     * 检查是否达成共识
     * @param memberValues 成员值列表
     * @return 是否达成共识和共识值
     */
    private fun hasConsensus(memberValues: List<ConsensusMemberValue>): Pair<Boolean, Any?> {
        if (memberValues.isEmpty()) {
            return false to null
        }

        val first = memberValues[0]
        for (i in 1 until memberValues.size) {
            val next = memberValues[i]
            if (first.value != next.value) {
                return false to null
            }
        }

        return true to first.value
    }

    /**
     * 记录共识状态
     * @param hasConsensus 是否达成共识
     * @param consensusValue 共识值
     * @param memberValues 成员值列表
     */
    private fun logConsensusStatus(hasConsensus: Boolean, consensusValue: Any?, memberValues: List<ConsensusMemberValue>) {
        if (logger.isDebugEnabled) {
            val status = if (hasConsensus) "达成共识" else "未达成共识"
            val values = memberValues.joinToString(", ") { "${it.memberId}:${it.value}" }
            logger.debug { "$status: 值=$consensusValue, 成员值=[$values]" }
        }
    }

    /**
     * 共识值类
     */
    data class ConsensusValue(
        val key: String,
        val valueExtractor: ConsensusValueExtractor
    )
}

/**
 * ConsensusChecks 类用于管理多个共识检查
 */
class ConsensusChecks {
    private val checks = ConcurrentHashMap<String, ConsensusCheck>()
    private val affectedKeysByStateKey = ConcurrentHashMap<String, MutableSet<String>>()

    /**
     * 添加共识检查
     * @param key 共识检查的键
     * @param check 共识检查
     */
    fun add(key: String, check: ConsensusCheck) {
        checks[key] = check
        // 对于 ConsensusCheck 接口，我们使用键作为受影响的键
        affectedKeysByStateKey.computeIfAbsent(key) { mutableSetOf() }.add(key)
    }

    /**
     * 获取共识检查
     * @param key 共识检查的键
     * @return 共识检查，如果不存在则返回 null
     */
    fun get(key: String): ConsensusCheck? {
        return checks[key]
    }

    /**
     * 获取受影响的键
     * @param stateKey 状态键
     * @return 受影响的键集合
     */
    fun getAffectedChecks(stateKey: String): Set<String> {
        return affectedKeysByStateKey[stateKey] ?: emptySet()
    }

    /**
     * 移除共识检查
     * @param key 共识检查的键
     * @return 如果成功移除共识检查，返回 true；否则返回 false
     */
    fun remove(key: String): Boolean {
        checks.remove(key) ?: return false
        // 对于 ConsensusCheck 接口，我们只需要移除键对应的条目
        affectedKeysByStateKey[key]?.remove(key)
        if (affectedKeysByStateKey[key]?.isEmpty() == true) {
            affectedKeysByStateKey.remove(key)
        }
        return true
    }

    /**
     * 清除所有共识检查
     */
    fun clear() {
        checks.clear()
        affectedKeysByStateKey.clear()
    }
}
