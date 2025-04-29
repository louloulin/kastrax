package actor.proto.cluster

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import java.util.*

class RendezvousTest {

    private lateinit var rendezvous: Rendezvous
    private lateinit var members: List<Member>

    @BeforeEach
    fun setup() {
        rendezvous = Rendezvous.create()
        members = createTestMembers(5)
        rendezvous.updateMembers(members)
    }

    @Test
    fun `should return empty string when no members are available`() {
        // Arrange
        val emptyMembers = emptyList<Member>()
        rendezvous.updateMembers(emptyMembers)

        // Act
        val result = rendezvous.getByClusterIdentity(ClusterIdentity(kind = "kind1", identity = "id1"))

        // Assert
        assertEquals("", result)
    }

    @Test
    fun `should return the only member when only one member is available`() {
        // Arrange
        val singleMember = createTestMembers(1)
        rendezvous.updateMembers(singleMember)

        // Act
        val result = rendezvous.getByClusterIdentity(ClusterIdentity(kind = "kind1", identity = "id1"))

        // Assert
        assertEquals(singleMember[0].address(), result)
    }

    @Test
    fun `should consistently return the same member for the same identity`() {
        // Arrange
        val identity = ClusterIdentity(kind = "kind1", identity = "id1")

        // Act
        val result1 = rendezvous.getByClusterIdentity(identity)
        val result2 = rendezvous.getByClusterIdentity(identity)

        // Assert
        assertNotEquals("", result1)
        assertEquals(result1, result2)
    }

    @Test
    fun `should distribute identities evenly across members`() {
        // Arrange
        val members = createTestMembers(5)
        rendezvous.updateMembers(members)
        val identities = (1..1000).map { ClusterIdentity(kind = "kind1", identity = "id$it") }
        val memberAddresses = members.map { it.address() }.toSet()

        // Act
        val distribution = identities.map { rendezvous.getByClusterIdentity(it) }
            .groupBy { it }
            .mapValues { it.value.size }

        // Assert
        assertEquals(memberAddresses, distribution.keys)

        // Check that the distribution is relatively even
        val min = distribution.values.minOrNull() ?: 0
        val max = distribution.values.maxOrNull() ?: 0
        val ratio = max.toDouble() / min.toDouble()

        // The ratio should be close to 1.0 for an even distribution
        // Allow some variance due to the random nature of the hash
        assertTrue(ratio < 1.5, "Distribution ratio $ratio should be less than 1.5")
    }

    @Test
    fun `should parse identity string correctly`() {
        // Arrange
        val identity = "kind1/id1"

        // Act
        val result = rendezvous.getByIdentity(identity)

        // Assert
        assertNotEquals("", result)
    }

    @Test
    fun `should return empty string for invalid identity format`() {
        // Arrange
        val identity = "invalid-format"

        // Act
        val result = rendezvous.getByIdentity(identity)

        // Assert
        assertEquals("", result)
    }

    @Test
    fun `should handle member updates correctly`() {
        // Arrange
        val identity = ClusterIdentity(kind = "kind1", identity = "id1")
        rendezvous.getByClusterIdentity(identity)

        // Act
        val newMembers = createTestMembers(10)
        rendezvous.updateMembers(newMembers)
        val newResult = rendezvous.getByClusterIdentity(identity)

        // Assert
        // The result might change after updating members, but it should not be empty
        assertNotEquals("", newResult)
    }

    @Test
    fun `should maintain consistency when members are added`() {
        // Arrange
        val identities = (1..100).map { ClusterIdentity(kind = "kind1", identity = "id$it") }
        val initialResults = identities.associateWith { rendezvous.getByClusterIdentity(it) }

        // Act
        // Add more members
        val newMembers = createTestMembers(10)
        rendezvous.updateMembers(newMembers)
        val newResults = identities.associateWith { rendezvous.getByClusterIdentity(it) }

        // Assert
        // Some identities should still map to the same members
        val unchangedCount = identities.count { initialResults[it] == newResults[it] }

        // With Rendezvous hashing, adding members should maintain some consistency
        // but not perfect consistency
        assertTrue(unchangedCount > 0, "Some identities should maintain their mapping")
    }

    // Helper method to create test members
    private fun createTestMembers(count: Int): List<Member> {
        val members = mutableListOf<Member>()
        for (i in 1..count) {
            val member = Member(
                id = "member$i",
                host = "host$i",
                port = 8000 + i
            )
            members.add(member)
        }
        return members
    }
}
