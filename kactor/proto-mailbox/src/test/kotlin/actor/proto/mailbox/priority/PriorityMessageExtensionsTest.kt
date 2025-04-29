package actor.proto.mailbox.priority

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Assertions.*

class PriorityMessageExtensionsTest {

    @BeforeEach
    fun setup() {
    }

    @Test
    fun `should create priority message with custom priority`() {
        val message = "test message"
        val priority = 5

        val priorityMessage = message.withPriority(priority)

        assertEquals(message, priorityMessage.message)
        assertEquals(priority, priorityMessage.getPriority())
    }

    @Test
    fun `should create high priority message`() {
        val message = "test message"

        val priorityMessage = message.withHighPriority()

        assertEquals(message, priorityMessage.message)
        assertEquals(MessagePriorities.HIGH, priorityMessage.getPriority())
    }

    @Test
    fun `should create medium priority message`() {
        val message = "test message"

        val priorityMessage = message.withMediumPriority()

        assertEquals(message, priorityMessage.message)
        assertEquals(MessagePriorities.MEDIUM, priorityMessage.getPriority())
    }

    @Test
    fun `should create low priority message`() {
        val message = "test message"

        val priorityMessage = message.withLowPriority()

        assertEquals(message, priorityMessage.message)
        assertEquals(MessagePriorities.LOW, priorityMessage.getPriority())
    }
}
