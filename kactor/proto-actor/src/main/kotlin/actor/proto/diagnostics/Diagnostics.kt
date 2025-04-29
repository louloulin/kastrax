package actor.proto.diagnostics

import actor.proto.ActorSystem
import actor.proto.LocalProcess
import actor.proto.PID
import actor.proto.Process
import actor.proto.ProcessRegistry
import actor.proto.mailbox.DefaultMailbox
import actor.proto.mailbox.Mailbox
import actor.proto.mailbox.MailboxStatus
import java.lang.reflect.Field
import java.time.Instant
import java.util.regex.Pattern

/**
 * Diagnostics provides methods for getting diagnostic information about processes.
 */
class Diagnostics(private val system: ActorSystem) {
    // Cache for reflection fields to improve performance
    private val mailboxField: Field? by lazy {
        try {
            val field = LocalProcess::class.java.getDeclaredField("mailbox")
            field.isAccessible = true
            field
        } catch (e: Exception) {
            null
        }
    }

    private val userCountField: Field? by lazy {
        try {
            val field = DefaultMailbox::class.java.getDeclaredField("userCount")
            field.isAccessible = true
            field
        } catch (e: Exception) {
            null
        }
    }

    private val sysCountField: Field? by lazy {
        try {
            val field = DefaultMailbox::class.java.getDeclaredField("sysCount")
            field.isAccessible = true
            field
        } catch (e: Exception) {
            null
        }
    }

    private val statusField: Field? by lazy {
        try {
            val field = DefaultMailbox::class.java.getDeclaredField("status")
            field.isAccessible = true
            field
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Get information about a process.
     * @param pid The PID of the process
     * @return The process information
     */
    fun getProcessInfo(pid: PID): ProcessInfo {
        val process = system.processRegistry().get(pid)
        return when (process) {
            is LocalProcess -> {
                val mailbox = getMailbox(process)
                val messageCount = getMessageCount(mailbox)
                val status = getProcessStatus(process, mailbox)
                val actorType = getActorType(process)

                ProcessInfo(
                    pid = pid,
                    mailboxType = mailbox?.javaClass?.simpleName ?: "Unknown",
                    messageCount = messageCount,
                    status = status,
                    actorType = actorType,
                    createdAt = Instant.now(), // We don't have creation time yet, so use now
                    lastMessageReceivedAt = null, // We don't track this yet
                    lastMessageSentAt = null, // We don't track this yet
                    parent = null, // We don't track parent-child relationships yet
                    children = emptyList() // We don't track parent-child relationships yet
                )
            }
            else -> ProcessInfo(
                pid = pid,
                mailboxType = "Unknown",
                messageCount = 0,
                status = ProcessStatus.UNKNOWN
            )
        }
    }

    /**
     * List processes matching a pattern.
     * @param pattern The pattern to match
     * @param matchType The type of matching to perform
     * @return The list of PIDs matching the pattern
     */
    fun listProcesses(pattern: String, matchType: MatchType): List<PID> {
        val regex = when (matchType) {
            MatchType.MATCH_PART_OF_STRING -> Pattern.compile(".*$pattern.*")
            MatchType.MATCH_EXACT_STRING -> Pattern.compile("^$pattern$")
            MatchType.MATCH_REGEX -> Pattern.compile(pattern)
        }

        return system.processRegistry().processes()
            .filter { regex.matcher(it.id).matches() }
            .toList()
    }

    /**
     * Get all processes in the system.
     * @return A list of all PIDs in the system
     */
    fun getAllProcesses(): List<PID> {
        return system.processRegistry().processes().toList()
    }

    /**
     * Get detailed information about all processes matching a pattern.
     * @param pattern The pattern to match
     * @param matchType The type of matching to perform
     * @return A list of ProcessInfo objects for all matching processes
     */
    fun getProcessInfos(pattern: String, matchType: MatchType): List<ProcessInfo> {
        return listProcesses(pattern, matchType).map { getProcessInfo(it) }
    }

    /**
     * Get detailed information about all processes in the system.
     * @return A list of ProcessInfo objects for all processes
     */
    fun getAllProcessInfos(): List<ProcessInfo> {
        return getAllProcesses().map { getProcessInfo(it) }
    }

    private fun getMailbox(process: LocalProcess): Mailbox? {
        return try {
            mailboxField?.get(process) as? Mailbox
        } catch (e: Exception) {
            null
        }
    }

    private fun getMessageCount(mailbox: Mailbox?): Int {
        if (mailbox !is DefaultMailbox) return 0

        return try {
            val userCount = userCountField?.get(mailbox) as? java.util.concurrent.atomic.AtomicInteger
            val sysCount = sysCountField?.get(mailbox) as? java.util.concurrent.atomic.AtomicInteger
            (userCount?.get() ?: 0) + (sysCount?.get() ?: 0)
        } catch (e: Exception) {
            0
        }
    }

    private fun getProcessStatus(process: Process, mailbox: Mailbox?): ProcessStatus {
        if (mailbox !is DefaultMailbox) return ProcessStatus.UNKNOWN

        return try {
            val status = statusField?.get(mailbox) as? java.util.concurrent.atomic.AtomicInteger
            when (status?.get()) {
                MailboxStatus.BUSY -> ProcessStatus.ALIVE
                MailboxStatus.IDLE -> ProcessStatus.ALIVE
                else -> ProcessStatus.UNKNOWN
            }
        } catch (e: Exception) {
            ProcessStatus.UNKNOWN
        }
    }

    private fun getActorType(process: Process): String {
        // This is a simplified implementation
        // In a real implementation, we would need to access the actor type from the process
        return process.javaClass.simpleName
    }
}

/**
 * MatchType represents the type of matching to perform when listing processes.
 */
enum class MatchType {
    MATCH_PART_OF_STRING,
    MATCH_EXACT_STRING,
    MATCH_REGEX
}
