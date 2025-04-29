package actor.proto.persistence.providers

import actor.proto.persistence.EventStore
import actor.proto.persistence.Provider
import actor.proto.persistence.ProviderState
import actor.proto.persistence.SnapshotStore
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

/**
 * Entry is a persistence entry for an actor.
 */
data class Entry(
    var eventIndex: Int = 0,
    var snapshot: Any? = null,
    val events: MutableList<Any> = mutableListOf()
)

/**
 * InMemoryProvider is an in-memory implementation of the Provider interface.
 */
class InMemoryProvider(private val snapshotInterval: Int) : Provider {
    private val state = InMemoryProviderState(snapshotInterval)
    
    override fun getState(): ProviderState = state
}

/**
 * InMemoryProviderState is an in-memory implementation of the ProviderState interface.
 */
class InMemoryProviderState(private val snapshotInterval: Int) : ProviderState {
    private val lock = ReentrantReadWriteLock()
    private val store = ConcurrentHashMap<String, Entry>()
    
    /**
     * Load or initialize an entry for an actor.
     * @param actorName The name of the actor.
     * @return The entry and whether it was loaded.
     */
    private fun loadOrInit(actorName: String): Pair<Entry, Boolean> {
        lock.read {
            val entry = store[actorName]
            if (entry != null) {
                return entry to true
            }
        }
        
        lock.write {
            val entry = Entry()
            store[actorName] = entry
            return entry to false
        }
    }
    
    override fun restart() {
        // No-op for in-memory provider
    }
    
    override fun getSnapshotInterval(): Int = snapshotInterval
    
    override fun getSnapshot(actorName: String): Triple<Any?, Int, Boolean> {
        val (entry, loaded) = loadOrInit(actorName)
        if (!loaded || entry.snapshot == null) {
            return Triple(null, 0, false)
        }
        return Triple(entry.snapshot, entry.eventIndex, true)
    }
    
    override fun persistSnapshot(actorName: String, snapshotIndex: Int, snapshot: Any) {
        val (entry, _) = loadOrInit(actorName)
        entry.eventIndex = snapshotIndex
        entry.snapshot = snapshot
    }
    
    override fun deleteSnapshots(actorName: String, inclusiveToIndex: Int) {
        // No-op for in-memory provider
    }
    
    override fun getEvents(actorName: String, eventIndexStart: Int, eventIndexEnd: Int, callback: (Any) -> Unit) {
        val (entry, _) = loadOrInit(actorName)
        val end = if (eventIndexEnd == 0) entry.events.size else eventIndexEnd
        for (i in eventIndexStart until end) {
            callback(entry.events[i])
        }
    }
    
    override fun persistEvent(actorName: String, eventIndex: Int, event: Any) {
        val (entry, _) = loadOrInit(actorName)
        entry.events.add(event)
    }
    
    override fun deleteEvents(actorName: String, inclusiveToIndex: Int) {
        // No-op for in-memory provider
    }
}
