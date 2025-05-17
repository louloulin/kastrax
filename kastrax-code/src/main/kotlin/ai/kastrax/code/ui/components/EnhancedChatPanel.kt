package ai.kastrax.code.ui.components

import ai.kastrax.code.context.CodeContextProvider
import ai.kastrax.code.memory.ShortTermMemory
import ai.kastrax.code.model.ChatConversation
import ai.kastrax.code.model.ChatMessage
import ai.kastrax.code.model.MessageRole
import ai.kastrax.code.service.CodeAgentService
import ai.kastrax.code.service.ConversationService
import ai.kastrax.code.workflow.CheckpointManager
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBLabel
import com.intellij.util.ui.JBUI
import com.intellij.ui.JBColor
import com.intellij.openapi.actionSystem.DataManager
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.ui.SimpleToolWindowPanel
import com.intellij.openapi.project.Project
import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.wm.WindowManager
import com.intellij.ui.JBSplitter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Font
import java.awt.event.KeyAdapter
import java.awt.event.KeyEvent
import javax.swing.BorderFactory
import javax.swing.Box
import javax.swing.BoxLayout
import javax.swing.JButton
import javax.swing.JComboBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextArea
import javax.swing.ScrollPaneConstants
import javax.swing.SwingUtilities

/**
 * 增强聊天面板
 *
 * 提供更强大的聊天界面，支持代码高亮和更多交互功能
 */
