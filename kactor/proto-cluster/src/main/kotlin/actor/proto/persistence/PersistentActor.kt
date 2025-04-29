package actor.proto.persistence

import actor.proto.Actor
import actor.proto.Context

/**
 * PersistentActor is a mock class for persistent actors.
 * This is a temporary solution until the real persistence module is implemented.
 */
abstract class PersistentActor : Actor {
    /**
     * Receive and process recovery messages.
     * @param context The actor context.
     * @param message The message to process.
     */
    abstract suspend fun receiveRecover(context: Context, message: Any)

    /**
     * Receive and process command messages.
     * @param context The actor context.
     * @param message The message to process.
     */
    abstract suspend fun receiveCommand(context: Context, message: Any)

    /**
     * Persist a snapshot of the actor's state.
     * @param snapshot The snapshot to persist.
     */
    suspend fun persistSnapshot(snapshot: Any) {
        // Mock implementation
    }

    override suspend fun Context.receive(msg: Any) {
        receiveCommand(this, msg)
    }
}