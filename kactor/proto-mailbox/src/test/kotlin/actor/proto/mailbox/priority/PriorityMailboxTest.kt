package actor.proto.mailbox.priority

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

/**
 * 简单的测试类，用于验证 PriorityMailbox 的基本功能
 */
class PriorityMailboxTest {
    @Test
    fun `should create priority mailbox`() {
        val mailbox = newPriorityMailbox()
        assertNotNull(mailbox)
    }
}
