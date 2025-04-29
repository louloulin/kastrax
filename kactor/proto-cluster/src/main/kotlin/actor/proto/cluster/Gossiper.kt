package actor.proto.cluster

import actor.proto.Actor
import actor.proto.Context
import actor.proto.PID
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.cluster.consensus.Consensus
import actor.proto.cluster.consensus.ConsensusCheck
import actor.proto.cluster.consensus.ConsensusCheckImpl
import actor.proto.cluster.consensus.ConsensusChecks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

/**
 * Gossiper is responsible for gossiping state between cluster members.
 */
class Gossiper(val cluster: Cluster) {
    val state = ConcurrentHashMap<String, GossipState>()
    val versions = ConcurrentHashMap<String, AtomicLong>()
    val consensusChecks = ConcurrentHashMap<String, ConsensusCheck>()
    val consensusRegistry = ConcurrentHashMap<String, Consensus>()
    private lateinit var pid: PID

    init {
        // Start the gossip actor
        val props = fromProducer { GossipActor(this) }
        pid = cluster.actorSystem.actorOf(props)

        // Start the gossip loop
        cluster.actorSystem.actorOf(fromProducer { GossipLoopActor(this) })
    }

    /**
     * Register a consensus check for a key.
     * @param key The key to check consensus for.
     * @param check The check function.
     * @return The consensus check.
     */
    fun registerConsensusCheck(key: String, check: ConsensusCheck): ConsensusCheck {
        consensusChecks[key] = check
        return check
    }

    /**
     * Update the gossip state for a key.
     * @param key The key to update.
     * @param value The new value.
     */
    fun updateState(key: String, value: Any) {
        val version = versions.computeIfAbsent(key) { AtomicLong(0) }.incrementAndGet()
        val gossipState = GossipState(key, value, version)
        state[key] = gossipState

        // Publish the update
        val eventStream = cluster.actorSystem.eventStream()
        eventStream.publish(GossipUpdate(key, value, version))
    }

    /**
     * Get the gossip state for a key.
     * @param key The key to get.
     * @return The gossip state, or null if not found.
     */
    fun getState(key: String): GossipState? {
        return state[key]
    }

    /**
     * Get all gossip states.
     * @return A map of keys to gossip states.
     */
    fun getAllStates(): Map<String, GossipState> {
        return state.toMap()
    }

    /**
     * Handle a gossip request.
     * @param request The gossip request.
     * @return The gossip response.
     */
    fun handleGossipRequest(request: GossipRequest): GossipResponse {
        val response = mutableMapOf<String, GossipState>()

        // Check if we have newer versions of any keys
        for ((key, theirState) in request.states) {
            val ourState = state[key]
            if (ourState != null && ourState.version > theirState.version) {
                response[key] = ourState
            }
        }

        // Check if we have keys they don't have
        for ((key, ourState) in state) {
            if (!request.states.containsKey(key)) {
                response[key] = ourState
            }
        }

        return GossipResponse(response)
    }

    /**
     * Handle a gossip response.
     * @param response The gossip response.
     */
    fun handleGossipResponse(response: GossipResponse) {
        for ((key, theirState) in response.states) {
            val ourState = state[key]
            if (ourState == null || theirState.version > ourState.version) {
                state[key] = theirState
                versions.computeIfAbsent(key) { AtomicLong(0) }.set(theirState.version)

                // Publish the update
                val eventStream = cluster.actorSystem.eventStream()
                eventStream.publish(GossipUpdate(key, theirState.value, theirState.version))

                // Check for consensus
                // Since we're now using a simple map, we'll check all consensus checks
                for ((_, check) in consensusChecks) {
                    val members = cluster.memberList.getMembers()
                    check.check(members)
                }
            }
        }
    }

