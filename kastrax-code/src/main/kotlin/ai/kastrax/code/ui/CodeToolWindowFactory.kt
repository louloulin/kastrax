package ai.kastrax.code.ui

import ai.kastrax.code.service.CodeAgentService
import ai.kastrax.code.service.ConversationService
import ai.kastrax.code.ui.components.ContextPanel
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

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
        // 创建聊天工具窗口面板
        val chatToolWindowPanel = ChatToolWindowPanel(project, toolWindow.disposable)

        // 创建内容
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(chatToolWindowPanel, "聊天", false)
        toolWindow.contentManager.addContent(content)

        // 创建上下文面板
        val contextPanel = ContextPanel(project)
        val contextContent = contentFactory.createContent(contextPanel, "上下文", false)
        toolWindow.contentManager.addContent(contextContent)

        // 在后台线程中初始化服务
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            CodeAgentService.getInstance(project).initialize()
        }
    }



}
