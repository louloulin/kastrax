package ai.kastrax.core.memory

import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmToolCall
import ai.kastrax.memory.api.*
import kotlinx.datetime.Instant

/**
 * 适配器类，将 kastrax-memory-api 中的接口适配到 kastrax-core 中使用。
 */
class MemoryAdapter(private val memory: Memory) {
    /**
     * 保存消息到指定的线程。
     */
    suspend fun saveMessage(message: LlmMessage, threadId: String): String {
        return memory.saveMessage(message.toMemoryMessage(), threadId)
    }
    
    /**
     * 获取指定线程的消息。
     */
    suspend fun getMessages(threadId: String, limit: Int = 10): List<LlmMessage> {
        return memory.getMessages(threadId, limit).map { it.message.toLlmMessage() }
    }
    
    /**
     * 搜索指定线程中与查询相关的消息。
     */
    suspend fun searchMessages(query: String, threadId: String, limit: Int = 5): List<LlmMessage> {
        return memory.searchMessages(query, threadId, limit).map { it.message.toLlmMessage() }
    }
    
    /**
     * 创建新的线程。
     */
    suspend fun createThread(title: String? = null): String {
        return memory.createThread(title)
    }
    
    /**
     * 删除指定的线程。
     */
    suspend fun deleteThread(threadId: String): Boolean {
        return memory.deleteThread(threadId)
    }
    
    /**
     * 获取线程信息。
     */
    suspend fun getThread(threadId: String): MemoryThread? {
        return memory.getThread(threadId)
    }
    
    /**
     * 列出所有线程。
     */
    suspend fun listThreads(limit: Int = 20, offset: Int = 0): List<MemoryThread> {
        return memory.listThreads(limit, offset)
    }
    
    /**
     * 将 LlmMessage 转换为 MemoryMessage。
     */
    private fun LlmMessage.toMemoryMessage(): Message {
        return object : Message {
            override val role: MessageRole = when (this@toMemoryMessage.role) {
                LlmMessageRole.SYSTEM -> MessageRole.SYSTEM
                LlmMessageRole.USER -> MessageRole.USER
                LlmMessageRole.ASSISTANT -> MessageRole.ASSISTANT
                LlmMessageRole.TOOL -> MessageRole.TOOL
            }
            override val content: String = this@toMemoryMessage.content
            override val name: String? = this@toMemoryMessage.name
            override val toolCalls: List<ToolCall> = this@toMemoryMessage.toolCalls.map { it.toToolCall() }
            override val toolCallId: String? = this@toMemoryMessage.toolCallId
        }
    }
    
    /**
     * 将 LlmToolCall 转换为 ToolCall。
     */
    private fun LlmToolCall.toToolCall(): ToolCall {
        return object : ToolCall {
            override val id: String = this@toToolCall.id
            override val name: String = this@toToolCall.name
            override val arguments: String = this@toToolCall.arguments
        }
    }
    
    /**
     * 将 Message 转换为 LlmMessage。
     */
    private fun Message.toLlmMessage(): LlmMessage {
        return LlmMessage(
            role = when (this.role) {
                MessageRole.SYSTEM -> LlmMessageRole.SYSTEM
                MessageRole.USER -> LlmMessageRole.USER
                MessageRole.ASSISTANT -> LlmMessageRole.ASSISTANT
                MessageRole.TOOL -> LlmMessageRole.TOOL
            },
            content = this.content,
            name = this.name,
            toolCalls = this.toolCalls.map { it.toLlmToolCall() },
            toolCallId = this.toolCallId
        )
    }
    
    /**
     * 将 ToolCall 转换为 LlmToolCall。
     */
    private fun ToolCall.toLlmToolCall(): LlmToolCall {
        return LlmToolCall(
            id = this.id,
            name = this.name,
            arguments = this.arguments
        )
    }
}
