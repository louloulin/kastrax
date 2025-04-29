package actor.proto.remote

import actor.proto.PID
import actor.proto.Process
import actor.proto.Unwatch
import actor.proto.Watch
import actor.proto.mailbox.SystemMessage
import actor.proto.send

class RemoteProcess(private val pid: PID) : Process() {
    override fun sendUserMessage(pid: PID, message: Any) = send(message)
    override fun sendSystemMessage(pid: PID, message: SystemMessage) = send(message)
    private fun send(msg: Any) {
        try {
            when (msg) {
                is Watch -> send(Remote.endpointManagerPid, RemoteWatch(msg.watcher, pid))
                is Unwatch -> send(Remote.endpointManagerPid, RemoteUnwatch(msg.watcher, pid))
                else -> Remote.sendMessage(pid, msg, -1)
            }
        } catch (e: Exception) {
            // Handle errors based on exception type
            val statusCode = when {
                e.message?.contains("unavailable", ignoreCase = true) == true -> ResponseStatusCode.UNAVAILABLE
                e.message?.contains("timeout", ignoreCase = true) == true -> ResponseStatusCode.TIMEOUT
                else -> ResponseStatusCode.ERROR
            }

            // Log the error
            println("Remote communication error: ${e.message}, Status code: $statusCode")

            // If this is a request-response pattern, we could send back an error response here
            // For now, we'll just propagate the exception
            throw ResponseError(statusCode)
        }
    }
}

