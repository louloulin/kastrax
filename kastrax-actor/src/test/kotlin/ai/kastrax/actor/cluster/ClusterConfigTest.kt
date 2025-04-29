package ai.kastrax.actor.cluster

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.time.Duration

class ClusterConfigTest {

    @Test
    fun `test default cluster configuration`() {
        val config = ClusterConfig()

        assertEquals("0.0.0.0", config.hostname)
        assertEquals(8090, config.port)
        assertEquals("kastrax-cluster", config.clusterName)
        assertTrue(config.seeds.isEmpty())
        assertEquals(Duration.ofMillis(300), config.gossipInterval)
        assertEquals(Duration.ofSeconds(5), config.requestTimeout)
    }

    @Test
    fun `test custom cluster configuration`() {
        val config = ClusterConfig(
            hostname = "127.0.0.1",
            port = 8091,
            clusterName = "test-cluster",
            seeds = listOf("localhost:8090", "localhost:8091"),
            gossipInterval = Duration.ofMillis(500),
            requestTimeout = Duration.ofSeconds(3)
        )

        assertEquals("127.0.0.1", config.hostname)
        assertEquals(8091, config.port)
        assertEquals("test-cluster", config.clusterName)
        assertEquals(2, config.seeds.size)
        assertEquals("localhost:8090", config.seeds[0])
        assertEquals("localhost:8091", config.seeds[1])
        assertEquals(Duration.ofMillis(500), config.gossipInterval)
        assertEquals(Duration.ofSeconds(3), config.requestTimeout)
    }

    @Test
    fun `test conversion to kactor cluster config`() {
        val config = ClusterConfig(
            hostname = "127.0.0.1",
            port = 8091,
            clusterName = "test-cluster",
            seeds = listOf("localhost:8090", "localhost:8091")
        )

        val kactorConfig = config.toKactorClusterConfig()

        assertEquals("test-cluster", kactorConfig.name)
        assertEquals("127.0.0.1", kactorConfig.remoteConfig.hostname)
        assertEquals(8091, kactorConfig.remoteConfig.port)

        // Get the cluster provider and check the seeds
        val provider = kactorConfig.clusterProvider
        assertNotNull(provider)

        // Check that the identity lookup is set
        assertNotNull(kactorConfig.identityLookup)
    }
}
