package ai.kastrax.code.ui

import ai.kastrax.code.model.ChatConversation
import ai.kastrax.code.model.ChatMessage
import ai.kastrax.code.model.MessageRole
import ai.kastrax.code.service.CodeAgentService
import ai.kastrax.code.service.ConversationService
import ai.kastrax.code.ui.components.EnhancedChatPanel
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import javax.swing.JComponent

/**
 * 增强聊天面板测试
 */
class EnhancedChatPanelTest : LightPlatformTestCase() {

    private lateinit var chatPanel: EnhancedChatPanel
    private lateinit var project: Project
    private lateinit var conversation: ChatConversation
    private lateinit var conversationService: ConversationService
    private lateinit var codeAgentService: CodeAgentService

    override fun setUp() {
        super.setUp()
        project = mock(Project::class.java)
        
        // 创建会话
        conversation = ChatConversation(
            id = "test-conversation",
            title = "测试会话",
            messages = mutableListOf(
                ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "欢迎使用 KastraX Code！"
                )
            )
        )
        
        // 模拟服务
        conversationService = mock(ConversationService::class.java)
        codeAgentService = mock(CodeAgentService::class.java)
        
        // 创建聊天面板
        chatPanel = EnhancedChatPanel(project, conversation)
    }

    /**
     * 测试清空消息
     */
    @Test
    fun testClearMessages() {
        // 添加一条消息
        val message = ChatMessage(
            role = MessageRole.USER,
            content = "测试消息"
        )
        conversation.addMessage(message)
        
        // 清空消息
        chatPanel.clearMessages()
        
        // 验证会话消息已清空
        assertEquals("会话消息应为空", 0, conversation.messages.size)
    }

    /**
     * 测试发送用户消息
     */
    @Test
    fun testSendUserMessage() {
        // 发送消息
        chatPanel.sendUserMessage("测试消息")
        
        // 验证会话消息已添加
        assertTrue("会话应包含用户消息", conversation.messages.any { it.role == MessageRole.USER && it.content == "测试消息" })
    }
}
