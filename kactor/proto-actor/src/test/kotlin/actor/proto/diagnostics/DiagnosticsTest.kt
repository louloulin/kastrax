package actor.proto.diagnostics

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.fromProducer
import actor.proto.send
import actor.proto.stop
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DiagnosticsTest {
    
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
    fun `should get process info`() {
        // Create an actor
        val props = fromProducer { TestActor() }
        val pid = system.actorOf(props, "test-actor")
        
        // Get process info
        val processInfo = system.getProcessInfo(pid)
        
        // Verify process info
        assertEquals(pid, processInfo.pid)
        assertEquals(ProcessStatus.ALIVE, processInfo.status)
        
        // Stop the actor
        system.stop(pid)
    }
    
    @Test
    fun `should list processes`() {
        // Create actors
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
    
    class TestActor : actor.proto.Actor {
        override suspend fun actor.proto.Context.receive(msg: Any) {
            // Do nothing
        }
    }
}
