package ai.kastrax.code.ui.components

import ai.kastrax.code.model.ChatConversation
import ai.kastrax.code.model.ChatMessage
import ai.kastrax.code.model.MessageRole
import ai.kastrax.code.service.CodeAgentService
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.util.ui.JBUI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Dimension
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.ScrollPaneConstants

/**
 * 聊天面板
 * 
 * 显示完整的聊天界面，包括消息列表和输入区域
 */
class ChatPanel(
    private val project: Project,
    private val conversation: ChatConversation
) : JBPanel<ChatPanel>(BorderLayout()) {
    
    private val messagesPanel = JBPanel<JBPanel<*>>()
    private val inputArea = JTextArea(3, 20)
    private val sendButton = JButton("发送")
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    
    init {
        // 设置消息列表面板
        messagesPanel.layout = BoxLayout(messagesPanel, BoxLayout.Y_AXIS)
        messagesPanel.border = JBUI.Borders.empty(8)
        
        val scrollPane = JBScrollPane(messagesPanel)
        scrollPane.horizontalScrollBarPolicy = ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER
        scrollPane.verticalScrollBarPolicy = ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED
        
        // 设置输入区域
        inputArea.lineWrap = true
        inputArea.wrapStyleWord = true
        inputArea.border = JBUI.Borders.empty(8)
        
        val inputScrollPane = JBScrollPane(inputArea)
        inputScrollPane.preferredSize = Dimension(0, 100)
        
        // 设置发送按钮
        sendButton.addActionListener {
            sendMessage()
        }
        
        // 创建输入面板
        val inputPanel = JBPanel<JBPanel<*>>(BorderLayout())
        inputPanel.add(inputScrollPane, BorderLayout.CENTER)
        inputPanel.add(sendButton, BorderLayout.EAST)
        
        // 添加组件到主面板
        add(scrollPane, BorderLayout.CENTER)
        add(inputPanel, BorderLayout.SOUTH)
        
        // 显示现有消息
        displayMessages()
    }
    
    /**
     * 显示现有消息
     */
    private fun displayMessages() {
        messagesPanel.removeAll()
        
        if (conversation.messages.isEmpty()) {
            // 添加欢迎消息
            val welcomeMessage = ChatMessage(
                role = MessageRole.ASSISTANT,
                content = "欢迎使用 KastraX Code！我是你的 AI 编程助手，可以帮助你编写、解释和重构代码。请在下方输入框中输入你的问题或指令。"
            )
            addMessagePanel(welcomeMessage)
        } else {
            // 显示现有消息
            conversation.messages.forEach { message ->
                addMessagePanel(message)
            }
        }
        
        revalidate()
        repaint()
    }
    
    /**
     * 添加消息面板
     */
    private fun addMessagePanel(message: ChatMessage) {
        val messagePanel = ChatMessagePanel(message)
        messagesPanel.add(messagePanel)
        messagesPanel.add(Box.createVerticalStrut(8))
        
        // 滚动到底部
        messagesPanel.revalidate()
        messagesPanel.repaint()
        
        val scrollPane = SwingUtilities.getAncestorOfClass(JBScrollPane::class.java, messagesPanel) as? JBScrollPane
        scrollPane?.let {
            val verticalBar = it.verticalScrollBar
            verticalBar.value = verticalBar.maximum
        }
    }
    
    /**
     * 发送消息
     */
    private fun sendMessage() {
        val text = inputArea.text.trim()
        if (text.isEmpty()) {
            return
        }
        
        // 创建用户消息
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = text
        )
        
        // 添加到会话
        conversation.addMessage(userMessage)
        
        // 显示用户消息
        addMessagePanel(userMessage)
        
        // 清空输入区域
        inputArea.text = ""
        
        // 显示正在处理的消息
        val processingMessage = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "正在处理你的请求..."
        )
        addMessagePanel(processingMessage)
        
        // 调用代码智能体
        coroutineScope.launch {
            try {
                val codeAgent = CodeAgentService.getInstance(project).getCodeAgent()
                
                // 根据消息内容判断调用哪个方法
                val response = if (text.contains("解释") || text.contains("explain")) {
                    // 提取代码部分
                    val codePattern = "```(\\w*)\\s*\\n([\\s\\S]*?)```".toRegex()
                    val matchResult = codePattern.find(text)
                    
                    if (matchResult != null) {
                        val code = matchResult.groupValues[2]
                        codeAgent.explainCode(code, ai.kastrax.code.model.DetailLevel.DETAILED)
                    } else {
                        "请提供要解释的代码，使用 ```language\\n代码\\n``` 格式。"
                    }
                } else if (text.contains("重构") || text.contains("refactor")) {
                    // 提取代码和指令
                    val codePattern = "```(\\w*)\\s*\\n([\\s\\S]*?)```".toRegex()
                    val matchResult = codePattern.find(text)
                    
                    if (matchResult != null) {
                        val code = matchResult.groupValues[2]
                        val instructions = text.replace(matchResult.value, "").trim()
                        codeAgent.refactorCode(code, instructions)
                    } else {
                        "请提供要重构的代码，使用 ```language\\n代码\\n``` 格式，并说明重构要求。"
                    }
                } else if (text.contains("测试") || text.contains("test")) {
                    // 提取代码
                    val codePattern = "```(\\w*)\\s*\\n([\\s\\S]*?)```".toRegex()
                    val matchResult = codePattern.find(text)
                    
                    if (matchResult != null) {
                        val code = matchResult.groupValues[2]
                        val language = matchResult.groupValues[1]
                        val framework = if (language.equals("java", ignoreCase = true)) "JUnit" 
                                       else if (language.equals("kotlin", ignoreCase = true)) "JUnit" 
                                       else "默认测试框架"
                        codeAgent.generateTest(code, framework)
                    } else {
                        "请提供要生成测试的代码，使用 ```language\\n代码\\n``` 格式。"
                    }
                } else {
                    // 默认生成代码
                    val language = if (text.contains("java", ignoreCase = true)) "java"
                                  else if (text.contains("kotlin", ignoreCase = true)) "kotlin"
                                  else if (text.contains("python", ignoreCase = true)) "python"
                                  else if (text.contains("javascript", ignoreCase = true) || text.contains("js", ignoreCase = true)) "javascript"
                                  else "kotlin"
                    
                    codeAgent.generateCode(text, language)
                }
                
                // 移除处理中的消息
                messagesPanel.remove(messagesPanel.componentCount - 2) // 移除消息
                messagesPanel.remove(messagesPanel.componentCount - 1) // 移除间隔
                
                // 创建助手回复消息
                val assistantMessage = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = response
                )
                
                // 添加到会话
                conversation.addMessage(assistantMessage)
                
                // 显示助手回复
                addMessagePanel(assistantMessage)
                
            } catch (e: Exception) {
                // 移除处理中的消息
                messagesPanel.remove(messagesPanel.componentCount - 2) // 移除消息
                messagesPanel.remove(messagesPanel.componentCount - 1) // 移除间隔
                
                // 创建错误消息
                val errorMessage = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "处理请求时出错：${e.message}"
                )
                
                // 添加到会话
                conversation.addMessage(errorMessage)
                
                // 显示错误消息
                addMessagePanel(errorMessage)
            }
        }
    }
}
