package ai.kastrax.memory.impl

import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MemoryProcessor
import ai.kastrax.memory.api.MemoryProcessorOptions
import ai.kastrax.memory.api.MessageRole

/**
 * 模拟令牌限制器，用于测试。
 */
class TokenLimiter(private val maxTokens: Int) : MemoryProcessor {
    override suspend fun process(
        messages: List<MemoryMessage>,
        options: MemoryProcessorOptions
    ): List<MemoryMessage> {
        // 简单地返回前 maxTokens 个消息
        return messages.take(maxTokens / 10) // 假设每条消息有10个令牌
    }
}

/**
 * 模拟工具调用过滤器，用于测试。
 */
class ToolCallFilter : MemoryProcessor {
    override suspend fun process(
        messages: List<MemoryMessage>,
        options: MemoryProcessorOptions
    ): List<MemoryMessage> {
        // 过滤掉工具调用和工具调用结果
        return messages.filter { message ->
            message.message.toolCalls.isEmpty() && message.message.toolCallId == null
        }
    }
}
