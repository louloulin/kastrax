package ai.kastrax.code.actions

import ai.kastrax.code.model.ChatMessage
import ai.kastrax.code.model.MessageRole
import ai.kastrax.code.service.CodeAgentService
import ai.kastrax.code.service.ConversationService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.wm.ToolWindowManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

/**
 * 重构代码动作
 * 
 * 在编辑器右键菜单中添加"使用 KastraX 重构代码"选项
 */
class RefactorCodeAction : AnAction() {
    
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
                "请先选择要重构的代码",
                "KastraX 代码重构"
            )
            return
        }
        
        // 显示输入对话框
        val instructions = Messages.showInputDialog(
            project,
            "请输入重构指令：",
            "KastraX 代码重构",
            null
        ) ?: return
        
        // 构建提示
        val prompt = """
            请根据以下指令重构代码：
            
            ```
            $selectedText
            ```
            
            重构指令：$instructions
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
                
                // 重构代码
                val refactoredCode = codeAgent.refactorCode(selectedText, instructions)
                
                // 创建助手回复消息
                val assistantMessage = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = refactoredCode
                )
                
                // 添加到会话
                conversation.addMessage(assistantMessage)
                
                // 如果用户希望，可以将重构后的代码替换原代码
                ApplicationManager.getApplication().invokeLater {
                    val replaceCode = Messages.showYesNoDialog(
                        project,
                        "是否用重构后的代码替换原代码？",
                        "KastraX 代码重构",
                        "替换",
                        "取消",
                        null
                    )
                    
                    if (replaceCode == Messages.YES) {
                        replaceCodeInEditor(editor, refactoredCode)
                    }
                }
                
            } catch (ex: Exception) {
                // 显示错误消息
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "重构代码时出错：${ex.message}",
                        "KastraX 代码重构错误"
                    )
                }
            }
        }
    }
    
    /**
     * 在编辑器中替换代码
     */
    private fun replaceCodeInEditor(editor: Editor, code: String) {
        ApplicationManager.getApplication().runWriteAction {
            // 提取代码块中的代码
            val codeBlockPattern = "```(\\w*)\\s*\\n([\\s\\S]*?)```".toRegex()
            val matchResult = codeBlockPattern.find(code)
            
            val codeToInsert = if (matchResult != null) {
                matchResult.groupValues[2]
            } else {
                code
            }
            
            // 替换代码
            val selectionModel = editor.selectionModel
            val document = editor.document
            
            if (selectionModel.hasSelection()) {
                document.replaceString(
                    selectionModel.selectionStart,
                    selectionModel.selectionEnd,
                    codeToInsert
                )
            }
        }
    }
}
