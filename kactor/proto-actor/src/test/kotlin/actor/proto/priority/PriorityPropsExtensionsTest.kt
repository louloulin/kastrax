package actor.proto.priority

import actor.proto.Actor
import actor.proto.Context
import actor.proto.Props
import actor.proto.fromProducer
import actor.proto.mailbox.Mailbox
import actor.proto.mailbox.priority.PriorityMailbox
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class PriorityPropsExtensionsTest {
    @Test
    fun `should create props with priority mailbox`() {
        // 创建基本 Props
        val props = Props()

        // 添加优先级邮箱
        val propsWithPriorityMailbox = props.withPriorityMailbox()

        // 验证邮箱生产者
        val mailbox = propsWithPriorityMailbox.mailboxProducer()
        assertTrue(mailbox is PriorityMailbox)
    }

    @Test
    fun `should create props with priority mailbox with custom parameters`() {
        // 创建基本 Props
        val props = Props()

        // 添加自定义参数的优先级邮箱
        val userMailboxSize = 100
        val initialCapacity = 20
        val propsWithPriorityMailbox = props.withPriorityMailbox(userMailboxSize, initialCapacity)

        // 验证邮箱生产者
        val mailbox = propsWithPriorityMailbox.mailboxProducer()
        assertTrue(mailbox is PriorityMailbox)
    }

    @Test
    fun `should create props with priority mailbox from producer`() {
        // 创建带生产者的 Props
        val props = fromProducer { TestActor() }

        // 添加优先级邮箱
        val propsWithPriorityMailbox = props.withPriorityMailbox()

        // 验证邮箱生产者
        val mailbox = propsWithPriorityMailbox.mailboxProducer()
        assertTrue(mailbox is PriorityMailbox)

        // 验证 producer 没有被修改
        assertNotNull(propsWithPriorityMailbox.producer)
    }

    /**
     * 测试用的 Actor 类
     */
    class TestActor : Actor {
        override suspend fun Context.receive(msg: Any) {
            // 空实现
        }
    }
}
