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
                content = "Summary of the conversation: This is a test summary."
            ),
            createdAt = Clock.System.now()
        )
        
        // 组合压缩后的消息列表
        return systemMessages + listOf(summaryMessage) + recentMessages
    }
    
    override fun shouldCompress(
        messages: List<MemoryMessage>,
        config: MemoryCompressionConfig
    ): Boolean {
        // 如果压缩功能未启用，不压缩
        if (!config.enabled) {
            return false
        }
        
        // 如果消息数量小于阈值，不压缩
        if (messages.size < config.threshold) {
            return false
        }
        
        return true
    }
    
    override suspend fun summarize(messages: List<MemoryMessage>, maxLength: Int): String {
        return "This is a test summary of the conversation."
    }
}
