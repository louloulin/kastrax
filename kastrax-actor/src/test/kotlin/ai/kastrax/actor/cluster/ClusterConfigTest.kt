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
        assertEquals(1, config.minClusterSize)
        assertEquals(Duration.ofSeconds(1), config.gossipInterval)
        assertEquals(Duration.ofSeconds(1), config.heartbeatInterval)
        assertEquals(Duration.ofSeconds(1), config.monitorInterval)
        assertEquals(Duration.ofSeconds(5), config.deathThreshold)
    }
    
    @Test
    fun `test custom cluster configuration`() {
        val config = ClusterConfig(
            hostname = "127.0.0.1",
            port = 8091,
            clusterName = "test-cluster",
            seeds = listOf("localhost:8090", "localhost:8091"),
            minClusterSize = 2,
            gossipInterval = Duration.ofMillis(500),
            heartbeatInterval = Duration.ofMillis(500),
            monitorInterval = Duration.ofMillis(500),
            deathThreshold = Duration.ofSeconds(3)
        )
        
        assertEquals("127.0.0.1", config.hostname)
        assertEquals(8091, config.port)
        assertEquals("test-cluster", config.clusterName)
        assertEquals(2, config.seeds.size)
        assertEquals("localhost:8090", config.seeds[0])
        assertEquals("localhost:8091", config.seeds[1])
        assertEquals(2, config.minClusterSize)
        assertEquals(Duration.ofMillis(500), config.gossipInterval)
        assertEquals(Duration.ofMillis(500), config.heartbeatInterval)
        assertEquals(Duration.ofMillis(500), config.monitorInterval)
        assertEquals(Duration.ofSeconds(3), config.deathThreshold)
    }
    
    @Test
    fun `test conversion to kactor cluster config`() {
        val config = ClusterConfig(
            hostname = "127.0.0.1",
            port = 8091,
            clusterName = "test-cluster",
            seeds = listOf("localhost:8090", "localhost:8091"),
            minClusterSize = 2
        )
        
        val kactorConfig = config.toKactorClusterConfig()
        
        assertEquals("test-cluster", kactorConfig.clusterName)
        assertEquals("127.0.0.1:8091", kactorConfig.address)
        assertEquals(2, kactorConfig.seeds.size)
        assertEquals("localhost:8090", kactorConfig.seeds[0])
        assertEquals("localhost:8091", kactorConfig.seeds[1])
        assertEquals(2, kactorConfig.minClusterSize)
    }
}
