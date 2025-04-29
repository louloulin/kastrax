package actor.proto.mailbox.priority

import java.util.concurrent.PriorityBlockingQueue
import java.util.concurrent.atomic.AtomicLong

/**
 * 消息优先级接口，用于确定消息的优先级
 */
interface MessagePriority {
    /**
     * 获取消息的优先级
     * 数值越小，优先级越高
     * @return 消息的优先级
     */
    fun getPriority(): Int
}

/**
 * 优先级消息包装器，用于包装普通消息并赋予优先级
 * @param message 原始消息
 * @param priority 消息优先级
 */
data class PriorityMessage(val message: Any, private val priorityValue: Int) : MessagePriority {
    override fun getPriority(): Int = priorityValue
}

/**
 * 优先级队列接口，定义了优先级队列的基本操作
 */
interface PriorityQueue<T> {
    /**
     * 将元素添加到队列中
     * @param item 要添加的元素
     */
    fun enqueue(item: T)

    /**
     * 从队列中取出元素
     * @return 队列中的下一个元素，如果队列为空则返回 null
     */
    fun dequeue(): T?

    /**
     * 获取队列中的元素数量
     * @return 队列中的元素数量
     */
    fun count(): Int

    /**
     * 检查队列是否为空
     * @return 如果队列为空则返回 true，否则返回 false
     */
    fun isEmpty(): Boolean
}

/**
 * 默认优先级队列实现，使用 PriorityBlockingQueue 作为底层存储
 * @param initialCapacity 队列的初始容量
 */
class DefaultPriorityQueue<T : MessagePriority>(initialCapacity: Int = 11) : PriorityQueue<T> {
    // 使用序列号来确保相同优先级的消息按照先进先出的顺序处理
    private val sequenceNumber = AtomicLong(0)

    // 使用 PriorityBlockingQueue 作为底层存储
    private val queue = PriorityBlockingQueue<PriorityQueueItem<T>>(
        initialCapacity,
        Comparator { a, b ->
            // 首先比较优先级
            val priorityComparison = a.priority.compareTo(b.priority)
            if (priorityComparison != 0) {
                priorityComparison
            } else {
                // 如果优先级相同，则比较序列号
                a.sequenceNumber.compareTo(b.sequenceNumber)
            }
        }
    )

    override fun enqueue(item: T) {
        val priority = item.getPriority()
        val sequenceNumber = this.sequenceNumber.getAndIncrement()
        queue.add(PriorityQueueItem(item, priority, sequenceNumber))
    }

    override fun dequeue(): T? {
        val item = queue.poll() ?: return null
        return item.item
    }

    override fun count(): Int = queue.size

    override fun isEmpty(): Boolean = queue.isEmpty()

    /**
     * 优先级队列项，包含元素、优先级和序列号
     * @param item 元素
     * @param priority 优先级
     * @param sequenceNumber 序列号
     */
    private data class PriorityQueueItem<T>(
        val item: T,
        val priority: Int,
        val sequenceNumber: Long
    )
}

/**
 * 创建一个新的默认优先级队列
 * @param initialCapacity 队列的初始容量
 * @return 新的默认优先级队列
 */
fun <T : MessagePriority> newDefaultPriorityQueue(initialCapacity: Int = 11): PriorityQueue<T> {
    return DefaultPriorityQueue(initialCapacity)
}
