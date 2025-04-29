package actor.proto.cluster.consensus

import actor.proto.cluster.Member
import actor.proto.cluster.MemberStatus

/**
 * ConsensusCheckBuilder 类用于构建共识检查
 */
class ConsensusCheckBuilder {
    private val checks = mutableListOf<ConsensusCheck>()

    /**
     * 获取所有共识检查
     * @return 共识检查列表
     */
    fun buildChecks(): List<ConsensusCheck> {
        return checks.toList()
    }

    /**
     * 添加成员状态共识检查
     * @param status 要检查的成员状态
     * @param minCount 最小成员数量
     * @param percentage 达成共识的百分比
     * @return 构建器实例
     */
    fun withMemberStatus(status: MemberStatus, minCount: Int = 1, percentage: Double = 0.5): ConsensusCheckBuilder {
        checks.add(MemberStatusConsensusCheck(status, minCount, percentage))
        return this
    }

    /**
     * 添加成员数量共识检查
     * @param minCount 最小成员数量
     * @param maxCount 最大成员数量
     * @return 构建器实例
     */
    fun withMemberCount(minCount: Int = 1, maxCount: Int = Int.MAX_VALUE): ConsensusCheckBuilder {
        checks.add(MemberCountConsensusCheck(minCount, maxCount))
        return this
    }

    /**
     * 添加成员角色共识检查
     * @param role 要检查的成员角色
     * @param minCount 最小成员数量
     * @param percentage 达成共识的百分比
     * @return 构建器实例
     */
    fun withMemberRole(role: String, minCount: Int = 1, percentage: Double = 0.5): ConsensusCheckBuilder {
        checks.add(MemberRoleConsensusCheck(role, minCount, percentage))
        return this
    }

    /**
     * 添加自定义共识检查
     * @param checkFn 自定义检查函数
     * @return 构建器实例
     */
    fun withCustomCheck(checkFn: (List<Member>) -> Any?): ConsensusCheckBuilder {
        checks.add(CustomConsensusCheck(checkFn))
        return this
    }

    /**
     * 添加共识检查
     * @param check 要添加的共识检查
     * @return 构建器实例
     */
    fun withCheck(check: ConsensusCheck): ConsensusCheckBuilder {
        checks.add(check)
        return this
    }

    /**
     * 构建所有共识检查
     * @return 所有共识检查
     */
    fun buildAll(): ConsensusCheck {
        return AllConsensusCheck(checks.toList())
    }

    /**
     * 构建任意共识检查
     * @return 任意共识检查
     */
    fun buildAny(): ConsensusCheck {
        return AnyConsensusCheck(checks.toList())
    }

    companion object {
        /**
         * 创建构建器实例
         * @return 构建器实例
         */
        fun create(): ConsensusCheckBuilder {
            return ConsensusCheckBuilder()
        }
    }
}
