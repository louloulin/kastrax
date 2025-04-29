package actor.proto.mailbox

import org.jctools.queues.MpscArrayQueue
import org.jctools.queues.MpscGrowableArrayQueue
import org.jctools.queues.MpscLinkedQueue
import org.jctools.queues.MpscUnboundedArrayQueue
import org.jctools.queues.atomic.MpscAtomicArrayQueue
import org.jctools.queues.atomic.MpscGrowableAtomicArrayQueue
import org.jctools.queues.atomic.MpscUnboundedAtomicArrayQueue
import java.util.concurrent.ConcurrentLinkedQueue

private val emptyStats : Array<MailboxStatistics> = arrayOf()
fun newUnboundedMailbox(stats: Array<MailboxStatistics> = emptyStats): Mailbox = DefaultMailbox(ConcurrentLinkedQueue<Any>(), ConcurrentLinkedQueue<Any>(), stats)

fun newMpscAtomicArrayMailbox(capacity: Int = 1000, stats: Array<MailboxStatistics> = emptyStats): Mailbox = DefaultMailbox(ConcurrentLinkedQueue<Any>(), MpscAtomicArrayQueue(capacity), stats)
fun newMpscArrayMailbox(capacity: Int = 1000, stats: Array<MailboxStatistics> = emptyStats): Mailbox = DefaultMailbox(ConcurrentLinkedQueue<Any>(), MpscArrayQueue(capacity), stats)

fun newMpscUnboundedArrayMailbox(chunkSize: Int = 5, stats: Array<MailboxStatistics> = emptyStats): Mailbox = DefaultMailbox(ConcurrentLinkedQueue<Any>(), MpscUnboundedArrayQueue(chunkSize), stats)
fun newMpscUnboundedAtomicArrayMailbox(chunkSize: Int = 5, stats: Array<MailboxStatistics> = emptyStats): Mailbox = DefaultMailbox(ConcurrentLinkedQueue<Any>(), MpscUnboundedAtomicArrayQueue(chunkSize), stats)


fun newMpscGrowableArrayMailbox(initialCapacity: Int = 5, stats: Array<MailboxStatistics> = emptyStats): Mailbox = DefaultMailbox(ConcurrentLinkedQueue<Any>(), MpscGrowableArrayQueue(initialCapacity, 2 shl 11), stats)
fun newMpscGrowableAtomicArrayMailbox(initialCapacity: Int = 5, stats: Array<MailboxStatistics> = emptyStats): Mailbox = DefaultMailbox(ConcurrentLinkedQueue<Any>(), MpscGrowableAtomicArrayQueue(initialCapacity, 2 shl 11), stats)


fun newMpscLinkedMailbox(stats: Array<MailboxStatistics> = emptyStats): Mailbox = DefaultMailbox(ConcurrentLinkedQueue<Any>(), MpscLinkedQueue(), stats)

// Removed the newSpecifiedMailbox function as ConcurrentQueueSpec and newQueue are no longer available in the latest JCTools version
