package ai.kastrax.code.service

import ai.kastrax.code.model.ChatConversation
import ai.kastrax.code.model.ChatMessage
import ai.kastrax.core.common.KastraXBase
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import java.time.Instant
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * 会话服务
 *
 * 管理聊天会话的创建、获取和删除
 */
@Service(Service.Level.PROJECT)
class ConversationService(private val project: Project) : KastraXBase(component = "CONVERSATION_SERVICE", name = "kastrax-code-conversation-service") {

    // 使用父类的logger
    private val conversations = ConcurrentHashMap<String, ChatConversation>()
    private var currentConversationId: String? = null

    /**
     * 创建新会话
     *
     * @param title 会话标题，如果为空则使用默认标题
     * @return 新创建的会话
     */
    fun createConversation(title: String = "新会话 ${UUID.randomUUID().toString().substring(0, 8)}"): ChatConversation {
        val conversation = ChatConversation(
            id = UUID.randomUUID().toString(),
            title = title,
            createdAt = Instant.now(),
            updatedAt = Instant.now()
        )

        conversations[conversation.id] = conversation
        currentConversationId = conversation.id

        logger.debug { "创建新会话: ${conversation.id}" }
        return conversation
    }

    /**
     * 获取会话
     *
     * @param id 会话ID
     * @return 会话，如果不存在则返回null
     */
    fun getConversation(id: String): ChatConversation? {
        return conversations[id]
    }

    /**
     * 获取当前会话
     *
     * @return 当前会话，如果不存在则创建新会话
     */
    fun getCurrentConversation(): ChatConversation {
        val id = currentConversationId
        return if (id != null && conversations.containsKey(id)) {
            conversations[id]!!
        } else {
            createConversation()
        }
    }

    /**
     * 设置当前会话
     *
     * @param id 会话ID
     */
    fun setCurrentConversation(id: String) {
        if (conversations.containsKey(id)) {
            currentConversationId = id
            logger.debug { "设置当前会话: $id" }
        }
    }

    /**
     * 获取所有会话
     *
     * @return 所有会话列表
     */
    fun getAllConversations(): List<ChatConversation> {
        return conversations.values.toList()
    }

    /**
     * 获取排序后的会话列表
     *
     * @return 按更新时间降序排序的会话列表
     */
    fun getSortedConversations(): List<ChatConversation> {
        return conversations.values.sortedByDescending { it.updatedAt }
    }

    /**
     * 删除会话
     *
     * @param id 会话ID
     */
    fun deleteConversation(id: String) {
        conversations.remove(id)

        if (currentConversationId == id) {
            currentConversationId = conversations.keys.firstOrNull()
        }

        logger.debug { "删除会话: $id" }
    }

    /**
     * 删除会话
     *
     * @param conversation 会话
     */
    fun deleteConversation(conversation: ChatConversation) {
        deleteConversation(conversation.id)
    }

    /**
     * 清空所有会话
     */
    fun clearAllConversations() {
        conversations.clear()
        currentConversationId = null
        logger.debug { "清空所有会话" }
    }

    /**
     * 添加消息到当前会话
     *
     * @param message 消息
     */
    fun addMessageToCurrentConversation(message: ChatMessage) {
        val conversation = getCurrentConversation()
        conversation.addMessage(message)
        logger.debug { "添加消息到当前会话: ${conversation.id}" }
    }

    companion object {
        /**
         * 获取服务实例
         *
         * @param project 项目
         * @return 服务实例
         */
        fun getInstance(project: Project): ConversationService {
            return project.service<ConversationService>()
        }
    }
}
