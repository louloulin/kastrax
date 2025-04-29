package actor.proto.persistence

import actor.proto.Actor
import actor.proto.Context
import actor.proto.MessageEnvelope
import kotlinx.coroutines.runBlocking

/**
 * PersistentActor is an actor that can persist its state.
 */
abstract class PersistentActor : Actor, Receiver {
    private val mixin = Mixin()

    /**
     * Initialize the persistent actor.
     * @param provider The persistence provider.
     * @param context The actor context.
     */
    suspend fun init(provider: Provider, context: Context) {
        mixin.init(provider, context)
    }

    /**
     * Persist a message.
     * @param message The message to persist.
     */
    fun persistReceive(message: Any) {
        mixin.persistReceive(message)
    }

    /**
     * Persist a snapshot.
     * @param snapshot The snapshot to persist.
     */
    fun persistSnapshot(snapshot: Any) {
        mixin.persistSnapshot(snapshot)
    }

    /**
     * Check if the actor is recovering.
     * @return True if the actor is recovering, false otherwise.
     */
    fun recovering(): Boolean = mixin.recovering()

    /**
     * Get the name of the actor.
     * @return The name of the actor.
     */
    fun name(): String = mixin.name()

    override suspend fun receive(message: MessageEnvelope) {
        // This method is called by the Mixin during recovery
        val context = message.sender as? Context ?: return
        context.receive(message.message)
    }

    /**
     * Handle a message during recovery.
     * @param context The actor context.
     * @param message The message to handle.
     */
    abstract suspend fun receiveRecover(context: Context, message: Any)

    /**
     * Handle a message during normal operation.
     * @param context The actor context.
     * @param message The message to handle.
     */
    abstract suspend fun receiveCommand(context: Context, message: Any)

    override suspend fun Context.receive(message: Any) {
        if (message is Replay) {
            // Initialize the actor with the provider
            val provider = message as? Provider ?: return
            // We need to call init in a coroutine because it's a suspend function
            kotlinx.coroutines.runBlocking {
                init(provider, this@receive)
            }
            return
        }

        if (recovering()) {
            receiveRecover(this, message)
        } else {
            receiveCommand(this, message)
        }
    }
}
