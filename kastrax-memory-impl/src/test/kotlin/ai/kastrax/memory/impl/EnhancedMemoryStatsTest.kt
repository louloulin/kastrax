package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MessageRole
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class EnhancedMemoryStatsTest {

    @Test
    fun `test memory stats in enhanced memory`() = runTest {
        // 创建增强型内存
        val memory = createTestMemory {
            storage(InMemoryStorage())
            lastMessages(10)
        }

        // 创建线程
        val threadId = memory.createThread("Test Thread")

        // 保存消息
        memory.saveMessage(
            SimpleMessage(role = MessageRole.USER, content = "Hello"),
            threadId
        )

        memory.saveMessage(
            SimpleMessage(role = MessageRole.ASSISTANT, content = "Hi there"),
            threadId
        )

        memory.saveMessage(
            SimpleMessage(role = MessageRole.USER, content = "How are you?"),
            threadId
        )

        // 获取内存统计信息
        val stats = (memory as EnhancedMemory).getMemoryStats()

        // 验证总消息数
        assertEquals(3L, stats["totalMessages"])

        // 验证线程消息数
        val threadCounts = stats["threadCounts"] as Map<*, *>
        assertEquals(3L, threadCounts[threadId])

        // 验证角色消息数
        val roleCounts = stats["roleCounts"] as Map<*, *>
        assertEquals(2L, roleCounts["USER"])
        assertEquals(1L, roleCounts["ASSISTANT"])

        // 验证小时消息数
        val hourlyCounts = stats["hourlyCounts"] as Map<*, *>
        assertTrue(hourlyCounts.isNotEmpty())

        // 验证运行时间
        assertTrue((stats["uptime"] as Long) >= 0)
    }
}
