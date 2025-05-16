package ai.kastrax.code.actions

import ai.kastrax.code.model.ChatMessage
import ai.kastrax.code.model.DetailLevel
import ai.kastrax.code.model.MessageRole
import ai.kastrax.code.service.CodeAgentService
import ai.kastrax.code.service.ConversationService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

/**
 * 解释代码动作
 * 
 * 在编辑器右键菜单中添加"使用 KastraX 解释代码"选项
 */
class ExplainCodeAction : AnAction() {
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    
    /**
     * 执行动作
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        // 获取选中的文本
        val selectedText = editor.selectionModel.selectedText
        if (selectedText.isNullOrBlank()) {
            Messages.showWarningDialog(
                project,
                "请先选择要解释的代码",
                "KastraX 代码解释"
            )
            return
        }
        
        // 选择详细程度
        val detailLevels = arrayOf("基础", "详细", "全面")
        val detailLevel = Messages.showChooseDialog(
            project,
            "请选择解释的详细程度：",
            "KastraX 代码解释",
            null,
            detailLevels,
            detailLevels[1]
        )
        
        if (detailLevel < 0) return
        
        // 构建提示
        val prompt = """
            请解释以下代码：
            
            ```
            $selectedText
            ```
        """.trimIndent()
        
        // 创建用户消息
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = prompt
        )
        
        // 添加到会话
        val conversationService = ConversationService.getInstance(project)
        val conversation = conversationService.getCurrentConversation()
        conversation.addMessage(userMessage)
        
        // 显示工具窗口
        ApplicationManager.getApplication().invokeLater {
            val toolWindow = ToolWindowManager.getInstance(project).getToolWindow("KastraX Code")
            toolWindow?.show()
        }
        
        // 调用代码智能体
        coroutineScope.launch {
            try {
                val codeAgent = CodeAgentService.getInstance(project).getCodeAgent()
                
                // 转换详细程度
                val level = when (detailLevel) {
                    0 -> DetailLevel.BASIC
                    1 -> DetailLevel.DETAILED
                    2 -> DetailLevel.COMPREHENSIVE
                    else -> DetailLevel.DETAILED
                }
                
                // 解释代码
                val explanation = codeAgent.explainCode(selectedText, level)
                
                // 创建助手回复消息
                val assistantMessage = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = explanation
                )
                
                // 添加到会话
                conversation.addMessage(assistantMessage)
                
            } catch (ex: Exception) {
                // 显示错误消息
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "解释代码时出错：${ex.message}",
                        "KastraX 代码解释错误"
                    )
                }
            }
        }
    }
}
