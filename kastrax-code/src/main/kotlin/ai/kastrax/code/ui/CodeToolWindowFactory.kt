package ai.kastrax.code.ui

import ai.kastrax.code.service.CodeAgentService
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.content.ContentFactory
import java.awt.BorderLayout
import javax.swing.JButton
import javax.swing.JPanel
import javax.swing.JTextArea

/**
 * 代码工具窗口工厂
 * 
 * 创建代码工具窗口
 */
class CodeToolWindowFactory : ToolWindowFactory {
    
    /**
     * 创建工具窗口内容
     *
     * @param project 项目
     * @param toolWindow 工具窗口
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(createToolWindowPanel(project), "KastraX Code", false)
        toolWindow.contentManager.addContent(content)
        
        // 初始化服务
        CodeAgentService.getInstance(project).initialize()
    }
    
    /**
     * 创建工具窗口面板
     *
     * @param project 项目
     * @return 面板
     */
    private fun createToolWindowPanel(project: Project): JPanel {
        val panel = JBPanel<JBPanel<*>>(BorderLayout())
        
        // 标题
        val titlePanel = JPanel(BorderLayout())
        titlePanel.add(JBLabel("KastraX Code - AI 编程助手"), BorderLayout.CENTER)
        panel.add(titlePanel, BorderLayout.NORTH)
        
        // 聊天区域
        val chatArea = JTextArea()
        chatArea.isEditable = false
        chatArea.lineWrap = true
        chatArea.wrapStyleWord = true
        chatArea.text = "欢迎使用 KastraX Code！\n\n这是一个基于 KastraX 框架的 AI 编程助手，可以帮助你编写、解释和重构代码。\n\n请在下方输入框中输入你的问题或指令。"
        
        val chatScrollPane = JBScrollPane(chatArea)
        panel.add(chatScrollPane, BorderLayout.CENTER)
        
        // 输入区域
        val inputPanel = JPanel(BorderLayout())
        val inputArea = JTextArea(3, 20)
        inputArea.lineWrap = true
        inputArea.wrapStyleWord = true
        
        val inputScrollPane = JBScrollPane(inputArea)
        inputPanel.add(inputScrollPane, BorderLayout.CENTER)
        
        val sendButton = JButton("发送")
        sendButton.addActionListener {
            val input = inputArea.text.trim()
            if (input.isNotEmpty()) {
                chatArea.append("\n\n你: $input")
                inputArea.text = ""
                
                // 这里可以添加调用代码智能体的逻辑
                chatArea.append("\n\nKastraX: 正在处理你的请求...")
            }
        }
        
        inputPanel.add(sendButton, BorderLayout.EAST)
        panel.add(inputPanel, BorderLayout.SOUTH)
        
        return panel
    }
}
