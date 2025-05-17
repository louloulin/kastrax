package ai.kastrax.code.model

/**
 * 聊天会话
 *
 * 表示一个聊天会话，包含多个消息
 */
data class ChatConversation(
    val id: String,
    val title: String = "新会话",
    val messages: MutableList<ChatMessage> = mutableListOf()
) {
    /**
     * 添加消息
     *
     * @param message 消息
     */
    fun addMessage(message: ChatMessage) {
        messages.add(message)
    }

    /**
     * 清空消息
     */
    fun clearMessages() {
        messages.clear()
    }

    /**
     * 获取最后一条消息
     *
     * @return 最后一条消息，如果没有则返回null
     */
    fun getLastMessage(): ChatMessage? {
        return messages.lastOrNull()
    }

    /**
     * 获取用户消息
     *
     * @return 用户消息列表
     */
    fun getUserMessages(): List<ChatMessage> {
        return messages.filter { it.role == MessageRole.USER }
    }

    /**
     * 获取助手消息
     *
     * @return 助手消息列表
     */
    fun getAssistantMessages(): List<ChatMessage> {
        return messages.filter { it.role == MessageRole.ASSISTANT }
    }
}
