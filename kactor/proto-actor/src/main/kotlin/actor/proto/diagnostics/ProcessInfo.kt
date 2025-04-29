package actor.proto.diagnostics

import actor.proto.PID
import java.time.Instant

/**
 * ProcessInfo contains diagnostic information about a process.
 */
data class ProcessInfo(
    val pid: PID,
    val mailboxType: String,
    val messageCount: Int,
    val status: ProcessStatus,
    val actorType: String = "Unknown",
    val createdAt: Instant = Instant.now(),
    val lastMessageReceivedAt: Instant? = null,
    val lastMessageSentAt: Instant? = null,
    val parent: PID? = null,
    val children: List<PID> = emptyList()
)

/**
 * ProcessStatus represents the status of a process.
 */
enum class ProcessStatus {
    ALIVE,
    STOPPING,
    STOPPED,
    UNKNOWN
}

/**
 * Converts a ProcessInfo to a human-readable string.
 */
fun ProcessInfo.toDetailedString(): String {
    return """
        |PID: ${pid.address}/${pid.id}
        |Status: $status
        |Actor Type: $actorType
        |Mailbox Type: $mailboxType
        |Message Count: $messageCount
        |Created At: $createdAt
        |Last Message Received: ${lastMessageReceivedAt ?: "Never"}
        |Last Message Sent: ${lastMessageSentAt ?: "Never"}
        |Parent: ${parent?.let { "${it.address}/${it.id}" } ?: "None"}
        |Children: ${children.size}
    """.trimMargin()
}
