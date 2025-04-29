package actor.proto.mailbox.priority

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class PriorityQueueTest {
    private lateinit var queue: PriorityQueue<MessagePriority>

    @BeforeEach
    fun setup() {
        queue = newDefaultPriorityQueue()
    }

    @Test
    fun `should enqueue and dequeue items in priority order`() {
        // 添加不同优先级的消息
        queue.enqueue(PriorityMessage("low", 2))
        queue.enqueue(PriorityMessage("medium", 1))
        queue.enqueue(PriorityMessage("high", 0))

        // 验证消息按优先级顺序出队
        val high = queue.dequeue() as PriorityMessage
        assertEquals("high", high.message)
        assertEquals(0, high.getPriority())

        val medium = queue.dequeue() as PriorityMessage
        assertEquals("medium", medium.message)
        assertEquals(1, medium.getPriority())

        val low = queue.dequeue() as PriorityMessage
        assertEquals("low", low.message)
        assertEquals(2, low.getPriority())

        // 验证队列为空
        assertNull(queue.dequeue())
        assertTrue(queue.isEmpty())
    }

    @Test
    fun `should maintain FIFO order for same priority items`() {
        // 添加相同优先级的消息
        queue.enqueue(PriorityMessage("first", 1))
        queue.enqueue(PriorityMessage("second", 1))
        queue.enqueue(PriorityMessage("third", 1))

        // 验证消息按添加顺序出队
        val first = queue.dequeue() as PriorityMessage
        assertEquals("first", first.message)

        val second = queue.dequeue() as PriorityMessage
        assertEquals("second", second.message)

        val third = queue.dequeue() as PriorityMessage
        assertEquals("third", third.message)
    }

    @Test
    fun `should handle mixed priority items correctly`() {
        // 添加混合优先级的消息
        queue.enqueue(PriorityMessage("low1", 2))
        queue.enqueue(PriorityMessage("high1", 0))
        queue.enqueue(PriorityMessage("medium1", 1))
        queue.enqueue(PriorityMessage("high2", 0))
        queue.enqueue(PriorityMessage("low2", 2))
        queue.enqueue(PriorityMessage("medium2", 1))

        // 验证消息按优先级和添加顺序出队
        val high1 = queue.dequeue() as PriorityMessage
        assertEquals("high1", high1.message)

        val high2 = queue.dequeue() as PriorityMessage
        assertEquals("high2", high2.message)

        val medium1 = queue.dequeue() as PriorityMessage
        assertEquals("medium1", medium1.message)

        val medium2 = queue.dequeue() as PriorityMessage
        assertEquals("medium2", medium2.message)

        val low1 = queue.dequeue() as PriorityMessage
        assertEquals("low1", low1.message)

        val low2 = queue.dequeue() as PriorityMessage
        assertEquals("low2", low2.message)
    }

    @Test
    fun `should return correct count`() {
        assertEquals(0, queue.count())

        queue.enqueue(PriorityMessage("message1", 1))
        assertEquals(1, queue.count())

        queue.enqueue(PriorityMessage("message2", 2))
        assertEquals(2, queue.count())

        queue.dequeue()
        assertEquals(1, queue.count())

        queue.dequeue()
        assertEquals(0, queue.count())
    }

    @Test
    fun `should check if queue is empty`() {
        assertTrue(queue.isEmpty())

        queue.enqueue(PriorityMessage("message", 1))
        assertFalse(queue.isEmpty())

        queue.dequeue()
        assertTrue(queue.isEmpty())
    }

    @Test
    fun `should handle custom message priority implementation`() {
        // 自定义实现 MessagePriority 接口的类
        class CustomPriorityMessage(val content: String, private val priority: Int) : MessagePriority {
            override fun getPriority(): Int = priority
        }

        // 创建自定义消息
        val high = CustomPriorityMessage("high", 0)
        val medium = CustomPriorityMessage("medium", 1)
        val low = CustomPriorityMessage("low", 2)

        // 添加自定义消息
        queue.enqueue(low)
        queue.enqueue(high)
        queue.enqueue(medium)

        // 验证消息按优先级顺序出队
        val highOut = queue.dequeue() as CustomPriorityMessage
        assertEquals("high", highOut.content)

        val mediumOut = queue.dequeue() as CustomPriorityMessage
        assertEquals("medium", mediumOut.content)

        val lowOut = queue.dequeue() as CustomPriorityMessage
        assertEquals("low", lowOut.content)
    }
}
