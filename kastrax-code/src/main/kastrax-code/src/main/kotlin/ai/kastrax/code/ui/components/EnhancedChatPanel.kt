package ai.kastrax.code.ui.components

import ai.kastrax.code.model.ChatConversation
import ai.kastrax.code.model.ChatMessage
import ai.kastrax.code.model.MessageRole
import ai.kastrax.code.service.CodeAgentService
import ai.kastrax.code.service.ConversationService
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.OnePixelSplitter
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import java.awt.FlowLayout
import java.awt.event.ActionEvent
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.AbstractAction
import javax.swing.Action
import javax.swing.JButton
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.KeyStroke

/**
 * 增强聊天面板
 *
 * 提供更丰富的聊天功能，包括多标签页、上下文显示等
 *
 * @param project 项目
 * @param conversation 聊天会话
 */
class EnhancedChatPanel(
    private val project: Project,
    private val conversation: ChatConversation
) : SimpleToolWindowPanel(true) {

    private val agentService = CodeAgentService.getInstance(project)
    private val conversationService = ConversationService.getInstance(project)

    private val messagesPanel = JBPanel<JBPanel<*>>(BorderLayout())
    private val inputArea = JTextArea(3, 20)
    private val sendButton = JButton("发送")

    private val tabbedPane = JBTabbedPane()
    private val splitter = OnePixelSplitter(true, 0.7f)

    init {
        initializeUI()
        loadConversation()
    }

    /**
     * 初始化UI
     */
    private fun initializeUI() {
        // 创建主面板
        val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())

        // 创建标签页面板
        tabbedPane.addTab("聊天", createChatPanel())
        tabbedPane.addTab("上下文", JBPanel<JBPanel<*>>())

        mainPanel.add(tabbedPane, BorderLayout.CENTER)

        // 设置内容
        setContent(mainPanel)

        // 设置工具栏
        setToolbar(createToolbar())
    }

    /**
     * 创建工具栏
     */
    private fun createToolbar(): JComponent {
        val toolbarPanel = JBPanel<JBPanel<*>>(BorderLayout())

        // 创建新会话按钮
        val newConversationButton = JButton("新会话")
        newConversationButton.addActionListener {
            val newConversation = conversationService.createConversation()
            val newPanel = EnhancedChatPanel(project, newConversation)

            // 通知父容器刷新
            ApplicationManager.getApplication().invokeLater {
                val event = java.util.EventObject(this)
                // 在实际实现中，这里应该使用正确的事件通知机制
            }
        }

        // 创建清空会话按钮
        val clearConversationButton = JButton("清空会话")
        clearConversationButton.addActionListener {
            messagesPanel.removeAll()
            conversation.messages.clear()
            conversationService.saveConversation(conversation)
            messagesPanel.revalidate()
            messagesPanel.repaint()
        }

        // 创建保存会话按钮
        val saveConversationButton = JButton("保存会话")
        saveConversationButton.addActionListener {
            conversationService.saveConversation(conversation)
        }

        // 添加按钮到工具栏
        val buttonsPanel = JBPanel<JBPanel<*>>(FlowLayout(FlowLayout.LEFT))
        buttonsPanel.add(newConversationButton)
        buttonsPanel.add(clearConversationButton)
        buttonsPanel.add(saveConversationButton)

        toolbarPanel.add(buttonsPanel, BorderLayout.WEST)

        return toolbarPanel
    }

    /**
     * 创建聊天面板
     */
    private fun createChatPanel(): JComponent {
        val chatPanel = JBPanel<JBPanel<*>>(BorderLayout())

        // 创建消息面板
        val scrollPane = JBScrollPane(messagesPanel)
        chatPanel.add(scrollPane, BorderLayout.CENTER)

        // 创建输入面板
        val inputPanel = JBPanel<JBPanel<*>>(BorderLayout())
        inputPanel.border = JBUI.Borders.empty(10, 0, 0, 0)

        // 设置输入区域
        inputArea.border = JBUI.Borders.empty(5)
        inputArea.lineWrap = true
        inputArea.wrapStyleWord = true

        // 添加快捷键
        val enterAction = object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                sendMessage()
            }
        }

        inputArea.getInputMap(JComponent.WHEN_FOCUSED).put(
            KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, KeyEvent.CTRL_DOWN_MASK),
            "sendMessage"
        )
        inputArea.actionMap.put("sendMessage", enterAction)

        // 添加键盘监听器
        inputArea.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && e.isControlDown) {
                    e.consume()
                    sendMessage()
                }
            }
        })

        inputPanel.add(JBScrollPane(inputArea), BorderLayout.CENTER)

        // 设置发送按钮
        sendButton.addActionListener { sendMessage() }

        val buttonPanel = JBPanel<JBPanel<*>>(BorderLayout())
        buttonPanel.border = JBUI.Borders.emptyLeft(10)
        buttonPanel.add(sendButton, BorderLayout.NORTH)

        inputPanel.add(buttonPanel, BorderLayout.EAST)

        chatPanel.add(inputPanel, BorderLayout.SOUTH)

        return chatPanel
    }

    /**
     * 加载会话
     */
    private fun loadConversation() {
        messagesPanel.removeAll()

        // 使用垂直布局
        val layout = BorderLayout()
        messagesPanel.layout = layout

        val messagesContainer = JBPanel<JBPanel<*>>()
        messagesContainer.layout = javax.swing.BoxLayout(messagesContainer, javax.swing.BoxLayout.Y_AXIS)

        // 添加消息
        for (message in conversation.messages) {
            val messagePanel = createMessagePanel(message)
            messagesContainer.add(messagePanel)
        }

        messagesPanel.add(messagesContainer, BorderLayout.NORTH)
        messagesPanel.revalidate()
        messagesPanel.repaint()
    }

    /**
     * 创建消息面板
     */
    private fun createMessagePanel(message: ChatMessage): JComponent {
        val messagePanel = JBPanel<JBPanel<*>>(BorderLayout())
        messagePanel.border = JBUI.Borders.empty(5)

        // 创建标题
        val titlePanel = JBPanel<JBPanel<*>>(BorderLayout())
        val roleLabel = JBLabel(if (message.role == MessageRole.USER) "用户" else "AI")
        roleLabel.foreground = if (message.role == MessageRole.USER) JBColor.BLUE else JBColor.GREEN
        titlePanel.add(roleLabel, BorderLayout.WEST)

        messagePanel.add(titlePanel, BorderLayout.NORTH)

        // 创建内容
        val contentPanel = JBPanel<JBPanel<*>>(BorderLayout())
        val contentArea = JTextArea(message.content)
        contentArea.isEditable = false
        contentArea.lineWrap = true
        contentArea.wrapStyleWord = true
        contentArea.background = JBColor.background()
        contentPanel.add(JBScrollPane(contentArea), BorderLayout.CENTER)

        messagePanel.add(contentPanel, BorderLayout.CENTER)

        return messagePanel
    }

    /**
     * 发送消息
     */
    private fun sendMessage() {
        val content = inputArea.text.trim()
        if (content.isEmpty()) {
            return
        }

        // 创建用户消息
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = content
        )

        // 添加到会话
        conversation.messages.add(userMessage)

        // 更新UI
        val userMessagePanel = createMessagePanel(userMessage)
        val messagesContainer = messagesPanel.components.firstOrNull() as? JPanel
        messagesContainer?.add(userMessagePanel)
        messagesPanel.revalidate()
        messagesPanel.repaint()

        // 清空输入区域
        inputArea.text = ""

        // 禁用发送按钮
        sendButton.isEnabled = false

        // 创建AI响应消息（占位）
        val aiMessage = ChatMessage(
            role = MessageRole.ASSISTANT,
            content = "正在思考..."
        )

        // 添加到会话
        conversation.messages.add(aiMessage)

        // 更新UI
        val aiMessagePanel = createMessagePanel(aiMessage)
        messagesContainer?.add(aiMessagePanel)
        messagesPanel.revalidate()
        messagesPanel.repaint()

        // 异步获取AI响应
        ApplicationManager.getApplication().executeOnPooledThread {
            try {
                // 模拟AI响应延迟
                Thread.sleep(1000)

                // 更新AI响应
                val response = "这是一个模拟的AI响应。在实际实现中，这里将使用DeepSeek模型生成响应。"
                aiMessage.content = response

                // 更新UI
                ApplicationManager.getApplication().invokeLater {
                    // 重新加载会话
                    loadConversation()

                    // 启用发送按钮
                    sendButton.isEnabled = true

                    // 保存会话
                    conversationService.saveConversation(conversation)
                }
            } catch (e: Exception) {
                // 处理错误
                ApplicationManager.getApplication().invokeLater {
                    aiMessage.content = "生成响应时发生错误: ${e.message}"
                    loadConversation()
                    sendButton.isEnabled = true
                }
            }
        }
    }
}
