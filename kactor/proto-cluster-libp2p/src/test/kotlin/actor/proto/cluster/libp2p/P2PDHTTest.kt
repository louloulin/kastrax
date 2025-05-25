package actor.proto.cluster.libp2p

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterConfig
import actor.proto.cluster.ClusterIdentity
import actor.proto.cluster.IdentityLookup
import actor.proto.remote.RemoteConfig
import ai.kastrax.runtime.coroutines.KastraxCoroutineGlobal
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import java.time.Duration

class P2PDHTTest {

    private lateinit var dht: P2PDHT
    private lateinit var cluster: Cluster

    @BeforeEach
    fun setup() {
        val config = P2PClusterConfig(
            clusterName = "test-cluster"
        )

        val actorSystem = ActorSystem("test-system")
        val mockIdentityLookup = mock(IdentityLookup::class.java)
        val mockRemoteConfig = mock(RemoteConfig::class.java)

        val clusterConfig = ClusterConfig(
            name = "test-cluster",
            clusterProvider = mock(P2PClusterProvider::class.java),
            identityLookup = mockIdentityLookup,
            remoteConfig = mockRemoteConfig
        )

        cluster = Cluster(actorSystem, clusterConfig)
        dht = P2PDHT(cluster, config)
        dht.start()
    }

    @Test
    fun `should put and get value`() = KastraxCoroutineGlobal.runBlocking {
        // Arrange
        val key = "test-key"
        val value = "test-value".toByteArray()

        // Act
        val putResult = dht.put(key, value)
        val retrievedValue = dht.get(key)

        // Assert
        assertTrue(putResult)
        assertNotNull(retrievedValue)
        assertEquals(String(value), String(retrievedValue!!))
    }

    @Test
    fun `should return null for non-existent key`() = KastraxCoroutineGlobal.runBlocking {
        // Arrange
        val key = "non-existent-key"

        // Act
        val retrievedValue = dht.get(key)

        // Assert
        assertNull(retrievedValue)
    }

    @Test
    fun `should register and lookup actor`() = KastraxCoroutineGlobal.runBlocking {
        // Arrange
        val clusterIdentity = ClusterIdentity("test-kind", "test-id")
        val pid = PID("test-node", "test-kind/test-id")

        // Act
        val registerResult = dht.register(clusterIdentity, pid)
        val lookedUpPid = dht.lookup(clusterIdentity)

        // Assert
        assertTrue(registerResult)
        assertNotNull(lookedUpPid)
        assertEquals(pid.address, lookedUpPid!!.address)
        assertEquals(pid.id, lookedUpPid.id)
    }

    @Test
    fun `should return null for non-existent actor`() = KastraxCoroutineGlobal.runBlocking {
        // Arrange
        val clusterIdentity = ClusterIdentity("non-existent-kind", "non-existent-id")

        // Act
        val lookedUpPid = dht.lookup(clusterIdentity)

        // Assert
        assertNull(lookedUpPid)
    }

    @Test
    fun `should get stats`() {
        // Act
        val stats = dht.getStats()

        // Assert
        assertNotNull(stats)
        // Initial stats should have zero counts
        assertEquals(0, stats.hitRatio)
    }
}
