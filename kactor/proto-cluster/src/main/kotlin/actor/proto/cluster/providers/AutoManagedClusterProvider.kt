package actor.proto.cluster.providers

import actor.proto.Actor
import actor.proto.Context
import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterProvider
import actor.proto.cluster.Member
import actor.proto.cluster.MemberStatus
import actor.proto.fromProducer
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import mu.KotlinLogging
import java.net.InetAddress
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong

private val logger = KotlinLogging.logger {}

/**
 * AutoManagedClusterProvider is a cluster provider that automatically manages cluster membership.
 * It uses a simple gossip protocol to discover and monitor other nodes.
 */
class AutoManagedClusterProvider(
    private val port: Int,
    private val seedNodes: List<String> = emptyList()
) : ClusterProvider {
    private lateinit var cluster: Cluster
    private val members = ConcurrentHashMap<String, Member>()
    private val heartbeats = ConcurrentHashMap<String, AtomicLong>()
    private lateinit var pid: PID
    private var shutdown = false

    override suspend fun startMember(cluster: Cluster): Boolean {
        this.cluster = cluster

        // Create our own member
        val host = InetAddress.getLocalHost().hostName
        val id = "${host}:${port}"
        val member = Member(
            id = id,
            host = host,
            port = port,
            status = MemberStatus.ALIVE
        )

        // Add our member to the list
        members[id] = member
        heartbeats[id] = AtomicLong(0)

        // Start the member actor
        val props = fromProducer { MemberActor(this) }
        pid = cluster.actorSystem.actorOf(props)

        // Update the member list
        cluster.memberList.updateMembers(members.values.toList())

        // Connect to seed nodes
        for (seedNode in seedNodes) {
            if (seedNode != id) {
                connectToMember(seedNode)
            }
        }

        return true
    }

    override suspend fun startClient(cluster: Cluster): Boolean {
        this.cluster = cluster

        // Start the member actor
        val props = fromProducer { MemberActor(this) }
        pid = cluster.actorSystem.actorOf(props)

        // Connect to seed nodes
        for (seedNode in seedNodes) {
            connectToMember(seedNode)
        }

        return true
    }

    override suspend fun shutdown(graceful: Boolean): Boolean {
        shutdown = true

        if (graceful) {
            // Update our status to LEAVING
            val id = cluster.actorSystem.address
            val member = members[id]
            if (member != null) {
                members[id] = member.copy(status = MemberStatus.LEAVING)

                // Update the member list
                cluster.memberList.updateMembers(members.values.toList())

                // Wait for the update to propagate
                delay(1000)
            }
        }

        return true
    }

    /**
     * Connect to a member.
     * @param memberId The ID of the member to connect to.
     */
    private fun connectToMember(memberId: String) {
        // TODO: Implement remote connection
        // For now, just add the member to the list
        if (!members.containsKey(memberId)) {
            val parts = memberId.split(":")
            val host = parts[0]
            val port = parts[1].toInt()

            val member = Member(
                id = memberId,
                host = host,
                port = port,
                status = MemberStatus.ALIVE
            )

            members[memberId] = member
            heartbeats[memberId] = AtomicLong(0)

            // Update the member list
            cluster.memberList.updateMembers(members.values.toList())
        }
    }

    /**
     * Update the heartbeat for a member.
     * @param memberId The ID of the member.
     */
    fun updateHeartbeat(memberId: String) {
        heartbeats[memberId]?.incrementAndGet()
    }

    /**
     * Check for dead members.
     */
    fun checkDeadMembers() {
        val now = System.currentTimeMillis()
        val deadMembers = mutableListOf<String>()

        for ((memberId, member) in members) {
            if (member.status == MemberStatus.ALIVE) {
                val lastHeartbeat = heartbeats[memberId]?.get() ?: 0
                val elapsed = now - lastHeartbeat

                if (elapsed > cluster.config.heartbeatExpiration.toMillis()) {
                    deadMembers.add(memberId)
                }
            }
        }

        // Remove dead members
        for (memberId in deadMembers) {
            members.remove(memberId)
            heartbeats.remove(memberId)
        }

        // Update the member list
        if (deadMembers.isNotEmpty()) {
            cluster.memberList.updateMembers(members.values.toList())
        }
    }

    /**
     * Get all members.
     * @return A list of all members.
     */
    fun getMembers(): List<Member> {
        return members.values.toList()
    }
}

/**
 * MemberActor handles member-related messages.
 */
class MemberActor(private val provider: AutoManagedClusterProvider) : Actor {
    override suspend fun Context.receive(msg: Any) {
        when (msg) {
            is actor.proto.Started -> {
                // Start the heartbeat loop
                kotlinx.coroutines.GlobalScope.launch {
                    while (true) {
                        try {
                            // Update our heartbeat
                            provider.updateHeartbeat(self.id)

                            // Check for dead members
                            provider.checkDeadMembers()

                            // TODO: Send heartbeats to other members

                        } catch (e: Exception) {
                            logger.error("Error in heartbeat loop", e)
                        }

                        delay(1000)
                    }
                }
            }
            // TODO: Handle other member-related messages
        }
    }
}