    /**
     * Gossip with a random subset of members.
     */
    suspend fun gossip() {
        val members = cluster.memberList.getMembers()
            .filter { it.status == MemberStatus.ALIVE }
            .map { it.id }
            .toMutableList()

        // Remove our own ID
        members.remove(cluster.actorSystem.address)

        if (members.isEmpty()) {
            return
        }

        // Select a random subset of members to gossip with
        val fanOut = minOf(cluster.config.gossipFanOut, members.size)
        val selectedMembers = mutableListOf<String>()

        for (i in 0 until fanOut) {
            val index = Random.nextInt(members.size)
            selectedMembers.add(members.removeAt(index))
        }

        // Gossip with selected members
        for (memberId in selectedMembers) {
            try {
                // Create a gossip request
                GossipRequest(state.toMap())

                // Send the request to the member
                // TODO: Implement remote gossip request

            } catch (e: Exception) {
                logger.error(e) { "Error gossiping with member $memberId" }
            }
        }
    }
}

/**
 * GossipState represents the state of a key in the gossip protocol.
 */
data class GossipState(
    val key: String,
    val value: Any,
    val version: Long
)

/**
 * GossipUpdate represents an update to the gossip state.
 */
data class GossipUpdate(
    val key: String,
    val value: Any,
    val version: Long
)

/**
 * GossipRequest represents a request for gossip state.
 */
data class GossipRequest(
    val states: Map<String, GossipState>
)

/**
 * GossipResponse represents a response to a gossip request.
 */
data class GossipResponse(
    val states: Map<String, GossipState>
)

/**
 * 注册共识检查
 * @param key 共识检查的键
 * @param check 共识检查
 * @return 共识处理器
 */
fun Gossiper.registerConsensusCheck(key: String, check: ConsensusCheck): Consensus {
    consensusChecks[key] = check
    return consensusRegistry.computeIfAbsent(key) { actor.proto.cluster.consensus.DefaultConsensus(it) }
}

/**
 * 移除共识检查
 * @param key 共识检查的键
 * @return 如果成功移除共识检查，返回 true；否则返回 false
 */
fun Gossiper.removeConsensusCheck(key: String): Boolean {
    consensusRegistry.remove(key)
    return consensusChecks.remove(key) != null
}

/**
 * 获取共识处理器
 * @param key 共识处理器的键
 * @return 共识处理器，如果不存在则返回 null
 */
fun Gossiper.getConsensus(key: String): Consensus? {
    return consensusRegistry[key]
}

/**
 * 获取八卦状态
 * @return 八卦状态
 */
fun Gossiper.getGossipState(): MemberGossipState {
    val memberStates = mutableMapOf<String, GossipMemberState>()

    for ((key, _) in state) {
        // 将 GossipState 转换为 GossipMemberState
        memberStates[key] = GossipMemberState(key)
    }

    return MemberGossipState(memberStates)
}

/**
 * 获取活跃成员 ID
 * @return 活跃成员 ID 集合
 */
fun Gossiper.getActiveMemberIds(): Set<String> {
    return cluster.memberList.getMembers()
        .filter { it.status == MemberStatus.ALIVE }
        .map { it.id }
        .toSet()
}

/**
 * GossipActor handles gossip messages.
 */
class GossipActor(private val gossiper: Gossiper) : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is GossipRequest -> {
                val response = gossiper.handleGossipRequest(msg)
                sender?.let { send(it, response) }
            }
            is GossipResponse -> {
                gossiper.handleGossipResponse(msg)
            }
        }
    }
}

/**
 * GossipLoopActor periodically gossips with other members.
 */
class GossipLoopActor(private val gossiper: Gossiper) : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is actor.proto.Started -> {
                // Start the gossip loop
                CoroutineScope(Dispatchers.Default).launch {
                    while (true) {
                        try {
                            gossiper.gossip()
                        } catch (e: Exception) {
                            logger.error("Error in gossip loop", e)
                        }

                        delay(1000) // Use a fixed delay for now
                    }
                }
            }
        }
    }
}