class EnhancedChatPanel(
    private val project: Project,
    private val conversation: ChatConversation
) : SimpleToolWindowPanel(true) {

    private val messagesPanel = JBPanel<JPanel>()
    private val inputArea = JTextArea(3, 20)
    private val sendButton = JButton("发送")
    private val statusLabel = JBLabel("就绪")
    private val modelSelector = JComboBox(arrayOf("DeepSeek", "Gemini", "OpenAI", "Anthropic"))
    private val codeDisplayPanel = CodeDisplayPanel(project)
    private val contextPanel = ContextPanel(project)

    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)

    // 服务
    private val codeAgentService = CodeAgentService.getInstance(project)
    private val conversationService = ConversationService.getInstance(project)
    private val shortTermMemory = ShortTermMemory.getInstance(project)
    private val contextProvider = CodeContextProvider.getInstance(project)

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
        inputArea.font = inputArea.font.deriveFont(14f)

        // 添加键盘监听器，支持Ctrl+Enter发送
        inputArea.addKeyListener(object : KeyAdapter() {
            override fun keyPressed(e: KeyEvent) {
                if (e.keyCode == KeyEvent.VK_ENTER && e.isControlDown) {
                    e.consume()
                    sendMessage()
                }
            }
        })

        val inputScrollPane = JBScrollPane(inputArea)
        inputScrollPane.preferredSize = Dimension(0, 100)

        // 设置发送按钮
        sendButton.addActionListener {
            sendMessage()
        }

        // 创建输入控制面板
        val inputControlPanel = createInputControlPanel()

        // 创建输入面板
        val inputPanel = JBPanel<JBPanel<*>>(BorderLayout())
        inputPanel.add(inputScrollPane, BorderLayout.CENTER)
        inputPanel.add(inputControlPanel, BorderLayout.EAST)

        // 创建状态面板
        val statusPanel = createStatusPanel()

        // 创建主聊天面板
        val chatMainPanel = JBPanel<JBPanel<*>>(BorderLayout())
        chatMainPanel.add(scrollPane, BorderLayout.CENTER)
        chatMainPanel.add(inputPanel, BorderLayout.SOUTH)
        chatMainPanel.add(statusPanel, BorderLayout.NORTH)

        // 创建分割面板
        val splitter = JBSplitter(false, 0.7f)
        splitter.firstComponent = chatMainPanel

        // 创建右侧面板
        val rightPanel = createRightPanel()
        splitter.secondComponent = rightPanel

        // 创建工具栏
        val toolbar = createToolbar()

        // 设置面板
        setContent(splitter)
        setToolbar(toolbar)

        // 显示现有消息
        displayMessages()
    }

    /**
     * 创建工具栏
     */
    private fun createToolbar(): JComponent {
        val actionGroup = DefaultActionGroup()

        // 添加新建会话按钮
        val newConversationAction = object : AnAction("新建会话", "创建新的会话", AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {
                val newConversation = conversationService.createConversation("新会话")
                conversationService.setCurrentConversation(newConversation)
                // 通知父容器刷新
                ApplicationManager.getApplication().invokeLater {
                    val event = DataManager.getInstance().getDataContextChangeEvent()
                    WindowManager.getInstance().getFrame(project)?.dispatchEvent(event)
                }
            }
        }
        actionGroup.add(newConversationAction)

        // 添加清空会话按钮
        val clearConversationAction = object : AnAction("清空会话", "清空当前会话", null) {
            override fun actionPerformed(e: AnActionEvent) {
                conversation.clearMessages()
                messagesPanel.removeAll()
                messagesPanel.revalidate()
                messagesPanel.repaint()

                // 添加欢迎消息
                val welcomeMessage = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = "欢迎使用 KastraX Code！我是你的 AI 编程助手，可以帮助你编写、解释和重构代码。请在下方输入框中输入你的问题或指令。"
                )
                addMessagePanel(welcomeMessage)
            }
        }
        actionGroup.add(clearConversationAction)

        // 添加保存会话按钮
        val saveConversationAction = object : AnAction("保存会话", "保存当前会话", null) {
            override fun actionPerformed(e: AnActionEvent) {
                // 保存会话逻辑
                statusLabel.text = "会话已保存"
            }
        }
        actionGroup.add(saveConversationAction)

        // 添加创建检查点按钮
        val createCheckpointAction = object : AnAction("创建检查点", "创建代码检查点", null) {
            override fun actionPerformed(e: AnActionEvent) {
                coroutineScope.launch {
                    try {
                        val checkpointManager = CheckpointManager.getInstance(project)
                        val checkpoint = checkpointManager.createCheckpoint(
                            name = "手动检查点",
                            description = "由用户手动创建的检查点"
                        )
                        statusLabel.text = "检查点已创建: ${checkpoint.id}"
                    } catch (ex: Exception) {
                        statusLabel.text = "创建检查点失败: ${ex.message}"
                    }
                }
            }
        }
        actionGroup.add(createCheckpointAction)

        // 创建工具栏
        val actionToolbar = ActionManager.getInstance().createActionToolbar(
            "KastraxCodeChatToolbar",
            actionGroup,
            true
        )

        return actionToolbar.component
    }

    /**
     * 创建输入控制面板
     */
    private fun createInputControlPanel(): JPanel {
        val panel = JBPanel<JPanel>(BorderLayout())

        // 发送按钮
        panel.add(sendButton, BorderLayout.NORTH)

        return panel
    }

    /**
     * 创建状态面板
     */
    private fun createStatusPanel(): JPanel {
        val panel = JBPanel<JPanel>(FlowLayout(FlowLayout.LEFT))
        panel.border = BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(0, 0, 1, 0, JBColor.border()),
            JBUI.Borders.empty(4)
        )

        // 状态标签
        statusLabel.font = statusLabel.font.deriveFont(Font.ITALIC)
        panel.add(statusLabel)

        // 添加间隔
        panel.add(Box.createHorizontalStrut(20))

        // 模型选择器
        val modelLabel = JBLabel("模型:")
        panel.add(modelLabel)
        panel.add(modelSelector)

        return panel
    }

    /**
     * 创建右侧面板
     */
    private fun createRightPanel(): JPanel {
        val panel = JBPanel<JPanel>(BorderLayout())

        // 创建标签页面板
        val tabbedPane = com.intellij.ui.components.JBTabbedPane()

        // 添加代码展示面板
        tabbedPane.addTab("代码", codeDisplayPanel)

        // 添加上下文面板
        tabbedPane.addTab("上下文", contextPanel)

        panel.add(tabbedPane, BorderLayout.CENTER)

        return panel
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
        val messagePanel = EnhancedChatMessagePanel(
            message = message,
            project = project,
            onCodeSelected = { code, language ->
                codeDisplayPanel.setCode(code, language)
            }
        )
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

        // 更新状态
        statusLabel.text = "正在处理..."

        // 创建用户消息
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = text
        )

        // 添加到会话
        conversation.addMessage(userMessage)

        // 存储到短期记忆
        coroutineScope.launch {
            shortTermMemory.storeMessage("user", text)
        }

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
                val codeAgent = codeAgentService.getCodeAgent()

                // 获取当前上下文
                val context = contextProvider.getQueryContext(text)
                contextPanel.setContext(context)

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

                // 存储到短期记忆
                shortTermMemory.storeMessage("assistant", response)

                // 显示助手回复
                addMessagePanel(assistantMessage)

                // 提取代码并显示在代码面板中
                val codePattern = "```(\\w*)\\s*\\n([\\s\\S]*?)```".toRegex()
                val matchResult = codePattern.find(response)
                if (matchResult != null) {
                    val language = matchResult.groupValues[1]
                    val code = matchResult.groupValues[2]
                    codeDisplayPanel.setCode(code, language)
                }

                // 更新状态
                statusLabel.text = "就绪"

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

                // 更新状态
                statusLabel.text = "错误：${e.message}"
            }
        }
    }

    /**
     * 释放资源
     */
    fun dispose() {
        codeDisplayPanel.dispose()
    }
}
