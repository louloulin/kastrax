package ai.kastrax.code.ui

import ai.kastrax.code.service.CodeAgentService
import com.intellij.openapi.project.DumbAware
import com.intellij.openapi.project.Project
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory

/**
 * Kastrax Codex 工具窗口工厂
 *
 * 创建 Kastrax Codex 工具窗口
 */
class CodexAgentToolWindowFactory : ToolWindowFactory, DumbAware {

    /**
     * 创建工具窗口内容
     *
     * @param project 项目
     * @param toolWindow 工具窗口
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 创建 Codex 工具窗口面板
        val codexToolWindowPanel = CodexToolWindowPanel(project, toolWindow.disposable)

        // 创建内容
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(codexToolWindowPanel, "Codex", false)
        toolWindow.contentManager.addContent(content)

        // 在后台线程中初始化服务
        com.intellij.openapi.application.ApplicationManager.getApplication().executeOnPooledThread {
            CodeAgentService.getInstance(project).initialize()
        }
    }
}
