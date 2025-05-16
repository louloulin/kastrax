package ai.kastrax.code.model

import java.time.Instant
import java.util.UUID

/**
 * 聊天消息角色
 */
enum class MessageRole {
    USER,
    ASSISTANT,
    SYSTEM
}

/**
 * 聊天消息
 * 
 * 表示聊天中的一条消息
 */
data class ChatMessage(
    /**
     * 消息ID
     */
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * 消息角色
     */
    val role: MessageRole,
    
    /**
     * 消息内容
     */
    val content: String,
    
    /**
     * 消息创建时间
     */
    val timestamp: Instant = Instant.now(),
    
    /**
     * 消息元数据
     */
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 聊天会话
 * 
 * 表示一个完整的聊天会话，包含多条消息
 */
data class ChatConversation(
    /**
     * 会话ID
     */
    val id: String = UUID.randomUUID().toString(),
    
    /**
     * 会话标题
     */
    val title: String,
    
    /**
     * 会话消息列表
     */
    val messages: MutableList<ChatMessage> = mutableListOf(),
    
    /**
     * 会话创建时间
     */
    val createdAt: Instant = Instant.now(),
    
    /**
     * 会话更新时间
     */
    var updatedAt: Instant = Instant.now(),
    
    /**
     * 会话元数据
     */
    val metadata: MutableMap<String, Any> = mutableMapOf()
) {
    /**
     * 添加消息
     */
    fun addMessage(message: ChatMessage) {
        messages.add(message)
        updatedAt = Instant.now()
    }
    
    /**
     * 清空消息
     */
    fun clearMessages() {
        messages.clear()
        updatedAt = Instant.now()
    }
}
