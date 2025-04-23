package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MessageRole
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MemoryStatsTest {

    @Test
    fun `test memory stats recording`() = runTest {
        // 创建内存统计对象
        val memoryStats = MemoryStats()

        // 创建测试消息
        val message1 = MemoryMessage(
            id = "1",
            threadId = "thread1",
            message = SimpleMessage(role = MessageRole.USER, content = "Hello"),
            createdAt = Clock.System.now()
        )

        val message2 = MemoryMessage(
            id = "2",
            threadId = "thread1",
            message = SimpleMessage(role = MessageRole.ASSISTANT, content = "Hi there"),
            createdAt = Clock.System.now()
        )

        val message3 = MemoryMessage(
            id = "3",
            threadId = "thread2",
            message = SimpleMessage(role = MessageRole.USER, content = "How are you?"),
            createdAt = Clock.System.now()
        )

        // 记录消息
        memoryStats.recordMessage(message1)
        memoryStats.recordMessage(message2)
        memoryStats.recordMessage(message3)

        // 验证总消息数
        assertEquals(3, memoryStats.getTotalMessages())

        // 验证线程消息数
        assertEquals(2, memoryStats.getThreadMessageCount("thread1"))
        assertEquals(1, memoryStats.getThreadMessageCount("thread2"))

        // 验证角色消息数
        assertEquals(2, memoryStats.getRoleMessageCount("USER"))
        assertEquals(1, memoryStats.getRoleMessageCount("ASSISTANT"))

        // 验证小时消息数
        val hour = formatHour(Clock.System.now())
        assertEquals(3, memoryStats.getHourlyMessageCount(hour))

        // 验证所有线程消息数
        val threadCounts = memoryStats.getAllThreadMessageCounts()
        assertEquals(2, threadCounts["thread1"])
        assertEquals(1, threadCounts["thread2"])

        // 验证所有角色消息数
        val roleCounts = memoryStats.getAllRoleMessageCounts()
        assertEquals(2, roleCounts["USER"])
        assertEquals(1, roleCounts["ASSISTANT"])

        // 验证所有小时消息数
        val hourlyCounts = memoryStats.getAllHourlyMessageCounts()
        assertEquals(3, hourlyCounts[hour])

        // 验证运行时间
        assertTrue(memoryStats.getUptime() >= 0)

        // 测试重置
        memoryStats.reset()
        assertEquals(0, memoryStats.getTotalMessages())
        assertTrue(memoryStats.getAllThreadMessageCounts().isEmpty())
        assertTrue(memoryStats.getAllRoleMessageCounts().isEmpty())
        assertTrue(memoryStats.getAllHourlyMessageCounts().isEmpty())
    }

    /**
     * 格式化小时。
     */
    private fun formatHour(instant: kotlinx.datetime.Instant): String {
        val epochSeconds = instant.epochSeconds
        val epochHours = epochSeconds / 3600
        return epochHours.toString()
    }
}
