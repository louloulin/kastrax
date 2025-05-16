package ai.kastrax.code.ui

import ai.kastrax.code.service.CodeAgentService
import ai.kastrax.code.service.ConversationService
import ai.kastrax.code.ui.components.ChatPanel
import ai.kastrax.code.ui.components.EnhancedChatPanel
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

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
        // 初始化服务
        CodeAgentService.getInstance(project).initialize()

        // 创建聊天面板
        val chatPanel = createChatPanel(project)

        // 创建标签页面板
        val tabbedPane = JBTabbedPane()
        tabbedPane.addTab("聊天", chatPanel)

        // 创建内容
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(tabbedPane, null, false)
        toolWindow.contentManager.addContent(content)
    }

    /**
     * 创建聊天面板
     *
     * @param project 项目
     * @return 聊天面板
     */
    private fun createChatPanel(project: Project): JPanel {
        // 获取或创建会话
        val conversationService = ConversationService.getInstance(project)
        val conversation = conversationService.getCurrentConversation()

        // 创建增强聊天面板
        return EnhancedChatPanel(project, conversation)
    }
}
