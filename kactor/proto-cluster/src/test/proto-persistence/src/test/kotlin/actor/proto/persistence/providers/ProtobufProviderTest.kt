package actor.proto.persistence.providers

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.Serializable

class ProtobufProviderTest {
    @TempDir
    lateinit var tempDir: File
    
    private lateinit var filePath: String
    private lateinit var provider: ProtobufProvider
    
    @BeforeEach
    fun setup() {
        filePath = File(tempDir, "persistence.pb").absolutePath
        provider = ProtobufProvider(10, filePath)
    }
    
    @AfterEach
    fun teardown() {
        File(filePath).delete()
    }
    
    @Test
    fun `should persist and retrieve snapshot`() {
        // Given
        val actorName = "test-actor"
        val snapshot = TestSnapshot("test-data")
        val snapshotIndex = 5
        
        // When
        val state = provider.getState()
        state.persistSnapshot(actorName, snapshotIndex, snapshot)
        
        // Then
        val (retrievedSnapshot, retrievedIndex, found) = state.getSnapshot(actorName)
        assertTrue(found)
        assertEquals(snapshotIndex, retrievedIndex)
        assertEquals(snapshot, retrievedSnapshot)
    }
    
    @Test
    fun `should persist and retrieve events`() {
        // Given
        val actorName = "test-actor"
        val events = listOf(
            TestEvent("event-1"),
            TestEvent("event-2"),
            TestEvent("event-3")
        )
        
        // When
        val state = provider.getState()
        events.forEachIndexed { index, event ->
            state.persistEvent(actorName, index, event)
        }
        
        // Then
        val retrievedEvents = mutableListOf<Any>()
        state.getEvents(actorName, 0, 0) { event ->
            retrievedEvents.add(event)
        }
        
        assertEquals(events.size, retrievedEvents.size)
        for (i in events.indices) {
            assertEquals(events[i], retrievedEvents[i])
        }
    }
    
    @Test
    fun `should delete snapshots`() {
        // Given
        val actorName = "test-actor"
        val snapshot = TestSnapshot("test-data")
        val snapshotIndex = 5
        
        // When
        val state = provider.getState()
        state.persistSnapshot(actorName, snapshotIndex, snapshot)
        state.deleteSnapshots(actorName, snapshotIndex)
        
        // Then
        val (_, _, found) = state.getSnapshot(actorName)
        assertFalse(found)
    }
    
    @Test
    fun `should delete events`() {
        // Given
        val actorName = "test-actor"
        val events = listOf(
            TestEvent("event-1"),
            TestEvent("event-2"),
            TestEvent("event-3")
        )
        
        // When
        val state = provider.getState()
        events.forEachIndexed { index, event ->
            state.persistEvent(actorName, index, event)
        }
        state.deleteEvents(actorName, 1)
        
        // Then
        val retrievedEvents = mutableListOf<Any>()
        state.getEvents(actorName, 0, 0) { event ->
            retrievedEvents.add(event)
        }
        
        assertEquals(1, retrievedEvents.size)
        assertEquals(events[2], retrievedEvents[0])
    }
    
    @Test
    fun `should persist state to file and load it back`() {
        // Given
        val actorName = "test-actor"
        val snapshot = TestSnapshot("test-data")
        val snapshotIndex = 5
        val events = listOf(
            TestEvent("event-1"),
            TestEvent("event-2"),
            TestEvent("event-3")
        )
        
        // When
        val state = provider.getState()
        state.persistSnapshot(actorName, snapshotIndex, snapshot)
        events.forEachIndexed { index, event ->
            state.persistEvent(actorName, index, event)
        }
        
        // Create a new provider that loads the state from the file
        val newProvider = ProtobufProvider(10, filePath)
        val newState = newProvider.getState()
        
        // Then
        val (retrievedSnapshot, retrievedIndex, found) = newState.getSnapshot(actorName)
        assertTrue(found)
        assertEquals(snapshotIndex, retrievedIndex)
        assertEquals(snapshot, retrievedSnapshot)
        
        val retrievedEvents = mutableListOf<Any>()
        newState.getEvents(actorName, 0, 0) { event ->
            retrievedEvents.add(event)
        }
        
        assertEquals(events.size, retrievedEvents.size)
        for (i in events.indices) {
            assertEquals(events[i], retrievedEvents[i])
        }
    }
    
    @Test
    fun `should handle multiple actors`() {
        // Given
        val actor1 = "actor-1"
        val actor2 = "actor-2"
        val snapshot1 = TestSnapshot("snapshot-1")
        val snapshot2 = TestSnapshot("snapshot-2")
        val event1 = TestEvent("event-1")
        val event2 = TestEvent("event-2")
        
        // When
        val state = provider.getState()
        state.persistSnapshot(actor1, 1, snapshot1)
        state.persistSnapshot(actor2, 2, snapshot2)
        state.persistEvent(actor1, 0, event1)
        state.persistEvent(actor2, 0, event2)
        
        // Then
        val (retrievedSnapshot1, retrievedIndex1, found1) = state.getSnapshot(actor1)
        assertTrue(found1)
        assertEquals(1, retrievedIndex1)
        assertEquals(snapshot1, retrievedSnapshot1)
        
        val (retrievedSnapshot2, retrievedIndex2, found2) = state.getSnapshot(actor2)
        assertTrue(found2)
        assertEquals(2, retrievedIndex2)
        assertEquals(snapshot2, retrievedSnapshot2)
        
        val retrievedEvents1 = mutableListOf<Any>()
        state.getEvents(actor1, 0, 0) { event ->
            retrievedEvents1.add(event)
        }
        assertEquals(1, retrievedEvents1.size)
        assertEquals(event1, retrievedEvents1[0])
        
        val retrievedEvents2 = mutableListOf<Any>()
        state.getEvents(actor2, 0, 0) { event ->
            retrievedEvents2.add(event)
        }
        assertEquals(1, retrievedEvents2.size)
        assertEquals(event2, retrievedEvents2[0])
    }
}

/**
 * TestSnapshot is a test class for snapshots.
 */
data class TestSnapshot(val data: String) : Serializable

/**
 * TestEvent is a test class for events.
 */
data class TestEvent(val data: String) : Serializable
