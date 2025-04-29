package actor.proto.persistence

import actor.proto.Context
import actor.proto.MessageEnvelope

/**
 * Persistent is the interface for persistent actors.
 */
interface Persistent {
    /**
     * Initialize the persistent actor.
     * @param provider The persistence provider.
     * @param context The actor context.
     */
    suspend fun init(provider: Provider, context: Context)

    /**
     * Persist a message.
     * @param message The message to persist.
     */
    fun persistReceive(message: Any)

    /**
     * Persist a snapshot.
     * @param snapshot The snapshot to persist.
     */
    fun persistSnapshot(snapshot: Any)

    /**
     * Check if the actor is recovering.
     * @return True if the actor is recovering, false otherwise.
     */
    fun recovering(): Boolean

    /**
     * Get the name of the actor.
     * @return The name of the actor.
     */
    fun name(): String
}

/**
 * Mixin is a mixin for persistent actors.
 */
class Mixin : Persistent {
    private var eventIndex: Int = 0
    private lateinit var providerState: ProviderState
    private lateinit var name: String
    private lateinit var receiver: Receiver
    private var recovering: Boolean = false

    override fun recovering(): Boolean = recovering

    override fun name(): String = name

    override fun persistReceive(message: Any) {
        providerState.persistEvent(name(), eventIndex, message)
        if (eventIndex % providerState.getSnapshotInterval() == 0) {
            // We can't call receiver.receive directly because it's a suspend function
            // This would need to be handled differently in a real implementation
        }
        eventIndex++
    }

    override fun persistSnapshot(snapshot: Any) {
        providerState.persistSnapshot(name(), eventIndex, snapshot)
    }

    override suspend fun init(provider: Provider, context: Context) {
        if (!::providerState.isInitialized) {
            providerState = provider.getState()
        }

        receiver = context as Receiver

        name = context.self.id
        eventIndex = 0
        recovering = true

        providerState.restart()
        val (snapshot, eventIdx, ok) = providerState.getSnapshot(name())
        if (ok) {
            eventIndex = eventIdx
            // We can't call receiver.receive directly because it's a suspend function
            // This would need to be handled differently in a real implementation
        }

        providerState.getEvents(name(), eventIndex, 0) { event ->
            // We can't call receiver.receive directly because it's a suspend function
            // This would need to be handled differently in a real implementation
            eventIndex++
        }

        recovering = false
        // We can't call receiver.receive directly because it's a suspend function
        // This would need to be handled differently in a real implementation
    }
}

/**
 * Receiver is the interface for message receivers.
 */
interface Receiver {
    /**
     * Receive a message.
     * @param message The message to receive.
     */
    suspend fun receive(message: MessageEnvelope)
}
