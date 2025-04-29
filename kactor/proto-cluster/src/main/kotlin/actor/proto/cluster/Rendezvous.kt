package actor.proto.cluster

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Rendezvous hash implementation for cluster member selection.
 * This is a Kotlin implementation of the Rendezvous (highest random weight) hashing algorithm.
 * It's used to consistently map cluster identities to cluster members.
 */
class Rendezvous {
    private val rwLock = ReentrantReadWriteLock()
    private val hasher = MessageDigest.getInstance("SHA-256")
    private val hashLock = Any()
    private var members = listOf<MemberData>()

    /**
     * Get the address of the member that should handle the given cluster identity.
     * @param clusterIdentity The cluster identity to get the member for.
     * @return The address of the selected member, or empty string if no members are available.
     */
    fun getByClusterIdentity(clusterIdentity: ClusterIdentity): String {
        return rwLock.read {
            val identity = clusterIdentity.identity
            val membersByKind = memberDataByKind(clusterIdentity.kind)

            when (membersByKind.size) {
                0 -> ""
                1 -> membersByKind[0].member.address()
                else -> {
                    val keyBytes = identity.toByteArray(StandardCharsets.UTF_8)

                    var maxScore = 0
                    var maxMember: MemberData? = null

                    for (node in membersByKind) {
                        val score = hash(node.hashBytes, keyBytes)
                        if (score > maxScore) {
                            maxScore = score
                            maxMember = node
                        }
                    }

                    maxMember?.member?.address() ?: ""
                }
            }
        }
    }

    /**
     * Get the address of the member that should handle the given identity.
     * @param identity The identity string in the format "kind/id".
     * @return The address of the selected member, or empty string if no members are available.
     */
    fun getByIdentity(identity: String): String {
        val parts = identity.split("/", limit = 2)
        if (parts.size != 2) {
            return ""
        }

        return getByClusterIdentity(ClusterIdentity(identity = parts[1], kind = parts[0]))
    }

    /**
     * Update the list of members used for hashing.
     * @param memberList The list of members.
     */
    fun updateMembers(memberList: List<Member>) {
        rwLock.write {
            this.members = memberList.map { member ->
                val keyBytes = member.address().toByteArray(StandardCharsets.UTF_8)
                MemberData(member, keyBytes)
            }
        }
    }

    /**
     * Get the members that can handle the given kind.
     * @param kind The kind to filter members by.
     * @return A list of members that can handle the given kind.
     */
    private fun memberDataByKind(kind: String): List<MemberData> {
        // In a real implementation, we would filter members by kind
        // For now, we'll return all members since the Member class doesn't have a hasKind method
        return members
    }

    /**
     * Hash the combination of node and key bytes.
     * @param nodeBytes The bytes representing the node.
     * @param keyBytes The bytes representing the key.
     * @return A hash value.
     */
    private fun hash(nodeBytes: ByteArray, keyBytes: ByteArray): Int {
        synchronized(hashLock) {
            hasher.reset()
            hasher.update(keyBytes)
            hasher.update(nodeBytes)
            val hash = hasher.digest()
            // Convert first 4 bytes to an Int (using unsigned conversion)
            return ((hash[0].toInt() and 0xFF) shl 24) or
                   ((hash[1].toInt() and 0xFF) shl 16) or
                   ((hash[2].toInt() and 0xFF) shl 8) or
                   (hash[3].toInt() and 0xFF)
        }
    }

    /**
     * Data class to hold a member and its pre-computed hash bytes.
     */
    private data class MemberData(val member: Member, val hashBytes: ByteArray) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as MemberData

            if (member != other.member) return false
            if (!hashBytes.contentEquals(other.hashBytes)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = member.hashCode()
            result = 31 * result + hashBytes.contentHashCode()
            return result
        }
    }

    companion object {
        /**
         * Create a new Rendezvous hash instance.
         * @return A new Rendezvous hash instance.
         */
        fun create(): Rendezvous {
            return Rendezvous()
        }
    }
}
