package actor.proto.cluster

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach

class RendezvousMemberStrategyTest {

    private lateinit var strategy: RendezvousMemberStrategy

    @BeforeEach
    fun setup() {
        strategy = RendezvousMemberStrategy()

        // Add some members
        for (i in 1..5) {
            strategy.addMember("member$i")
        }
    }

    @Test
    fun `should return null when no members are available`() {
        // Arrange
        val emptyStrategy = RendezvousMemberStrategy()

        // Act
        val result = emptyStrategy.getPartition("kind/id")

        // Assert
        assertNull(result)
    }

    @Test
    fun `should consistently return the same member for the same identity`() {
        // Arrange
        val identity = "kind/id1"

        // Act
        val result1 = strategy.getPartition(identity)
        val result2 = strategy.getPartition(identity)

        // Assert
        assertNotNull(result1)
        assertEquals(result1, result2)
    }

    @Test
    fun `should handle identity without kind correctly`() {
        // Arrange
        val identity = "id1"

        // Act
        val result = strategy.getPartition(identity)

        // Assert
        assertNotNull(result)
    }

    @Test
    fun `should distribute identities evenly across members`() {
        // Arrange
        val identities = (1..1000).map { "kind/id$it" }

        // Act
        val distribution = identities.mapNotNull { strategy.getPartition(it) }
            .groupBy { it }
            .mapValues { it.value.size }

        // Assert
        assertEquals(5, distribution.size)

        // Check that the distribution is relatively even
        val min = distribution.values.minOrNull() ?: 0
        val max = distribution.values.maxOrNull() ?: 0
        val ratio = max.toDouble() / min.toDouble()

        // The ratio should be close to 1.0 for an even distribution
        // Allow some variance due to the random nature of the hash
        assertTrue(ratio < 1.5, "Distribution ratio $ratio should be less than 1.5")
    }

    @Test
    fun `should maintain consistency when members are added`() {
        // Arrange
        val identities = (1..100).map { "kind/id$it" }
        val initialResults = identities.associateWith { strategy.getPartition(it) }

        // Act
        // Add more members
        for (i in 6..10) {
            strategy.addMember("member$i")
        }
        val newResults = identities.associateWith { strategy.getPartition(it) }

        // Assert
        // Some identities should still map to the same members
        val unchangedCount = identities.count { initialResults[it] == newResults[it] }

        // With Rendezvous hashing, adding members should maintain some consistency
        // but not perfect consistency
        assertTrue(unchangedCount > 0, "Some identities should maintain their mapping")
    }

    @Test
    fun `should maintain consistency when members are removed`() {
        // Arrange
        val identities = (1..100).map { "kind/id$it" }
        val initialResults = identities.associateWith { strategy.getPartition(it) }

        // Act
        // Remove a member
        strategy.removeMember("member1")
        val newResults = identities.associateWith { strategy.getPartition(it) }

        // Assert
        // Identities that were not mapped to the removed member should still map to the same members
        val unchangedCount = identities.count {
            initialResults[it] != "member1" && initialResults[it] == newResults[it]
        }

        // With Rendezvous hashing, removing members should maintain consistency for
        // identities that were not mapped to the removed member
        assertTrue(unchangedCount > 0, "Some identities should maintain their mapping")
    }
}
