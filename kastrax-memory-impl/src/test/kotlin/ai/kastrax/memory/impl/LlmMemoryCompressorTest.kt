package ai.kastrax.memory.impl

import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmResponse
import ai.kastrax.memory.api.MemoryCompressionConfig
import ai.kastrax.memory.api.MemoryMessage
import ai.kastrax.memory.api.MessageRole
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Clock
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class LlmMemoryCompressorTest {

    // 模拟LLM提供者
    private class MockLlmProvider : LlmProvider {
        override val model: String = "mock-model"

        override suspend fun generate(
            messages: List<LlmMessage>,
            options: LlmOptions
        ): LlmResponse {
            // 简单地返回一个摘要
            return LlmResponse(
                content = "This is a summary of the conversation.",
                finishReason = "stop"
            )
        }

        override suspend fun streamGenerate(
            messages: List<LlmMessage>,
            options: LlmOptions
        ): kotlinx.coroutines.flow.Flow<String> {
            return kotlinx.coroutines.flow.flowOf("This is a summary of the conversation.")
        }

        override suspend fun embedText(text: String): List<Float> {
            // 返回一个简单的嵌入向量
            return List(128) { 0.0f }
        }
    }

    @Test
    fun `test should compress`() = runTest {
        val compressor = LlmMemoryCompressor(MockLlmProvider())

        // 创建配置
        val config = MemoryCompressionConfig(
            enabled = true,
            threshold = 5,
            preserveSystemMessages = true,
            preserveRecentMessages = 2
        )

        // 创建消息列表
        val messages = List(10) { index ->
            MemoryMessage(
                id = "msg$index",
                threadId = "thread1",
                message = SimpleMessage(
                    role = if (index % 2 == 0) MessageRole.USER else MessageRole.ASSISTANT,
                    content = "Message $index"
                ),
                createdAt = Clock.System.now()
            )
        }

        // 测试应该压缩
        val shouldCompress = compressor.shouldCompress(messages, config)
        assertTrue(shouldCompress)

        // 测试不应该压缩的情况
        val smallMessages = messages.take(3)
        val shouldNotCompress = compressor.shouldCompress(smallMessages, config)
        assertFalse(shouldNotCompress)

        // 测试禁用压缩的情况
        val disabledConfig = MemoryCompressionConfig(enabled = false)
        val shouldNotCompressDisabled = compressor.shouldCompress(messages, disabledConfig)
        assertFalse(shouldNotCompressDisabled)
    }

    @Test
    fun `test compress`() = runTest {
        val compressor = LlmMemoryCompressor(MockLlmProvider())

        // 创建配置
        val config = MemoryCompressionConfig(
            enabled = true,
            threshold = 5,
            preserveSystemMessages = true,
            preserveRecentMessages = 2
        )

        // 创建系统消息
        val systemMessage = MemoryMessage(
            id = "system1",
            threadId = "thread1",
            message = SimpleMessage(
                role = MessageRole.SYSTEM,
                content = "System message"
            ),
            createdAt = Clock.System.now()
        )

        // 创建普通消息
        val regularMessages = List(8) { index ->
            MemoryMessage(
                id = "msg$index",
                threadId = "thread1",
                message = SimpleMessage(
                    role = if (index % 2 == 0) MessageRole.USER else MessageRole.ASSISTANT,
                    content = "Message $index"
                ),
                createdAt = Clock.System.now()
            )
        }

        // 合并消息
        val allMessages = listOf(systemMessage) + regularMessages

        // 压缩消息
        val compressedMessages = compressor.compress(allMessages, config)

        // 验证结果
        assertEquals(4, compressedMessages.size) // 1个系统消息 + 1个摘要消息 + 2个最近消息

        // 验证系统消息被保留
        assertTrue(compressedMessages.any { it.message.role == MessageRole.SYSTEM })

        // 验证摘要消息存在
        assertTrue(compressedMessages.any { it.message.content.contains("summary") })

        // 验证最近的消息被保留
        assertTrue(compressedMessages.any { it.id == "msg6" })
        assertTrue(compressedMessages.any { it.id == "msg7" })
    }
}
