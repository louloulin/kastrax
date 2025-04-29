package actor.proto.diagnostics

import actor.proto.ActorSystem
import actor.proto.diagnostics.MatchType
import actor.proto.PID
import actor.proto.ProcessRegistry
import actor.proto.fromProducer
import actor.proto.send
import actor.proto.stop
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DiagnosticsIntegrationTest {

    private lateinit var system: ActorSystem

    @BeforeEach
    fun setup() {
        system = ActorSystem("test")
    }

    @AfterEach
    fun teardown() {
        // Clean up
    }

    @Test
    fun `should get process info for multiple actors`() {
        // Create multiple actors
        val props = fromProducer { TestActor() }
        val pid1 = system.actorOf(props, "test-actor-1")
        val pid2 = system.actorOf(props, "test-actor-2")
        val pid3 = system.actorOf(props, "other-actor")

        // Send messages to actors to ensure they're initialized
        system.send(pid1, "hello")
        system.send(pid2, "hello")
        system.send(pid3, "hello")

        // Get process info for all actors
        val allProcessInfos = system.getAllProcessInfos()

        // Verify we have at least our 3 actors
        assertTrue(allProcessInfos.size >= 3)

        // Verify our actors are in the list
        val testActor1Info = allProcessInfos.find { it.pid == pid1 }
        val testActor2Info = allProcessInfos.find { it.pid == pid2 }
        val otherActorInfo = allProcessInfos.find { it.pid == pid3 }

        assertNotNull(testActor1Info)
        assertNotNull(testActor2Info)
        assertNotNull(otherActorInfo)

        // Verify process info contains expected data
        assertEquals(ProcessStatus.ALIVE, testActor1Info?.status)
        assertEquals(ProcessStatus.ALIVE, testActor2Info?.status)
        assertEquals(ProcessStatus.ALIVE, otherActorInfo?.status)

        // Stop the actors
        system.stop(pid1)
        system.stop(pid2)
        system.stop(pid3)
    }

    @Test
    fun `should list processes matching pattern`() {
        // 清理之前的测试可能留下的进程
        try {
            ProcessRegistry.remove(PID(ProcessRegistry.address, "test-actor-1"))
            ProcessRegistry.remove(PID(ProcessRegistry.address, "test-actor-2"))
            ProcessRegistry.remove(PID(ProcessRegistry.address, "other-actor"))
        } catch (e: Exception) {
            // 忽略异常
        }

        // Create multiple actors
        val props = fromProducer { TestActor() }
        val pid1 = system.actorOf(props, "test-actor-1")
        val pid2 = system.actorOf(props, "test-actor-2")
        val pid3 = system.actorOf(props, "other-actor")

        // List processes matching "test"
        val testActors = system.listProcesses("test", MatchType.MATCH_PART_OF_STRING)

        // Verify list
        assertTrue(testActors.contains(pid1))
        assertTrue(testActors.contains(pid2))
        assertFalse(testActors.contains(pid3))

        // List processes matching exact name
        val exactActors = system.listProcesses("test-actor-1", MatchType.MATCH_EXACT_STRING)

        // Verify list
        assertTrue(exactActors.contains(pid1))
        assertFalse(exactActors.contains(pid2))
        assertFalse(exactActors.contains(pid3))

        // List processes matching regex
        val regexActors = system.listProcesses(".*-actor-[12]", MatchType.MATCH_REGEX)

        // Verify list
        assertTrue(regexActors.contains(pid1))
        assertTrue(regexActors.contains(pid2))
        assertFalse(regexActors.contains(pid3))

        // Stop the actors
        system.stop(pid1)
        system.stop(pid2)
        system.stop(pid3)
    }

    @Test
    fun `should get detailed process info`() {
        // Create an actor
        val props = fromProducer { TestActor() }
        val pid = system.actorOf(props, "test-actor")

        // Send a message to the actor
        system.send(pid, "hello")

        // Get process info
        val processInfo = system.getProcessInfo(pid)

        // Verify process info
        assertEquals(pid, processInfo.pid)
        assertEquals(ProcessStatus.ALIVE, processInfo.status)
        assertNotNull(processInfo.mailboxType)
        assertNotNull(processInfo.createdAt)

        // Get detailed string
        val detailedString = processInfo.toDetailedString()

        // Verify detailed string contains expected information
        assertTrue(detailedString.contains(pid.id))
        assertTrue(detailedString.contains(pid.address))
        assertTrue(detailedString.contains(processInfo.status.toString()))
        assertTrue(detailedString.contains(processInfo.mailboxType))

        // Stop the actor
        system.stop(pid)
    }

    class TestActor : actor.proto.Actor {
        override suspend fun actor.proto.Context.receive(msg: Any) {
            // Do nothing
        }
    }
}
