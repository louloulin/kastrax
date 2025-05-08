package ai.kastrax.codex.actions

import ai.kastrax.codex.service.AgentType
import ai.kastrax.codex.service.CodexService
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.ui.popup.JBPopupFactory
import com.intellij.openapi.util.TextRange
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * 生成代码 Action，用于测试 CodexAgent 的代码生成功能
 */
class GenerateCodeAction : AnAction("Generate Code with Kastrax Agent") {
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        
        // 获取选中的文本
        val selectedText = editor.selectionModel.selectedText
        
        // 如果没有选中文本，弹出输入对话框
        val prompt = if (selectedText.isNullOrBlank()) {
            Messages.showInputDialog(
                project,
                "请输入代码生成描述：",
                "生成代码",
                Messages.getQuestionIcon()
            ) ?: return
        } else {
            // 如果选中了文本，弹出确认对话框
            val options = arrayOf("生成代码", "解释代码", "取消")
            val choice = Messages.showDialog(
                project,
                "请选择操作：",
                "Kastrax Agent",
                options,
                0,
                Messages.getQuestionIcon()
            )
            
            when (choice) {
                0 -> "根据以下描述生成代码：\n$selectedText"
                1 -> selectedText
                else -> return
            }
        }
        
        // 显示加载指示器
        val popup = JBPopupFactory.getInstance()
            .createBalloonBuilder(Messages.configureMessagePaneUi(null, "正在生成..."))
            .setFadeoutTime(3000)
            .createBalloon()
        
        popup.show(
            JBPopupFactory.getInstance().guessBestPopupLocation(editor),
            com.intellij.openapi.ui.popup.Balloon.Position.below
        )
        
        // 调用 CodexService 生成代码
        coroutineScope.launch {
            try {
                val service = CodexService.getInstance(project)
                val response = service.generateCodeCompletion(prompt)
                
                // 在 UI 线程中更新编辑器
                withContext(Dispatchers.Main) {
                    popup.hide()
                    insertGeneratedCode(project, editor, response.text)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    popup.hide()
                    Messages.showErrorDialog(
                        project,
                        "生成代码时出错：${e.message}",
                        "错误"
                    )
                }
            }
        }
    }
    
    /**
     * 插入生成的代码
     */
    private fun insertGeneratedCode(project: Project, editor: Editor, generatedCode: String) {
        // 提取代码块
        val codeBlockRegex = "```(?:\\w+)?\\s*\\n([\\s\\S]*?)\\n```".toRegex()
        val codeMatch = codeBlockRegex.find(generatedCode)
        
        val codeToInsert = codeMatch?.groupValues?.get(1) ?: generatedCode
        
        // 在编辑器中插入代码
        WriteCommandAction.runWriteCommandAction(project) {
            val document = editor.document
            val caretModel = editor.caretModel
            val offset = caretModel.offset
            
            document.insertString(offset, codeToInsert)
            caretModel.moveToOffset(offset + codeToInsert.length)
        }
    }
    
    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        
        e.presentation.isEnabled = project != null && editor != null
    }
}
