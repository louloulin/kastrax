package actor.proto.cluster.grain

import actor.proto.Context
import actor.proto.persistence.PersistentActor
import actor.proto.persistence.ReplayComplete
import actor.proto.persistence.RequestSnapshot
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * GrainPersistentActor is a persistent actor that can be used as a grain.
 */
abstract class GrainPersistentActor<TState> : PersistentActor() {
    private var state: TState? = null

    /**
     * Get the initial state of the grain.
     * @return The initial state.
     */
    abstract fun initialState(): TState

    /**
     * Get the current state of the grain.
     * @return The current state.
     */
    fun getState(): TState {
        return state ?: initialState().also { state = it }
    }

    /**
     * Set the state of the grain.
     * @param newState The new state.
     */
    fun setState(newState: TState) {
        state = newState
    }

    override suspend fun receiveRecover(context: Context, message: Any) {
        when (message) {
            // Check if message is of type TState using reflection
            is Any -> {
                if (initialState() != null && initialState()!!::class.java.isAssignableFrom(message.javaClass)) {
                    @Suppress("UNCHECKED_CAST")
                    state = message as TState
                    logger.debug { "Recovered state: $state" }
                }
            }
            is ReplayComplete -> {
                logger.debug { "Recovery complete, state: $state" }
                if (state == null) {
                    state = initialState()
                }
            }
            else -> {
                logger.debug { "Received unknown message during recovery: $message" }
            }
        }
    }

    override suspend fun receiveCommand(context: Context, message: Any) {
        when (message) {
            is RequestSnapshot -> {
                val currentState = getState()
                persistSnapshot(currentState as Any)
                logger.debug { "Created snapshot, state: $currentState" }
            }
            else -> {
                handleCommand(context, message)
            }
        }
    }

    /**
     * Handle a command message.
     * @param context The actor context.
     * @param message The message to handle.
     */
    abstract suspend fun handleCommand(context: Context, message: Any)
}
