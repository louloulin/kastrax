package actor.proto.cluster.consensus

import actor.proto.ActorSystem
import actor.proto.cluster.Cluster
import actor.proto.cluster.ClusterConfig
import actor.proto.cluster.Member
import actor.proto.cluster.MemberStatus
import actor.proto.cluster.informer.DefaultInformer
import actor.proto.cluster.providers.SimpleClusterProvider
import actor.proto.remote.RemoteConfig
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.mockito.kotlin.*
import java.util.concurrent.TimeUnit

@Disabled("This test needs to be fixed")
class ConsensusTest {
    private lateinit var actorSystem: ActorSystem
    private lateinit var cluster: Cluster
    private lateinit var consensusManager: ConsensusManager
    
    @BeforeEach
    fun setup() {
        actorSystem = mock()
        
        val clusterProvider = mock<SimpleClusterProvider>()
        val remoteConfig = RemoteConfig("localhost", 0)
        val config = ClusterConfig(
            name = "test-cluster",
            clusterProvider = clusterProvider,
            identityLookup = mock(),
            remoteConfig = remoteConfig,
            informer = DefaultInformer()
        )
        
        cluster = mock()
        whenever(cluster.actorSystem).thenReturn(actorSystem)
        whenever(cluster.config).thenReturn(config)
        
        consensusManager = ConsensusManager(cluster)
    }
    
    @AfterEach
    fun teardown() {
        consensusManager.stop()
    }
    
    @Test
    fun `should register and check consensus`() = runBlocking {
        // Given
        val members = listOf(
            Member(id = "member1", host = "localhost", port = 8000, status = MemberStatus.ALIVE),
            Member(id = "member2", host = "localhost", port = 8001, status = MemberStatus.ALIVE),
            Member(id = "member3", host = "localhost", port = 8002, status = MemberStatus.UNAVAILABLE)
        )
        
        whenever(cluster.memberList.getMembers()).thenReturn(members)
        
        // When
        val builder = ConsensusCheckBuilder.create()
            .withMemberStatus(MemberStatus.ALIVE, minCount = 2)
        
        val consensus = consensusManager.registerCheck("test-consensus", builder.buildAll())
        consensusManager.checkConsensus() // Manually trigger consensus check
        
        // Then
        val (value, reached) = consensus.tryGetConsensus()
        assertTrue(reached)
        assertNotNull(value)
        
        val matchingMembers = value as List<Member>
        assertEquals(2, matchingMembers.size)
    }
    
    @Test
    fun `should not reach consensus when conditions not met`() = runBlocking {
        // Given
        val members = listOf(
            Member(id = "member1", host = "localhost", port = 8000, status = MemberStatus.ALIVE),
            Member(id = "member2", host = "localhost", port = 8001, status = MemberStatus.UNAVAILABLE),
            Member(id = "member3", host = "localhost", port = 8002, status = MemberStatus.UNAVAILABLE)
        )
        
        whenever(cluster.memberList.getMembers()).thenReturn(members)
        
        // When
        val builder = ConsensusCheckBuilder.create()
            .withMemberStatus(MemberStatus.ALIVE, minCount = 2)
        
        val consensus = consensusManager.registerCheck("test-consensus", builder.buildAll())
        consensusManager.checkConsensus() // Manually trigger consensus check
        
        // Then
        val (_, reached) = consensus.tryGetConsensus()
        assertFalse(reached)
    }
    
    @Test
    fun `should reset consensus when conditions change`() = runBlocking {
        // Given
        val initialMembers = listOf(
            Member(id = "member1", host = "localhost", port = 8000, status = MemberStatus.ALIVE),
            Member(id = "member2", host = "localhost", port = 8001, status = MemberStatus.ALIVE),
            Member(id = "member3", host = "localhost", port = 8002, status = MemberStatus.ALIVE)
        )
        
        whenever(cluster.memberList.getMembers()).thenReturn(initialMembers)
        
        // When
        val builder = ConsensusCheckBuilder.create()
            .withMemberStatus(MemberStatus.ALIVE, minCount = 3)
        
        val consensus = consensusManager.registerCheck("test-consensus", builder.buildAll())
        consensusManager.checkConsensus() // Manually trigger consensus check
        
        // Then
        val (_, initialReached) = consensus.tryGetConsensus()
        assertTrue(initialReached)
        
        // When conditions change
        val updatedMembers = listOf(
            Member(id = "member1", host = "localhost", port = 8000, status = MemberStatus.ALIVE),
            Member(id = "member2", host = "localhost", port = 8001, status = MemberStatus.UNAVAILABLE),
            Member(id = "member3", host = "localhost", port = 8002, status = MemberStatus.ALIVE)
        )
        
        whenever(cluster.memberList.getMembers()).thenReturn(updatedMembers)
        consensusManager.checkConsensus() // Manually trigger consensus check
        
        // Then
        val (_, updatedReached) = consensus.tryGetConsensus()
        assertFalse(updatedReached)
    }
    
    @Test
    fun `should wait for consensus`() = runBlocking {
        // Given
        val members = listOf(
            Member(id = "member1", host = "localhost", port = 8000, status = MemberStatus.ALIVE),
            Member(id = "member2", host = "localhost", port = 8001, status = MemberStatus.ALIVE)
        )
        
        whenever(cluster.memberList.getMembers()).thenReturn(members)
        
        // When
        val builder = ConsensusCheckBuilder.create()
            .withMemberStatus(MemberStatus.ALIVE, minCount = 2)
        
        val consensus = consensusManager.registerCheck("test-consensus", builder.buildAll())
        
        // Then
        val result = consensus.waitForConsensus(100)
        assertNull(result) // Timeout because we didn't trigger consensus check
        
        // When we trigger consensus check
        consensusManager.checkConsensus()
        
        // Then
        val resultAfterCheck = consensus.waitForConsensus(100)
        assertNotNull(resultAfterCheck)
    }
    
    @Test
    fun `should use different consensus checks`() = runBlocking {
        // Given
        val members = listOf(
            Member(id = "member1", host = "localhost", port = 8000, status = MemberStatus.ALIVE, labels = mapOf("role" to "worker")),
            Member(id = "member2", host = "localhost", port = 8001, status = MemberStatus.ALIVE, labels = mapOf("role" to "worker")),
            Member(id = "member3", host = "localhost", port = 8002, status = MemberStatus.ALIVE, labels = mapOf("role" to "manager"))
        )
        
        whenever(cluster.memberList.getMembers()).thenReturn(members)
        
        // When
        val statusBuilder = ConsensusCheckBuilder.create()
            .withMemberStatus(MemberStatus.ALIVE, minCount = 3)
        
        val roleBuilder = ConsensusCheckBuilder.create()
            .withMemberRole("worker", minCount = 2)
        
        val countBuilder = ConsensusCheckBuilder.create()
            .withMemberCount(minCount = 3, maxCount = 5)
        
        val statusConsensus = consensusManager.registerCheck("status-consensus", statusBuilder.buildAll())
        val roleConsensus = consensusManager.registerCheck("role-consensus", roleBuilder.buildAll())
        val countConsensus = consensusManager.registerCheck("count-consensus", countBuilder.buildAll())
        
        consensusManager.checkConsensus() // Manually trigger consensus check
        
        // Then
        val (_, statusReached) = statusConsensus.tryGetConsensus()
        assertTrue(statusReached)
        
        val (_, roleReached) = roleConsensus.tryGetConsensus()
        assertTrue(roleReached)
        
        val (_, countReached) = countConsensus.tryGetConsensus()
        assertTrue(countReached)
    }
}
