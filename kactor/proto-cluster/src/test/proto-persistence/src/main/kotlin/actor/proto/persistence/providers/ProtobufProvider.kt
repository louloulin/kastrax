package actor.proto.persistence.providers

import actor.proto.persistence.EventStore
import actor.proto.persistence.Provider
import actor.proto.persistence.ProviderState
import actor.proto.persistence.SnapshotStore
import actor.proto.persistence.proto.ActorState
import actor.proto.persistence.proto.Event
import actor.proto.persistence.proto.PersistenceState
import actor.proto.persistence.proto.Snapshot
import actor.proto.persistence.serialization.ProtobufSerializer
import actor.proto.persistence.serialization.Serializer
import com.google.protobuf.ByteString
import mu.KotlinLogging
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import kotlin.concurrent.read
import kotlin.concurrent.write

private val logger = KotlinLogging.logger {}

/**
 * ProtobufProvider is a Protobuf implementation of the Provider interface.
 * It persists actor state to a file using Protobuf serialization.
 * @param snapshotInterval The interval at which to take snapshots.
 * @param filePath The path to the file where the state will be persisted.
 * @param serializer The serializer to use for serializing and deserializing objects.
 */
class ProtobufProvider(
    private val snapshotInterval: Int,
    private val filePath: String,
    private val serializer: Serializer = ProtobufSerializer()
) : Provider {
    private val state = ProtobufProviderState(snapshotInterval, filePath, serializer)
    
    override fun getState(): ProviderState = state
}

/**
 * ProtobufProviderState is a Protobuf implementation of the ProviderState interface.
 * @param snapshotInterval The interval at which to take snapshots.
 * @param filePath The path to the file where the state will be persisted.
 * @param serializer The serializer to use for serializing and deserializing objects.
 */
class ProtobufProviderState(
    private val snapshotInterval: Int,
    private val filePath: String,
    private val serializer: Serializer
) : ProviderState {
    private val lock = ReentrantReadWriteLock()
    private val actorStates = ConcurrentHashMap<String, ActorState.Builder>()
    
    init {
        // Load the state from the file if it exists
        val file = File(filePath)
        if (file.exists()) {
            try {
                FileInputStream(file).use { fis ->
                    val persistenceState = PersistenceState.parseFrom(fis)
                    for (actorState in persistenceState.actorsList) {
                        actorStates[actorState.actorName] = actorState.toBuilder()
                    }
                }
                logger.info { "Loaded persistence state from $filePath" }
            } catch (e: Exception) {
                logger.error(e) { "Failed to load persistence state from $filePath" }
            }
        }
    }
    
    /**
     * Save the state to the file.
     */
    private fun saveState() {
        try {
            val persistenceStateBuilder = PersistenceState.newBuilder()
            for (actorState in actorStates.values) {
                persistenceStateBuilder.addActors(actorState.build())
            }
            
            val file = File(filePath)
            file.parentFile?.mkdirs()
            
            FileOutputStream(file).use { fos ->
                persistenceStateBuilder.build().writeTo(fos)
            }
            logger.info { "Saved persistence state to $filePath" }
        } catch (e: Exception) {
            logger.error(e) { "Failed to save persistence state to $filePath" }
        }
    }
    
    /**
     * Get or create an actor state.
     * @param actorName The name of the actor.
     * @return The actor state.
     */
    private fun getOrCreateActorState(actorName: String): ActorState.Builder {
        return lock.write {
            actorStates.computeIfAbsent(actorName) {
                ActorState.newBuilder().setActorName(actorName)
            }
        }
    }
    
    override fun restart() {
        // No-op for Protobuf provider
    }
    
    override fun getSnapshotInterval(): Int = snapshotInterval
    
    override fun getSnapshot(actorName: String): Triple<Any?, Int, Boolean> {
        val actorState = lock.read {
            actorStates[actorName]
        } ?: return Triple(null, 0, false)
        
        if (!actorState.hasSnapshot()) {
            return Triple(null, 0, false)
        }
        
        val snapshot = actorState.snapshot
        val deserializedSnapshot = serializer.deserialize(snapshot.data, snapshot.type)
        return Triple(deserializedSnapshot, snapshot.index, true)
    }
    
    override fun persistSnapshot(actorName: String, snapshotIndex: Int, snapshot: Any) {
        val actorState = getOrCreateActorState(actorName)
        
        val (serializedSnapshot, snapshotType) = serializer.serialize(snapshot)
        val snapshotBuilder = Snapshot.newBuilder()
            .setIndex(snapshotIndex)
            .setData(serializedSnapshot)
            .setType(snapshotType)
        
        lock.write {
            actorState.setSnapshot(snapshotBuilder)
            saveState()
        }
    }
    
    override fun deleteSnapshots(actorName: String, inclusiveToIndex: Int) {
        val actorState = lock.read {
            actorStates[actorName]
        } ?: return
        
        if (!actorState.hasSnapshot()) {
            return
        }
        
        val snapshot = actorState.snapshot
        if (snapshot.index <= inclusiveToIndex) {
            lock.write {
                actorState.clearSnapshot()
                saveState()
            }
        }
    }
    
    override fun getEvents(actorName: String, eventIndexStart: Int, eventIndexEnd: Int, callback: (Any) -> Unit) {
        val actorState = lock.read {
            actorStates[actorName]
        } ?: return
        
        val events = actorState.eventsList
        val end = if (eventIndexEnd == 0) events.size else eventIndexEnd
        
        for (i in eventIndexStart until end) {
            if (i < events.size) {
                val event = events[i]
                val deserializedEvent = serializer.deserialize(event.data, event.type)
                callback(deserializedEvent)
            }
        }
    }
    
    override fun persistEvent(actorName: String, eventIndex: Int, event: Any) {
        val actorState = getOrCreateActorState(actorName)
        
        val (serializedEvent, eventType) = serializer.serialize(event)
        val eventBuilder = Event.newBuilder()
            .setIndex(eventIndex)
            .setData(serializedEvent)
            .setType(eventType)
        
        lock.write {
            actorState.addEvents(eventBuilder)
            saveState()
        }
    }
    
    override fun deleteEvents(actorName: String, inclusiveToIndex: Int) {
        val actorState = lock.read {
            actorStates[actorName]
        } ?: return
        
        val events = actorState.eventsList
        if (events.isEmpty()) {
            return
        }
        
        lock.write {
            val newEvents = events.filter { it.index > inclusiveToIndex }
            actorState.clearEvents()
            for (event in newEvents) {
                actorState.addEvents(event)
            }
            saveState()
        }
    }
}
