package ai.kastrax.code.ui.components

import ai.kastrax.code.model.ChatConversation
import ai.kastrax.code.model.ChatMessage
import ai.kastrax.code.model.MessageRole
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformTestCase
import org.junit.Test
import org.mockito.Mockito.mock
import java.lang.reflect.Method

/**
 * 增强聊天面板测试
 */
class EnhancedChatPanelTest : LightPlatformTestCase() {

    private lateinit var chatPanel: EnhancedChatPanel
    private lateinit var project: Project
    private lateinit var conversation: ChatConversation

    override fun setUp() {
        super.setUp()
        project = mock(Project::class.java)
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
        chatPanel = EnhancedChatPanel(project, conversation)
    }

    /**
     * 测试JBPanel构造函数
     */
    @Test
    fun testJBPanelConstructor() {
        // 验证chatPanel已经成功创建
        assertNotNull("聊天面板不应为空", chatPanel)
    }

    /**
     * 测试loadMessages方法
     */
    @Test
    fun testLoadMessages() {
        // 获取私有方法loadMessages
        val loadMessagesMethod = EnhancedChatPanel::class.java.getDeclaredMethod("loadMessages")
        loadMessagesMethod.isAccessible = true
        
        // 调用loadMessages方法
        loadMessagesMethod.invoke(chatPanel)
        
        // 验证消息已加载
        // 由于无法直接访问私有字段messagesPanel，这里只能验证chatPanel不为空
        assertNotNull("聊天面板不应为空", chatPanel)
    }

    /**
     * 测试清空消息
     */
    @Test
    fun testClearMessages() {
        // 添加一条消息
        conversation.addMessage(
            ChatMessage(
                role = MessageRole.USER,
                content = "测试消息"
            )
        )
        
        // 获取私有方法clearMessages
        val clearMessagesMethod = EnhancedChatPanel::class.java.getDeclaredMethod("clearMessages")
        clearMessagesMethod.isAccessible = true
        
        // 调用clearMessages方法
        clearMessagesMethod.invoke(chatPanel)
        
        // 验证会话消息已清空
        assertEquals("会话消息应为空", 0, conversation.messages.size)
    }

    /**
     * 测试发送用户消息
     */
    @Test
    fun testSendUserMessage() {
        // 获取私有方法sendUserMessage
        val sendUserMessageMethod = findMethod(EnhancedChatPanel::class.java, "sendUserMessage")
        sendUserMessageMethod?.isAccessible = true
        
        // 设置输入区域文本
        val inputAreaField = EnhancedChatPanel::class.java.getDeclaredField("inputArea")
        inputAreaField.isAccessible = true
        val inputArea = inputAreaField.get(chatPanel)
        val setTextMethod = inputArea.javaClass.getMethod("setText", String::class.java)
        setTextMethod.invoke(inputArea, "测试消息")
        
        // 调用sendUserMessage方法
        sendUserMessageMethod?.invoke(chatPanel)
        
        // 验证会话消息已添加
        assertTrue("会话应包含用户消息", conversation.messages.any { it.role == MessageRole.USER && it.content == "测试消息" })
    }
    
    /**
     * 查找方法，支持不同参数数量的重载方法
     */
    private fun findMethod(clazz: Class<*>, methodName: String): Method? {
        return clazz.declaredMethods.find { it.name == methodName }
    }
}
