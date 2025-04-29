package actor.proto.cluster

import actor.proto.EventStream
import mu.KotlinLogging
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = KotlinLogging.logger {}

/**
 * MemberList is responsible for keeping track of the current cluster topology.
 * It listens to changes from the ClusterProvider and updates the member list accordingly.
 */
class MemberList(private val cluster: Cluster) {
    private val lock = ReentrantReadWriteLock()
    private val members = ConcurrentHashMap<String, Member>()
    private val memberStrategyByKind = ConcurrentHashMap<String, MemberStrategy>()
    private val eventStream = cluster.actorSystem.eventStream()

    init {
        // Subscribe to topology events
        eventStream.subscribe({ event ->
            when (event) {
                is ClusterTopology -> {
                    // Handle topology changes
                    handleTopologyChanges(event)
                }
                is GossipUpdate -> {
                    if (event.key == "topology") {
                        // Handle gossip update for topology
                        handleGossipTopologyUpdate(event)
                    }
                }
            }
        })
    }

    /**
     * Get all members in the cluster.
     * @return A list of all members.
     */
    fun getMembers(): List<Member> {
        return lock.read { members.values.toList() }
    }

    /**
     * Get a member by ID.
     * @param id The ID of the member.
     * @return The member, or null if not found.
     */
    fun getMember(id: String): Member? {
        return lock.read { members[id] }
    }

    /**
     * Get the partition member for a given identity and kind.
     * @param identity The identity of the actor.
     * @param kind The kind of the actor.
     * @return The ID of the member that should host the actor, or null if no members are available.
     */
    fun getPartitionMember(identity: String, kind: String): String? {
        return lock.read {
            val strategy = memberStrategyByKind[kind] ?: createMemberStrategyForKind(kind)
            strategy.getPartition(identity)
        }
    }

    /**
     * Get the partition member for a given cluster identity.
     * @param clusterIdentity The cluster identity of the actor.
     * @return The ID of the member that should host the actor, or null if no members are available.
     */
    fun getPartitionMember(clusterIdentity: ClusterIdentity): String? {
        return getPartitionMember(clusterIdentity.identity, clusterIdentity.kind)
    }

    /**
     * Update the member list with a new set of members.
     * @param newMembers The new set of members.
     */
    fun updateMembers(newMembers: List<Member>) {
        lock.write {
            val oldMembers = members.values.toSet()
            val newMembersSet = newMembers.toSet()

            // Find joined and left members
            val joined = newMembersSet - oldMembers
            val left = oldMembers - newMembersSet

            // Update members map
            members.clear()
            newMembers.forEach { members[it.id] = it }

            // Update member strategies
            updateMemberStrategies()

            // Publish topology event
            if (joined.isNotEmpty() || left.isNotEmpty()) {
                val topology = ClusterTopology(
                    members = members.keys.toList(),
                    joined = joined.map { it.id },
                    left = left.map { it.id }
                )
                eventStream.publish(topology)
            }
        }
    }

    /**
     * Add a member to the member list.
     * @param member The member to add.
     */
    fun addMember(member: Member) {
        lock.write {
            val oldMember = members[member.id]
            members[member.id] = member

            // Update member strategies
            updateMemberStrategies()

            // Publish topology event if this is a new member
            if (oldMember == null) {
                val topology = ClusterTopology(
                    members = members.keys.toList(),
                    joined = listOf(member.id),
                    left = emptyList()
                )
                eventStream.publish(topology)
            }
        }
    }

    /**
     * Remove a member from the member list.
     * @param id The ID of the member to remove.
     */
    fun removeMember(id: String) {
        lock.write {
            val oldMember = members.remove(id)

            // Update member strategies
            updateMemberStrategies()

            // Publish topology event if a member was removed
            if (oldMember != null) {
                val topology = ClusterTopology(
                    members = members.keys.toList(),
                    joined = emptyList(),
                    left = listOf(id)
                )
                eventStream.publish(topology)
            }
        }
    }

    /**
     * Update the status of a member.
     * @param id The ID of the member.
     * @param status The new status of the member.
     */
    fun updateMemberStatus(id: String, status: MemberStatus) {
        lock.write {
            val member = members[id] ?: return@write
            members[id] = member.copy(status = status)

            // Update member strategies
            updateMemberStrategies()
        }
    }

    /**
     * Handle topology changes from the cluster provider.
     * @param topology The new topology.
     */
    private fun handleTopologyChanges(topology: ClusterTopology) {
        lock.write {
            // Handle blocked members
            topology.blocked.forEach { id ->
                // Add to block list
                // TODO: Implement block list functionality
                // cluster.remote.blockList.block(id)
            }

            // Handle left members
            topology.left.forEach { id ->
                members.remove(id)
            }

            // Update member strategies
            updateMemberStrategies()
        }
    }

    /**
     * Handle gossip update for topology.
     * @param update The gossip update.
     */
    private fun handleGossipTopologyUpdate(update: GossipUpdate) {
        // TODO: Implement gossip topology update handling
    }

    /**
     * Update all member strategies.
     */
    private fun updateMemberStrategies() {
        // Clear all strategies
        memberStrategyByKind.clear()

        // Create new strategies for each kind
        cluster.getClusterKinds().keys.forEach { kind ->
            createMemberStrategyForKind(kind)
        }
    }

    /**
     * Create a member strategy for a kind.
     * @param kind The kind to create a strategy for.
     * @return The created strategy.
     */
    private fun createMemberStrategyForKind(kind: String): MemberStrategy {
        val strategy = cluster.config.memberStrategyBuilder(cluster, kind)

        // Add all alive members to the strategy
        members.values
            .filter { it.status == MemberStatus.ALIVE }
            .forEach { strategy.addMember(it.id) }

        memberStrategyByKind[kind] = strategy
        return strategy
    }
}
