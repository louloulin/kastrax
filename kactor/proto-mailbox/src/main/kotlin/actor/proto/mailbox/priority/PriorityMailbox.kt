package actor.proto.mailbox.priority

import actor.proto.mailbox.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger

/**
 * 优先级邮箱，根据消息优先级处理消息
 * @param userMailboxSize 用户邮箱的大小，0 表示无限制
 * @param initialCapacity 优先级队列的初始容量
 * @param stats 邮箱统计信息
 */
class PriorityMailbox(
    private val userMailboxSize: Int = 0,
    private val initialCapacity: Int = 11,
    private val stats: Array<MailboxStatistics> = emptyArray()
) : Mailbox {
    // 系统消息队列
    private val systemMessages = ConcurrentLinkedQueue<Any>()

    // 用户消息队列
    private val userMessages = ConcurrentLinkedQueue<Any>()

    // 优先级消息队列
    private val priorityMessages: PriorityQueue<MessagePriority> = newDefaultPriorityQueue(initialCapacity)

    // 运行状态
    private val status = AtomicInteger(MailboxStatus.IDLE)

    // 处理器
    private lateinit var dispatcher: Dispatcher
    private lateinit var invoker: MessageInvoker

    // 暂停状态
    private var suspended = false

    override fun postUserMessage(message: Any) {
        for (stat in stats) {
            stat.messagePosted(message)
        }

        when (message) {
            is MessagePriority -> {
                priorityMessages.enqueue(message)
            }
            else -> {
                userMessages.add(message)
            }
        }

        schedule()
    }

    override fun postSystemMessage(message: Any) {
        for (stat in stats) {
            stat.messagePosted(message)
        }

        systemMessages.add(message)
        schedule()
    }

    override fun registerHandlers(invoker: MessageInvoker, dispatcher: Dispatcher) {
        this.invoker = invoker
        this.dispatcher = dispatcher
    }

    override fun start() {
        for (stat in stats) {
            stat.mailboxStarted()
        }
    }

    private fun schedule() {
        val previousStatus = status.getAndSet(MailboxStatus.BUSY)
        if (previousStatus == MailboxStatus.IDLE) {
            dispatcher.schedule(this)
        }
    }

    override suspend fun run() {
        processMessages()
    }

    private suspend fun processMessages() {
        var messageCount = 0

        try {
            // 处理系统消息
            var sys = systemMessages.poll()
            while (sys != null) {
                messageCount++

                for (stat in stats) {
                    stat.messageReceived(sys)
                }

                when (sys) {
                    is SuspendMailbox -> suspended = true
                    is ResumeMailbox -> suspended = false
                }

                invoker.invokeSystemMessage(sys as SystemMessage)

                if (messageCount >= dispatcher.throughput) {
                    break
                }

                sys = systemMessages.poll()
            }

            // 如果邮箱被暂停，则不处理用户消息
            if (suspended) {
                status.set(MailboxStatus.IDLE)
                schedule()
                return
            }

            // 处理优先级消息
            var priorityMsg = priorityMessages.dequeue()
            while (priorityMsg != null && messageCount < dispatcher.throughput) {
                messageCount++

                for (stat in stats) {
                    stat.messageReceived(priorityMsg)
                }

                invoker.invokeUserMessage(
                    when (priorityMsg) {
                        is PriorityMessage -> priorityMsg.message
                        else -> priorityMsg
                    }
                )

                if (messageCount >= dispatcher.throughput) {
                    break
                }

                priorityMsg = priorityMessages.dequeue()
            }

            // 处理普通用户消息
            var userMsg = userMessages.poll()
            while (userMsg != null && messageCount < dispatcher.throughput) {
                messageCount++

                for (stat in stats) {
                    stat.messageReceived(userMsg)
                }

                invoker.invokeUserMessage(userMsg)

                if (messageCount >= dispatcher.throughput) {
                    break
                }

                userMsg = userMessages.poll()
            }

            // 检查是否还有消息需要处理
            if (systemMessages.isEmpty() && userMessages.isEmpty() && priorityMessages.isEmpty()) {
                for (stat in stats) {
                    stat.mailboxEmpty()
                }

                status.set(MailboxStatus.IDLE)

                // 再次检查是否有新消息到达
                if (!systemMessages.isEmpty() || !userMessages.isEmpty() || !priorityMessages.isEmpty()) {
                    schedule()
                }
            } else {
                schedule()
            }
        } catch (e: Exception) {
            status.set(MailboxStatus.IDLE)
            throw e
        }
    }
}

/**
 * 创建一个新的优先级邮箱
 * @param userMailboxSize 用户邮箱的大小，0 表示无限制
 * @param initialCapacity 优先级队列的初始容量
 * @param stats 邮箱统计信息
 * @return 新的优先级邮箱
 */
fun newPriorityMailbox(
    userMailboxSize: Int = 0,
    initialCapacity: Int = 11,
    stats: Array<MailboxStatistics> = emptyArray()
): Mailbox {
    return PriorityMailbox(userMailboxSize, initialCapacity, stats)
}

/**
 * 创建一个新的优先级邮箱生产者
 * @param userMailboxSize 用户邮箱的大小，0 表示无限制
 * @param initialCapacity 优先级队列的初始容量
 * @return 新的优先级邮箱生产者
 */
fun newPriorityMailboxProducer(
    userMailboxSize: Int = 0,
    initialCapacity: Int = 11
): () -> Mailbox {
    return { newPriorityMailbox(userMailboxSize, initialCapacity) }
}
