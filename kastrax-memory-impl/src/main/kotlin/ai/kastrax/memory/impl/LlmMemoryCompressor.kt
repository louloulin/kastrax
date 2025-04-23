package ai.kastrax.memory.impl

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmResponse
import ai.kastrax.memory.api.MemoryCompressor
import ai.kastrax.memory.api.MemoryCompressionConfig
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.MessageRole
import ai.kastrax.memory.api.ToolCall
import kotlinx.datetime.Clock
import java.util.UUID

/**
 * 使用LLM的内存压缩器实现。
 *
 * @property llm LLM实例
 */
class LlmMemoryCompressor(
    private val llm: LlmProvider
) : MemoryCompressor, KastraXBase(component = "MEMORY_COMPRESSOR", name = "llm") {

    override suspend fun compress(
        messages: List<MemoryMessage>,
        config: MemoryCompressionConfig
    ): List<MemoryMessage> {
        // 如果不需要压缩，直接返回原始消息
        if (!shouldCompress(messages, config)) {
            return messages
        }

        // 分类消息
        val (systemMessages, nonSystemMessages) = partitionMessages(messages, config)
        val (messagesToCompress, recentMessages) = splitRecentMessages(nonSystemMessages, config.preserveRecentMessages)

        // 如果没有需要压缩的消息，直接返回原始消息
        val compressedMessages = if (messagesToCompress.isEmpty()) {
            messages
        } else {
            // 生成摘要并创建摘要消息
            val summaryMessage = createSummaryMessage(messagesToCompress, messages.firstOrNull()?.threadId ?: "")

            // 组合压缩后的消息列表
            systemMessages + listOf(summaryMessage) + recentMessages
        }

        return compressedMessages
    }

    /**
     * 分离系统消息和非系统消息。
     */
    private fun partitionMessages(
        messages: List<MemoryMessage>,
        config: MemoryCompressionConfig
    ): Pair<List<MemoryMessage>, List<MemoryMessage>> {
        val systemMessages = if (config.preserveSystemMessages) {
            messages.filter { it.message.role == MessageRole.SYSTEM }
        } else {
            emptyList()
        }

        val nonSystemMessages = messages.filter { it.message.role != MessageRole.SYSTEM }

        return Pair(systemMessages, nonSystemMessages)
    }

    /**
     * 分离需要压缩的消息和最近的消息。
     */
    private fun splitRecentMessages(
        messages: List<MemoryMessage>,
        preserveRecentCount: Int
    ): Pair<List<MemoryMessage>, List<MemoryMessage>> {
        val recentMessages = if (preserveRecentCount > 0) {
            messages.takeLast(preserveRecentCount)
        } else {
            emptyList()
        }

        val messagesToCompress = messages.dropLast(preserveRecentCount)

        return Pair(messagesToCompress, recentMessages)
    }

    /**
     * 创建摘要消息。
     */
    private suspend fun createSummaryMessage(
        messagesToCompress: List<MemoryMessage>,
        threadId: String
    ): MemoryMessage {
        // 生成摘要
        val summary = summarize(messagesToCompress)

        // 创建摘要消息
        val summaryMessage = object : Message {
            override val role = MessageRole.SYSTEM
            override val content = """
                以下是之前对话的摘要:

                $summary
            """.trimIndent()
            override val name = "conversation_summary"
            override val toolCalls = emptyList<ToolCall>()
            override val toolCallId = null
        }

        return MemoryMessage(
            id = UUID.randomUUID().toString(),
            threadId = threadId,
            message = summaryMessage,
            createdAt = Clock.System.now()
        )
    }
    override fun shouldCompress(
        messages: List<MemoryMessage>,
        config: MemoryCompressionConfig
    ): Boolean {
        // 检查是否需要压缩
        val isEnabled = config.enabled
        val hasEnoughMessages = messages.size >= config.threshold

        return isEnabled && hasEnoughMessages
    }

    override suspend fun summarize(messages: List<MemoryMessage>, maxLength: Int): String {
        // 构建对话文本
        val conversationText = messages.joinToString("\n\n") { memoryMessage ->
            val message = memoryMessage.message
            val role = when (message.role) {
                MessageRole.USER -> "用户"
                MessageRole.ASSISTANT -> "助手"
                MessageRole.SYSTEM -> "系统"
                MessageRole.TOOL -> "工具"
                else -> message.role.toString()
            }

            "$role: ${message.content}"
        }

        // 构建提示
        val prompt = """
            请对以下对话进行摘要，提取关键信息和重要细节。摘要应该简洁明了，不超过${maxLength}个字符。

            对话内容:
            $conversationText

            摘要:
        """.trimIndent()

        // 调用LLM生成摘要
        try {
            // 将消息转换为LlmMessage
            val llmMessage = LlmMessage(
                role = LlmMessageRole.USER,
                content = prompt
            )

            val response = llm.generate(listOf(llmMessage), LlmOptions())
            return response.content.trim()
        } catch (e: Exception) {
            this.logger.error("生成对话摘要失败: ${e.message}")
            return "无法生成摘要: ${e.message}"
        }
    }
}
