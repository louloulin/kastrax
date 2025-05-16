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
 * 生成代码动作
 * 
 * 在编辑器右键菜单中添加"使用 KastraX 生成代码"选项
 */
class GenerateCodeAction : AnAction() {
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    
    /**
     * 执行动作
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        // 获取选中的文本
        val selectedText = editor.selectionModel.selectedText
        
        // 显示输入对话框
        val prompt = Messages.showInputDialog(
            project,
            "请输入代码生成提示：",
            "KastraX 代码生成",
            null
        ) ?: return
        
        // 构建完整提示
        val fullPrompt = if (selectedText.isNullOrBlank()) {
            prompt
        } else {
            """
            基于以下代码生成：
            
            ```
            $selectedText
            ```
            
            提示：$prompt
            """.trimIndent()
        }
        
        // 创建用户消息
        val userMessage = ChatMessage(
            role = MessageRole.USER,
            content = fullPrompt
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
                
                // 推断语言
                val language = inferLanguageFromEditor(editor, project)
                
                // 生成代码
                val generatedCode = codeAgent.generateCode(fullPrompt, language)
                
                // 创建助手回复消息
                val assistantMessage = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = generatedCode
                )
                
                // 添加到会话
                conversation.addMessage(assistantMessage)
                
                // 如果用户希望，可以将生成的代码插入到编辑器
                ApplicationManager.getApplication().invokeLater {
                    val insertCode = Messages.showYesNoDialog(
                        project,
                        "是否将生成的代码插入到编辑器？",
                        "KastraX 代码生成",
                        "插入",
                        "取消",
                        null
                    )
                    
                    if (insertCode == Messages.YES) {
                        insertCodeToEditor(editor, generatedCode)
                    }
                }
                
            } catch (ex: Exception) {
                // 显示错误消息
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "生成代码时出错：${ex.message}",
                        "KastraX 代码生成错误"
                    )
                }
            }
        }
    }
    
    /**
     * 从编辑器推断语言
     */
    private fun inferLanguageFromEditor(editor: Editor, project: Project): String {
        val file = CommonDataKeys.VIRTUAL_FILE.getData(
            com.intellij.openapi.actionSystem.DataManager.getInstance().getDataContext(editor.component)
        )
        
        return when (file?.extension?.lowercase()) {
            "kt" -> "kotlin"
            "java" -> "java"
            "py" -> "python"
            "js" -> "javascript"
            "ts" -> "typescript"
            "html" -> "html"
            "css" -> "css"
            "json" -> "json"
            "xml" -> "xml"
            else -> "kotlin" // 默认使用 Kotlin
        }
    }
    
    /**
     * 将代码插入到编辑器
     */
    private fun insertCodeToEditor(editor: Editor, code: String) {
        ApplicationManager.getApplication().runWriteAction {
            // 提取代码块中的代码
            val codeBlockPattern = "```(\\w*)\\s*\\n([\\s\\S]*?)```".toRegex()
            val matchResult = codeBlockPattern.find(code)
            
            val codeToInsert = if (matchResult != null) {
                matchResult.groupValues[2]
            } else {
                code
            }
            
            // 插入代码
            val selectionModel = editor.selectionModel
            val document = editor.document
            
            if (selectionModel.hasSelection()) {
                document.replaceString(
                    selectionModel.selectionStart,
                    selectionModel.selectionEnd,
                    codeToInsert
                )
            } else {
                val caretOffset = editor.caretModel.offset
                document.insertString(caretOffset, codeToInsert)
            }
        }
    }
}
