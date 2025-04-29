package actor.proto.examples.cluster

import actor.proto.cluster.*
import java.util.*

/**
 * Example demonstrating how to use Rendezvous hash for consistent cluster routing
 */
object RendezvousExample {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Rendezvous Hash Example")
        println("======================")
        
        // Create a Rendezvous hash instance
        val rendezvous = Rendezvous.create()
        
        // Create some cluster members
        val members = createMembers(5)
        println("Created ${members.members().size} cluster members:")
        members.members().forEach { member ->
            println("- ${member.id} at ${member.address} with kinds: ${member.kinds.joinToString(", ")}")
        }
        
        // Update the Rendezvous hash with the members
        rendezvous.updateMembers(members)
        
        // Create some cluster identities
        val identities = listOf(
            ClusterIdentity("user", "user-1"),
            ClusterIdentity("user", "user-2"),
            ClusterIdentity("user", "user-3"),
            ClusterIdentity("order", "order-1"),
            ClusterIdentity("order", "order-2"),
            ClusterIdentity("payment", "payment-1")
        )
        
        // Map the identities to members using Rendezvous hash
        println("\nMapping identities to members:")
        identities.forEach { identity ->
            val memberAddress = rendezvous.getByClusterIdentity(identity)
            val member = members.members().find { it.address == memberAddress }
            println("- ${identity.kind}/${identity.identity} -> ${member?.id ?: "None"}")
        }
        
        // Demonstrate consistency when adding a new member
        println("\nAdding a new member...")
        val newMember = Member(
            id = "member6",
            host = "host6",
            port = 8006,
            kinds = listOf("user", "order"),
            alive = true
        )
        members.add(newMember)
        rendezvous.updateMembers(members)
        
        println("\nMapping identities to members after adding a new member:")
        identities.forEach { identity ->
            val memberAddress = rendezvous.getByClusterIdentity(identity)
            val member = members.members().find { it.address == memberAddress }
            println("- ${identity.kind}/${identity.identity} -> ${member?.id ?: "None"}")
        }
        
        // Demonstrate consistency when removing a member
        println("\nRemoving a member...")
        members.remove(members.members()[0])
        rendezvous.updateMembers(members)
        
        println("\nMapping identities to members after removing a member:")
        identities.forEach { identity ->
            val memberAddress = rendezvous.getByClusterIdentity(identity)
            val member = members.members().find { it.address == memberAddress }
            println("- ${identity.kind}/${identity.identity} -> ${member?.id ?: "None"}")
        }
        
        // Demonstrate distribution of identities
        println("\nDistribution of 1000 random identities:")
        val randomIdentities = (1..1000).map { 
            ClusterIdentity("user", "user-${UUID.randomUUID()}") 
        }
        
        val distribution = randomIdentities
            .map { rendezvous.getByClusterIdentity(it) }
            .groupBy { it }
            .mapValues { it.value.size }
            .toList()
            .sortedByDescending { it.second }
        
        distribution.forEach { (address, count) ->
            val member = members.members().find { it.address == address }
            val percentage = count / 10.0
            println("- ${member?.id ?: "None"}: $count identities (${percentage}%)")
        }
    }
    
    private fun createMembers(count: Int): MemberList {
        val memberList = MemberList()
        for (i in 1..count) {
            val member = Member(
                id = "member$i",
                host = "host$i",
                port = 8000 + i,
                kinds = when (i % 3) {
                    0 -> listOf("user", "order", "payment")
                    1 -> listOf("user", "order")
                    else -> listOf("user")
                },
                alive = true
            )
            memberList.add(member)
        }
        return memberList
    }
}
