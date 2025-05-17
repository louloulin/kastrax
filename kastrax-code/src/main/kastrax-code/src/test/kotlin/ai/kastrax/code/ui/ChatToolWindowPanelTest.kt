package ai.kastrax.code.ui

import ai.kastrax.code.model.ChatMessage
import ai.kastrax.code.model.MessageRole
import ai.kastrax.code.service.ConversationService
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import javax.swing.JComponent

/**
 * 聊天工具窗口面板测试
 */
class ChatToolWindowPanelTest : LightPlatformTestCase() {

    private lateinit var chatToolWindowPanel: ChatToolWindowPanel
    private lateinit var project: Project
    private lateinit var conversationService: ConversationService

    override fun setUp() {
        super.setUp()
        project = mock(Project::class.java)
        conversationService = mock(ConversationService::class.java)
        
        // 模拟ConversationService.getInstance方法
        val mockConversation = ConversationService.getInstance(project)
        `when`(mockConversation.createConversation()).thenReturn(
            ai.kastrax.code.model.ChatConversation(
                id = "test-conversation",
                messages = mutableListOf(
                    ChatMessage(
                        role = MessageRole.ASSISTANT,
                        content = "欢迎使用 KastraX Code！"
                    )
                )
            )
        )
        
        chatToolWindowPanel = ChatToolWindowPanel(project, this)
    }

    /**
     * 测试添加新的聊天标签页
     */
    @Test
    fun testAddNewChatTab() {
        val chatPanel = chatToolWindowPanel.addNewChatTab()
        
        assertNotNull("聊天面板不应为空", chatPanel)
        assertEquals("标签页数量应为1", 1, chatToolWindowPanel.getTabCount())
    }

    /**
     * 测试获取当前活动的聊天面板
     */
    @Test
    fun testGetActiveTabPanel() {
        chatToolWindowPanel.addNewChatTab()
        
        val activePanel = chatToolWindowPanel.getActiveTabPanel()
        
        assertNotNull("活动面板不应为空", activePanel)
    }

    /**
     * 测试发送消息
     */
    @Test
    fun testSendMessage() {
        chatToolWindowPanel.sendMessage("测试消息")
        
        val activePanel = chatToolWindowPanel.getActiveTabPanel()
        assertNotNull("活动面板不应为空", activePanel)
        
        // 由于实际发送消息涉及到异步操作，这里只能验证面板存在
        // 更详细的测试需要使用集成测试
    }
    
    /**
     * 获取标签页数量
     */
    private fun ChatToolWindowPanel.getTabCount(): Int {
        val field = ChatToolWindowPanel::class.java.getDeclaredField("tabbedPane")
        field.isAccessible = true
        val tabbedPane = field.get(this) as javax.swing.JTabbedPane
        return tabbedPane.tabCount
    }
}
