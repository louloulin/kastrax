package ai.kastrax.code.ui

import ai.kastrax.code.service.ConversationService
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBLabel
import com.intellij.ui.components.JBPanel
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Codex 工具窗口面板
 *
 * 提供 Kastrax Codex 功能的工具窗口
 */
class CodexToolWindowPanel(
    private val project: Project,
    private val parentDisposable: Disposable
) : SimpleToolWindowPanel(true) {

    init {
        // 初始化工具窗口面板
        initToolWindowPanel()
    }

    /**
     * 初始化工具窗口面板
     */
    private fun initToolWindowPanel() {
        ApplicationManager.getApplication().invokeLater {
            // 创建工具栏
            val actionToolbar = createActionToolbar()

            // 创建内容面板
            val contentPanel = createContentPanel()

            // 设置工具栏和内容
            setToolbar(actionToolbar.component)
            setContent(contentPanel)
        }
    }

    /**
     * 创建内容面板
     */
    private fun createContentPanel(): JPanel {
        return JBPanel<JBPanel<*>>().apply {
            layout = BorderLayout()
            add(JBLabel("Kastrax Codex 功能即将推出，敬请期待！", JBLabel.CENTER), BorderLayout.CENTER)
            border = JBUI.Borders.empty(10)
        }
    }

    /**
     * 创建操作工具栏
     */
    private fun createActionToolbar() = ActionManager.getInstance().createActionToolbar(
        "KastraxCodexToolbar",
        createActionGroup(),
        true
    )

    /**
     * 创建操作组
     */
    private fun createActionGroup(): DefaultActionGroup {
        val actionGroup = DefaultActionGroup("TOOLBAR_ACTION_GROUP", false)

        // 添加设置按钮
        actionGroup.add(object : AnAction("设置", "打开设置", AllIcons.General.Settings) {
            override fun actionPerformed(e: AnActionEvent) {
                // TODO: 打开设置对话框
            }
        })

        return actionGroup
    }
}
