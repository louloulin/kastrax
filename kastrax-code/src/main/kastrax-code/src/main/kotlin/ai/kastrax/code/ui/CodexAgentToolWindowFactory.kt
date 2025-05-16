package ai.kastrax.code.ui

import ai.kastrax.code.service.CodeAgentService
import ai.kastrax.code.service.ConversationService
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea

/**
 * Kastrax Codex 代理工具窗口工厂
 *
 * 创建 Kastrax Codex 代理工具窗口
 */
class CodexAgentToolWindowFactory : ToolWindowFactory, DumbAware {

    /**
     * 创建工具窗口内容
     *
     * @param project 项目
     * @param toolWindow 工具窗口
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 初始化服务
        CodeAgentService.getInstance(project).initialize()

        // 获取会话服务
        val conversationService = ConversationService.getInstance(project)
        val conversation = conversationService.getCurrentConversation()

        // 创建增强聊天面板
        val enhancedChatPanel = ai.kastrax.code.ui.components.EnhancedChatPanel(project, conversation)

        // 创建简单聊天面板作为备用
        val simpleChatPanel = createChatPanel(project)

        // 创建内容
        val contentFactory = ContentFactory.getInstance()
        val chatContent = contentFactory.createContent(enhancedChatPanel, "聊天", false)
        val historyContent = contentFactory.createContent(simpleChatPanel, "历史", false)

        toolWindow.contentManager.addContent(chatContent)
        toolWindow.contentManager.addContent(historyContent)
    }

    /**
     * 创建聊天面板
     *
     * @param project 项目
     * @return 聊天面板
     */
    private fun createChatPanel(project: Project): JPanel {
        // 创建主面板
        val mainPanel = JBPanel<JBPanel<*>>(BorderLayout())
        mainPanel.border = JBUI.Borders.empty(10)

        // 创建标题面板
        val titlePanel = JBPanel<JBPanel<*>>(BorderLayout())
        val titleLabel = JBLabel("Kastrax Codex AI 助手")
        titleLabel.font = titleLabel.font.deriveFont(16f)
        titlePanel.add(titleLabel, BorderLayout.WEST)

        // 创建模型选择下拉框（简化版）
        val modelPanel = JBPanel<JBPanel<*>>(BorderLayout())
        val modelLabel = JBLabel("模型: DeepSeek Chat")
        modelPanel.add(modelLabel, BorderLayout.EAST)
        titlePanel.add(modelPanel, BorderLayout.EAST)

        mainPanel.add(titlePanel, BorderLayout.NORTH)

        // 创建聊天历史区域
        val chatHistoryArea = JTextArea()
        chatHistoryArea.isEditable = false
        chatHistoryArea.text = "欢迎使用 Kastrax Codex AI 助手\n\n"
        chatHistoryArea.text += "我是基于 DeepSeek 的智能编程助手，可以帮助您：\n"
        chatHistoryArea.text += "- 生成代码\n"
        chatHistoryArea.text += "- 解释代码\n"
        chatHistoryArea.text += "- 重构代码\n"
        chatHistoryArea.text += "- 生成测试\n\n"
        chatHistoryArea.text += "请在下方输入框中输入您的问题或请求。"

        val scrollPane = JBScrollPane(chatHistoryArea)
        mainPanel.add(scrollPane, BorderLayout.CENTER)

        // 创建输入区域
        val inputPanel = JBPanel<JBPanel<*>>(BorderLayout())
        inputPanel.border = JBUI.Borders.empty(10, 0, 0, 0)

        val inputArea = JTextArea(3, 20)
        inputArea.border = JBUI.Borders.empty(5)
        inputPanel.add(JBScrollPane(inputArea), BorderLayout.CENTER)

        // 创建按钮面板
        val buttonPanel = JBPanel<JBPanel<*>>(BorderLayout())
        buttonPanel.border = JBUI.Borders.emptyLeft(10)

        // 添加发送按钮
        val sendButton = JButton("发送")
        sendButton.addActionListener {
            val userInput = inputArea.text.trim()
            if (userInput.isNotEmpty()) {
                chatHistoryArea.text += "\n\n用户: $userInput"
                chatHistoryArea.text += "\n\nKastrax Codex: 正在处理您的请求..."
                inputArea.text = ""

                // 模拟异步响应
                Thread {
                    Thread.sleep(1000)
                    chatHistoryArea.text = chatHistoryArea.text.replace("正在处理您的请求...", "这是一个模拟的响应。实际集成将使用 DeepSeek 模型生成响应。")
                }.start()
            }
        }
        buttonPanel.add(sendButton, BorderLayout.NORTH)
        inputPanel.add(buttonPanel, BorderLayout.EAST)

        mainPanel.add(inputPanel, BorderLayout.SOUTH)

        return mainPanel
    }
}
