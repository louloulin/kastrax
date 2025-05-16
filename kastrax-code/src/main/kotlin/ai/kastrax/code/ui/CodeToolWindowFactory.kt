package ai.kastrax.code.ui

import ai.kastrax.code.service.CodeAgentService
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
 * 代码工具窗口工厂
 *
 * 创建代码工具窗口
 */
class CodeToolWindowFactory : ToolWindowFactory, DumbAware {

    /**
     * 创建工具窗口内容
     *
     * @param project 项目
     * @param toolWindow 工具窗口
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 初始化服务
        CodeAgentService.getInstance(project).initialize()

        // 创建简单的聊天面板
        val chatPanel = createSimpleChatPanel(project)

        // 创建内容
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(chatPanel, null, false)
        toolWindow.contentManager.addContent(content)
    }

    /**
     * 创建简单的聊天面板
     *
     * @param project 项目
     * @return 聊天面板
     */
    private fun createSimpleChatPanel(project: Project): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        panel.border = JBUI.Borders.empty(10)

        // 添加标题
        val titleLabel = JBLabel("KastraX Code AI 助手")
        titleLabel.font = titleLabel.font.deriveFont(16f)
        panel.add(titleLabel, BorderLayout.NORTH)

        // 添加聊天历史区域
        val chatHistoryArea = JTextArea()
        chatHistoryArea.isEditable = false
        chatHistoryArea.text = "欢迎使用 KastraX Code AI 助手\n\n"
        chatHistoryArea.text += "请在下方输入框中输入您的问题或请求。\n"
        chatHistoryArea.text += "您也可以在编辑器中选中代码，然后右键点击选择 KastraX Code 菜单。"
        val scrollPane = JBScrollPane(chatHistoryArea)
        panel.add(scrollPane, BorderLayout.CENTER)

        // 添加输入区域
        val inputPanel = JBPanel<JBPanel<*>>(BorderLayout())
        val inputArea = JTextArea(3, 20)
        inputPanel.add(JBScrollPane(inputArea), BorderLayout.CENTER)

        // 添加发送按钮
        val sendButton = JButton("发送")
        sendButton.addActionListener {
            val userInput = inputArea.text.trim()
            if (userInput.isNotEmpty()) {
                chatHistoryArea.text += "\n\n用户: $userInput"
                chatHistoryArea.text += "\n\nKastraX: 正在处理您的请求..."
                inputArea.text = ""

                // 模拟异步响应
                Thread {
                    Thread.sleep(1000)
                    chatHistoryArea.text = chatHistoryArea.text.replace("正在处理您的请求...", "这是一个模拟的响应。实际集成将使用 DeepSeek 模型生成响应。")
                }.start()
            }
        }
        inputPanel.add(sendButton, BorderLayout.EAST)
        panel.add(inputPanel, BorderLayout.SOUTH)

        return panel
    }

}
