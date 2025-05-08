package ai.kastrax.codex.memory

import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.memory.api.Memory
import ai.kastrax.memory.api.MemoryBuilder
import ai.kastrax.memory.api.MemoryItem
import ai.kastrax.memory.api.Message
import ai.kastrax.memory.api.MessageRole
import com.intellij.openapi.diagnostic.Logger
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.util.UUID

/**
 * CodexMemoryManager 负责管理 CodexAgent 的记忆系统
 */
class CodexMemoryManager(
    private val projectId: String
) {
    private val logger = Logger.getInstance(CodexMemoryManager::class.java)
    
    // 对话记忆
    private val conversationMemory: Memory by lazy {
        MemoryBuilder.createConversationMemory(
            id = "codex-conversation-$projectId",
            capacity = 100
        )
    }
    
    // 代码上下文记忆
    private val codeContextMemory: Memory by lazy {
        MemoryBuilder.createConversationMemory(
            id = "codex-code-context-$projectId",
            capacity = 50
        )
    }
    
    /**
     * 获取对话记忆
     */
    fun getConversationMemory(): Memory = conversationMemory
    
    /**
     * 获取代码上下文记忆
     */
    fun getCodeContextMemory(): Memory = codeContextMemory
    
    /**
     * 保存对话消息
     */
    suspend fun saveConversation(message: LlmMessage, threadId: String) {
        try {
            val role = when (message.role) {
                LlmMessageRole.USER -> MessageRole.USER
                LlmMessageRole.ASSISTANT -> MessageRole.ASSISTANT
                LlmMessageRole.SYSTEM -> MessageRole.SYSTEM
                LlmMessageRole.TOOL -> MessageRole.TOOL
            }
            
            val memoryMessage = Message(
                role = role,
                content = message.content
            )
            
            conversationMemory.saveMessage(
                message = memoryMessage,
                threadId = threadId,
                metadata = mapOf(
                    "timestamp" to System.currentTimeMillis().toString(),
                    "project_id" to projectId
                )
            )
        } catch (e: Exception) {
            logger.error("Error saving conversation", e)
        }
    }
    
    /**
     * 保存代码上下文
     */
    suspend fun saveCodeContext(code: String, filePath: String, language: String) {
        try {
            val threadId = UUID.randomUUID().toString()
            
            val memoryMessage = Message(
                role = MessageRole.SYSTEM,
                content = code
            )
            
            codeContextMemory.saveMessage(
                message = memoryMessage,
                threadId = threadId,
                metadata = mapOf(
                    "file_path" to filePath,
                    "language" to language,
                    "timestamp" to System.currentTimeMillis().toString(),
                    "project_id" to projectId
                )
            )
        } catch (e: Exception) {
            logger.error("Error saving code context", e)
        }
    }
    
    /**
     * 检索相关记忆
     */
    suspend fun retrieveRelevantMemories(query: String, limit: Int = 5): List<MemoryItem> {
        try {
            // 从对话记忆中检索
            val conversationItems = conversationMemory.getMessages(limit = limit)
            
            // 从代码上下文记忆中检索
            val codeItems = codeContextMemory.getMessages(limit = limit)
            
            // 合并并按时间戳排序（最新的优先）
            return (conversationItems + codeItems)
                .sortedByDescending { 
                    it.metadata["timestamp"]?.toLongOrNull() ?: 0L 
                }
                .take(limit)
        } catch (e: Exception) {
            logger.error("Error retrieving memories", e)
            return emptyList()
        }
    }
    
    /**
     * 清除记忆
     */
    suspend fun clearMemories() {
        try {
            // 清除所有对话记忆
            val conversationThreads = conversationMemory.getThreads()
            conversationThreads.forEach { thread ->
                conversationMemory.deleteThread(thread.id)
            }
            
            // 清除所有代码上下文记忆
            val codeContextThreads = codeContextMemory.getThreads()
            codeContextThreads.forEach { thread ->
                codeContextMemory.deleteThread(thread.id)
            }
        } catch (e: Exception) {
            logger.error("Error clearing memories", e)
        }
    }
}
