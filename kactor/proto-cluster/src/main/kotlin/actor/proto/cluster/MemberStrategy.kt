package actor.proto.cluster

/**
 * MemberStrategy is responsible for selecting a member to host an actor.
 */
interface MemberStrategy {
    /**
     * Add a member to the strategy.
     * @param memberId The ID of the member to add.
     */
    fun addMember(memberId: String)

    /**
     * Remove a member from the strategy.
     * @param memberId The ID of the member to remove.
     */
    fun removeMember(memberId: String)

    /**
     * Get the partition member for a given identity.
     * @param identity The identity of the actor.
     * @return The ID of the member that should host the actor, or null if no members are available.
     */
    fun getPartition(identity: String): String?
}

/**
 * RoundRobinMemberStrategy selects members in a round-robin fashion.
 */
class RoundRobinMemberStrategy : MemberStrategy {
    private val members = mutableListOf<String>()
    private var index = 0

    override fun addMember(memberId: String) {
        synchronized(members) {
            if (!members.contains(memberId)) {
                members.add(memberId)
            }
        }
    }

    override fun removeMember(memberId: String) {
        synchronized(members) {
            members.remove(memberId)
            if (index >= members.size && members.isNotEmpty()) {
                index = 0
            }
        }
    }

    override fun getPartition(identity: String): String? {
        synchronized(members) {
            if (members.isEmpty()) {
                return null
            }

            val member = members[index]
            index = (index + 1) % members.size
            return member
        }
    }
}

/**
 * RendezvousMemberStrategy selects members using the rendezvous hashing algorithm.
 * This implementation uses the Rendezvous hash class for consistent hashing.
 */
class RendezvousMemberStrategy : MemberStrategy {
    private val members = mutableSetOf<String>()
    private val rendezvous = Rendezvous.create()
    private val membersList = mutableListOf<Member>()

    override fun addMember(memberId: String) {
        synchronized(members) {
            if (members.add(memberId)) {
                // Create a dummy member with the ID as address for the Rendezvous hash
                val member = Member(
                    id = memberId,
                    host = memberId, // Using ID as host for simplicity
                    port = 0
                )
                membersList.add(member)
                rendezvous.updateMembers(membersList)
            }
        }
    }

    override fun removeMember(memberId: String) {
        synchronized(members) {
            if (members.remove(memberId)) {
                // Find and remove the member from the member list
                val memberToRemove = membersList.find { it.id == memberId }
                if (memberToRemove != null) {
                    membersList.remove(memberToRemove)
                    rendezvous.updateMembers(membersList)
                }
            }
        }
    }

    override fun getPartition(identity: String): String? {
        synchronized(members) {
            if (members.isEmpty()) {
                return null
            }

            // Parse the identity to get the kind and ID
            // If the identity doesn't contain a slash, use a default kind
            val parts = if (identity.contains("/")) {
                identity.split("/", limit = 2)
            } else {
                listOf("default", identity)
            }

            val kind = parts[0]
            val id = parts[1]

            // Use the Rendezvous hash to get the member
            val address = rendezvous.getByClusterIdentity(ClusterIdentity(identity = id, kind = kind))

            // Find the member with the matching address
            return membersList.find { it.address() == address }?.id
        }
    }
}
