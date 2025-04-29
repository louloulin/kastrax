package actor.proto.cluster.consensus

import actor.proto.cluster.Member
import actor.proto.cluster.MemberStatus
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * ConsensusCheck 接口定义了共识检查的基本操作
 */
interface ConsensusCheck {
    /**
     * 检查是否达成共识
     * @param members 集群成员列表
     * @return 如果达成共识，返回共识值；否则返回 null
     */
    fun check(members: List<Member>): Any?
}

/**
 * ConsensusCheckResult 类用于存储共识检查结果
 * @param value 共识值
 * @param reached 是否达成共识
 */
data class ConsensusCheckResult(val value: Any?, val reached: Boolean)

/**
 * MemberStatusConsensusCheck 类用于检查成员状态是否达成共识
 * @param status 要检查的成员状态
 * @param minCount 最小成员数量
 * @param percentage 达成共识的百分比
 */
class MemberStatusConsensusCheck(
    private val status: MemberStatus,
    private val minCount: Int = 1,
    private val percentage: Double = 0.5
) : ConsensusCheck {
    override fun check(members: List<Member>): Any? {
        if (members.isEmpty()) {
            return null
        }
        
        val matchingMembers = members.filter { it.status == status }
        val matchingCount = matchingMembers.size
        
        if (matchingCount < minCount) {
            return null
        }
        
        val requiredCount = (members.size * percentage).toInt()
        if (matchingCount < requiredCount) {
            return null
        }
        
        logger.debug { "MemberStatusConsensusCheck: $matchingCount of ${members.size} members have status $status, required $requiredCount" }
        return matchingMembers
    }
}

/**
 * MemberCountConsensusCheck 类用于检查成员数量是否达成共识
 * @param minCount 最小成员数量
 * @param maxCount 最大成员数量
 */
class MemberCountConsensusCheck(
    private val minCount: Int = 1,
    private val maxCount: Int = Int.MAX_VALUE
) : ConsensusCheck {
    override fun check(members: List<Member>): Any? {
        val count = members.size
        
        if (count < minCount || count > maxCount) {
            return null
        }
        
        logger.debug { "MemberCountConsensusCheck: $count members, min: $minCount, max: $maxCount" }
        return count
    }
}

/**
 * MemberRoleConsensusCheck 类用于检查特定角色的成员是否达成共识
 * @param role 要检查的成员角色
 * @param minCount 最小成员数量
 * @param percentage 达成共识的百分比
 */
class MemberRoleConsensusCheck(
    private val role: String,
    private val minCount: Int = 1,
    private val percentage: Double = 0.5
) : ConsensusCheck {
    override fun check(members: List<Member>): Any? {
        if (members.isEmpty()) {
            return null
        }
        
        val matchingMembers = members.filter { it.labels.containsKey("role") && it.labels["role"] == role }
        val matchingCount = matchingMembers.size
        
        if (matchingCount < minCount) {
            return null
        }
        
        val requiredCount = (members.size * percentage).toInt()
        if (matchingCount < requiredCount) {
            return null
        }
        
        logger.debug { "MemberRoleConsensusCheck: $matchingCount of ${members.size} members have role $role, required $requiredCount" }
        return matchingMembers
    }
}

/**
 * AllConsensusCheck 类用于检查所有共识检查是否都达成共识
 * @param checks 要检查的共识检查列表
 */
class AllConsensusCheck(private val checks: List<ConsensusCheck>) : ConsensusCheck {
    override fun check(members: List<Member>): Any? {
        val results = mutableListOf<Any?>()
        
        for (check in checks) {
            val result = check.check(members)
            if (result == null) {
                return null
            }
            results.add(result)
        }
        
        logger.debug { "AllConsensusCheck: all ${checks.size} checks passed" }
        return results
    }
}

/**
 * AnyConsensusCheck 类用于检查任意一个共识检查是否达成共识
 * @param checks 要检查的共识检查列表
 */
class AnyConsensusCheck(private val checks: List<ConsensusCheck>) : ConsensusCheck {
    override fun check(members: List<Member>): Any? {
        for (check in checks) {
            val result = check.check(members)
            if (result != null) {
                logger.debug { "AnyConsensusCheck: one of ${checks.size} checks passed" }
                return result
            }
        }
        
        return null
    }
}

/**
 * CustomConsensusCheck 类用于自定义共识检查
 * @param checkFn 自定义检查函数
 */
class CustomConsensusCheck(private val checkFn: (List<Member>) -> Any?) : ConsensusCheck {
    override fun check(members: List<Member>): Any? {
        return checkFn(members)
    }
}
