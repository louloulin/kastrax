package actor.proto.priority

import actor.proto.ActorSystem
import actor.proto.PID
import actor.proto.mailbox.priority.MessagePriorities
import actor.proto.mailbox.priority.PriorityMessage
import actor.proto.mailbox.priority.withHighPriority
import actor.proto.mailbox.priority.withLowPriority
import actor.proto.mailbox.priority.withMediumPriority
import actor.proto.mailbox.priority.withPriority
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.mockito.Mockito.*

@Disabled("This test needs to be fixed")
class PriorityActorSystemExtensionsTest {
    private lateinit var system: ActorSystem
    private lateinit var pid: PID

    @BeforeEach
    fun setup() {
        // 创建模拟对象
        system = mock(ActorSystem::class.java)
        pid = mock(PID::class.java)
    }

    @Test
    fun `should send message with custom priority`() {
        val message = "test message"
        val priority = 5

        system.sendWithPriority(pid, message, priority)

        // 验证发送了正确的优先级消息
        val expectedMessage = message.withPriority(priority)
        verify(system).send(pid, expectedMessage)
    }

    @Test
    fun `should send message with high priority`() {
        val message = "test message"

        system.sendWithHighPriority(pid, message)

        // 验证发送了高优先级消息
        val expectedMessage = message.withHighPriority()
        verify(system).send(pid, expectedMessage)
    }

    @Test
    fun `should send message with medium priority`() {
        val message = "test message"

        system.sendWithMediumPriority(pid, message)

        // 验证发送了中优先级消息
        val expectedMessage = message.withMediumPriority()
        verify(system).send(pid, expectedMessage)
    }

    @Test
    fun `should send message with low priority`() {
        val message = "test message"

        system.sendWithLowPriority(pid, message)

        // 验证发送了低优先级消息
        val expectedMessage = message.withLowPriority()
        verify(system).send(pid, expectedMessage)
    }
}
