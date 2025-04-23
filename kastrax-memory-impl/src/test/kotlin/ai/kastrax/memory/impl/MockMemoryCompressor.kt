package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryCompressor
import ai.kastrax.memory.api.MemoryCompressionConfig
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MessageRole
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * 模拟内存压缩器，用于测试。
 */
class MockMemoryCompressor : MemoryCompressor {
    override suspend fun compress(
        messages: List<MemoryMessage>,
        config: MemoryCompressionConfig
    ): List<MemoryMessage> {
        // 如果不需要压缩，直接返回原始消息
        if (!shouldCompress(messages, config)) {
            return messages
        }

        // 保留系统消息
        val systemMessages = if (config.preserveSystemMessages) {
            messages.filter { it.message.role == MessageRole.SYSTEM }
        } else {
            emptyList()
        }

        // 保留最近的消息
        val recentMessages = if (config.preserveRecentMessages > 0) {
            messages.filter { it.message.role != MessageRole.SYSTEM }
                .takeLast(config.preserveRecentMessages)
        } else {
            emptyList()
        }

        // 创建摘要消息
        val summaryMessage = MemoryMessage(
            id = UUID.randomUUID().toString(),
            threadId = messages.firstOrNull()?.threadId ?: "",
            message = SimpleMessage(
                role = MessageRole.SYSTEM,
                content = "Summary of the conversation with ${messages.size} messages: This is a test summary."
            ),
            createdAt = Clock.System.now()
        )
        println("Created summary message: ${summaryMessage.message.content}")

        // 组合压缩后的消息列表
        return systemMessages + listOf(summaryMessage) + recentMessages
    }

    override fun shouldCompress(
        messages: List<MemoryMessage>,
        config: MemoryCompressionConfig
    ): Boolean {
        // 在测试中，我们总是返回 true，以确保压缩总是被触发
        println("MockMemoryCompressor.shouldCompress: messages.size=${messages.size}, config=$config")
        return true
    }

    override suspend fun summarize(messages: List<MemoryMessage>, maxLength: Int): String {
        return "This is a test summary of the conversation."
    }
}
