package actor.proto

import actor.proto.logging.Logger

interface SenderContext {
    /**
     * Get the logger for this context
     * @return The logger
     */
    val logger: Logger
    val message: Any?
    val headers: MessageHeader?
    val sender: PID?
    val self: PID

    fun send(target: PID, message: Any)
    fun request(target: PID, message: Any)
}

