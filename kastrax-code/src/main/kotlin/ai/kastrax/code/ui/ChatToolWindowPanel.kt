package ai.kastrax.code.ui

import ai.kastrax.code.model.ChatConversation
import ai.kastrax.code.service.ConversationService
import ai.kastrax.code.ui.components.EnhancedChatPanel
import com.intellij.icons.AllIcons
import com.intellij.openapi.Disposable
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.ui.components.JBTabbedPane
import com.intellij.ui.content.ContentFactory
import com.intellij.util.ui.JBUI
import com.intellij.util.ui.components.BorderLayoutPanel
import java.awt.BorderLayout
import javax.swing.JComponent
import javax.swing.JPanel

/**
 * 聊天工具窗口面板
 *
 * 提供带有标签页的聊天工具窗口
 */
class ChatToolWindowPanel(
    private val project: Project,
    private val parentDisposable: Disposable
) : SimpleToolWindowPanel(true) {

    private val tabbedPane = JBTabbedPane()

    init {
        // 初始化工具窗口面板
        initToolWindowPanel()
    }

    /**
     * 初始化工具窗口面板
     */
    private fun initToolWindowPanel() {
        ApplicationManager.getApplication().invokeLater {
            // 创建新的聊天标签页
            addNewChatTab()

            // 创建工具栏
            val actionToolbar = createActionToolbar()

            // 设置工具栏和内容
            setToolbar(actionToolbar.component)
            setContent(BorderLayoutPanel().apply {
                addToCenter(tabbedPane)
                border = JBUI.Borders.empty()
            })
        }
    }

    /**
     * 创建操作工具栏
     */
    private fun createActionToolbar() = ActionManager.getInstance().createActionToolbar(
        "KastraxCodeChatToolbar",
        createActionGroup(),
        true
    )

    /**
     * 创建操作组
     */
    private fun createActionGroup(): DefaultActionGroup {
        val actionGroup = DefaultActionGroup("TOOLBAR_ACTION_GROUP", false)

        // 添加新建会话按钮
        actionGroup.add(object : AnAction("新建会话", "创建新的会话", AllIcons.General.Add) {
            override fun actionPerformed(e: AnActionEvent) {
                addNewChatTab()
            }
        })

        // 添加清空当前会话按钮
        actionGroup.add(object : AnAction("清空会话", "清空当前会话", AllIcons.Actions.GC) {
            override fun actionPerformed(e: AnActionEvent) {
                getActiveTabPanel()?.clearMessages()
            }
        })

        // 添加分隔符
        actionGroup.addSeparator()

        // 添加设置按钮
        actionGroup.add(object : AnAction("设置", "打开设置", AllIcons.General.Settings) {
            override fun actionPerformed(e: AnActionEvent) {
                // TODO: 打开设置对话框
            }
        })

        return actionGroup
    }

    /**
     * 添加新的聊天标签页
     *
     * @return 聊天面板
     */
    fun addNewChatTab(): EnhancedChatPanel {
        // 创建新的会话
        val conversation = ConversationService.getInstance(project).createConversation()

        // 创建聊天面板
        val chatPanel = EnhancedChatPanel(project, conversation)

        // 添加到标签页
        tabbedPane.addTab("新会话", chatPanel)
        tabbedPane.selectedIndex = tabbedPane.tabCount - 1

        // 重绘和验证
        repaint()
        revalidate()

        return chatPanel
    }

    /**
     * 获取当前活动的聊天面板
     *
     * @return 当前活动的聊天面板，如果没有则返回null
     */
    fun getActiveTabPanel(): EnhancedChatPanel? {
        val selectedIndex = tabbedPane.selectedIndex
        if (selectedIndex >= 0) {
            return tabbedPane.getComponentAt(selectedIndex) as? EnhancedChatPanel
        }
        return null
    }

    /**
     * 发送消息
     *
     * @param message 消息内容
     */
    fun sendMessage(message: String) {
        val activePanel = getActiveTabPanel() ?: addNewChatTab()
        activePanel.sendUserMessage(message)
    }
}
