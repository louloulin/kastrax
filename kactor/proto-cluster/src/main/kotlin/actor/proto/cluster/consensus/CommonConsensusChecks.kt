package actor.proto.cluster.consensus

import actor.proto.cluster.ClusterTopology
import actor.proto.cluster.Gossiper
import actor.proto.cluster.MemberList
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * CommonConsensusChecks 类提供了一些常用的共识检查
 */
object CommonConsensusChecks {
    /**
     * 创建拓扑共识检查
     * @param gossiper 八卦器
     * @param memberList 成员列表
     * @return 共识处理器
     */
    fun createTopologyConsensusCheck(gossiper: Gossiper, memberList: MemberList): Consensus {
        val builder = ConsensusCheckBuilder("topology") { value ->
            when (value) {
                is ClusterTopology -> value.topologyHash
                else -> null
            }
        }

        val (consensus, _) = builder.build()
        // Create a simple check that always returns true for non-empty member lists
        val check = object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                return members.takeIf { it.isNotEmpty() }
            }
        }
        gossiper.registerConsensusCheck("topology", check)

        return consensus
    }

    /**
     * 创建成员状态共识检查
     * @param gossiper 八卦器
     * @return 共识处理器
     */
    fun createMemberStatusConsensusCheck(gossiper: Gossiper): Consensus {
        val builder = ConsensusCheckBuilder("member.status") { value ->
            when (value) {
                is Map<*, *> -> value.hashCode()
                else -> null
            }
        }

        val (consensus, _) = builder.build()
        // Create a simple check that always returns true for non-empty member lists
        val check = object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                return members.takeIf { it.isNotEmpty() }
            }
        }
        gossiper.registerConsensusCheck("member.status", check)

        return consensus
    }

    /**
     * 创建集群配置共识检查
     * @param gossiper 八卦器
     * @return 共识处理器
     */
    fun createClusterConfigConsensusCheck(gossiper: Gossiper): Consensus {
        val builder = ConsensusCheckBuilder("cluster.config") { value ->
            when (value) {
                is Map<*, *> -> value.hashCode()
                else -> null
            }
        }

        val (consensus, _) = builder.build()
        // Create a simple check that always returns true for non-empty member lists
        val check = object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                return members.takeIf { it.isNotEmpty() }
            }
        }
        gossiper.registerConsensusCheck("cluster.config", check)

        return consensus
    }

    /**
     * 创建自定义共识检查
     * @param gossiper 八卦器
     * @param key 共识检查的键
     * @param valueExtractor 值提取器
     * @return 共识处理器
     */
    fun createCustomConsensusCheck(gossiper: Gossiper, key: String, valueExtractor: ConsensusValueExtractor): Consensus {
        val builder = ConsensusCheckBuilder(key, valueExtractor)
        val (consensus, _) = builder.build()
        // Create a simple check that always returns true for non-empty member lists
        val check = object : ConsensusCheck {
            override fun check(members: List<actor.proto.cluster.Member>): Any? {
                return members.takeIf { it.isNotEmpty() }
            }
        }
        gossiper.registerConsensusCheck(key, check)

        return consensus
    }
}
