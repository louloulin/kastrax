package ai.kastrax.codex.ui

/**
 * Codex智能体工具窗口工厂
 */
class CodexAgentToolWindowFactory : ToolWindowFactory {

    /**
     * 创建工具窗口
     */
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        // 获取智能体服务
        val agentService = project.service<CodexAgentService>()

        // 创建智能体控制面板
        val agentControlPanel = AgentControlPanel(project, agentService)

        // 创建内容
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(agentControlPanel, "智能体", false)

        // 添加内容到工具窗口
        toolWindow.contentManager.addContent(content)
    }

    /**
     * 是否应该在启动时激活
     */
    override fun shouldBeAvailable(project: Project) = true
}