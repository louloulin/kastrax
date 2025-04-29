package actor.proto

import actor.proto.mailbox.SystemMessage

/**
 * FutureContinuation is a system message that is sent to an actor to resume processing
 * after a future completes.
 */
class FutureContinuation(
    val function: () -> Unit,
    val message: Any
) : SystemMessage
