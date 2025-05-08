package ai.kastrax.codex.actions

import ai.kastrax.codex.model.CodeContext
import ai.kastrax.codex.model.CodeContextBuilder
import ai.kastrax.codex.service.AgentConfig
import ai.kastrax.codex.service.AgentType
import ai.kastrax.codex.service.CodexAgentService
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing

/**
 * 生成代码动作，用于在编辑器中生成代码
 */
class GenerateCodeAction : AnAction() {
    
    private val logger = Logger.getInstance(GenerateCodeAction::class.java)
    private val coroutineScope = CoroutineScope(Dispatchers.Swing)
    
    /**
     * 执行动作
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        
        // 获取选中的文本
        val selectedText = editor.selectionModel.selectedText ?: ""
        
        // 获取文件信息
        val virtualFile = FileDocumentManager.getInstance().getFile(editor.document) ?: return
        val fileName = virtualFile.name
        val fileExtension = virtualFile.extension ?: ""
        
        // 确定编程语言
        val language = determineLanguage(fileExtension, psiFile.language.displayName)
        
        // 获取完整代码
        val fullCode = editor.document.text
        
        // 构建代码上下文
        val codeContext = CodeContextBuilder()
            .fileName(fileName)
            .language(language)
            .code(fullCode)
            .selection(selectedText)
            .build()
        
        // 获取任务描述
        val task = Messages.showInputDialog(
            project,
            "请描述您想要执行的任务:",
            "生成代码",
            Messages.getQuestionIcon()
        ) ?: return
        
        // 获取智能体服务
        val agentService = project.service<CodexAgentService>()
        
        // 在协程中执行
        coroutineScope.launch {
            try {
                // 创建智能体
                val agent = agentService.createProgrammingAgent(
                    AgentConfig(
                        name = "CodeGenerationAgent",
                        type = AgentType.CREATIVE,
                        instructions = "你是一个代码生成专家，专注于生成高质量、符合项目风格的代码。",
                        apiKey = "your-api-key-here" // 实际应用中应从设置中获取
                    )
                )
                
                // 发送代码上下文
                val updatedContext = CodeContextBuilder()
                    .fileName(codeContext.fileName)
                    .language(codeContext.language)
                    .code(codeContext.code)
                    .selection(codeContext.selection)
                    .task(task)
                    .build()
                
                val response = agentService.sendCodeContext(agent, updatedContext)
                
                // 处理响应
                val generatedCode = extractCodeFromResponse(response.text, language)
                
                // 插入生成的代码
                insertGeneratedCode(editor, generatedCode)
            } catch (e: Exception) {
                logger.error("生成代码失败", e)
                Messages.showErrorDialog(project, "生成代码失败: ${e.message}", "错误")
            }
        }
    }
    
    /**
     * 更新动作状态
     */
    override fun update(e: AnActionEvent) {
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        
        e.presentation.isEnabled = project != null && editor != null
    }
    
    /**
     * 获取动作更新线程
     */
    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.EDT
    }
    
    /**
     * 确定编程语言
     */
    private fun determineLanguage(fileExtension: String, displayName: String): String {
        return when (fileExtension.lowercase()) {
            "java" -> "java"
            "kt" -> "kotlin"
            "py" -> "python"
            "js" -> "javascript"
            "ts" -> "typescript"
            "html" -> "html"
            "css" -> "css"
            "xml" -> "xml"
            "json" -> "json"
            "md" -> "markdown"
            "go" -> "go"
            "rb" -> "ruby"
            "php" -> "php"
            "swift" -> "swift"
            "c" -> "c"
            "cpp", "cc" -> "cpp"
            "cs" -> "csharp"
            "sh" -> "bash"
            "sql" -> "sql"
            "yaml", "yml" -> "yaml"
            else -> displayName.lowercase()
        }
    }
    
    /**
     * 从响应中提取代码
     */
    private fun extractCodeFromResponse(response: String, language: String): String {
        // 查找代码块
        val codeBlockRegex = "```(?:$language)?\\s*([\\s\\S]*?)```".toRegex()
        val matchResult = codeBlockRegex.find(response)
        
        return if (matchResult != null) {
            matchResult.groupValues[1].trim()
        } else {
            // 如果没有找到代码块，返回整个响应
            response
        }
    }
    
    /**
     * 插入生成的代码
     */
    private fun insertGeneratedCode(editor: Editor, code: String) {
        val document = editor.document
        val project = editor.project ?: return
        
        // 确定插入位置
        val insertPosition = if (editor.selectionModel.hasSelection()) {
            // 如果有选中文本，替换选中的文本
            val selectionStart = editor.selectionModel.selectionStart
            val selectionEnd = editor.selectionModel.selectionEnd
            document.replaceString(selectionStart, selectionEnd, code)
            selectionStart
        } else {
            // 如果没有选中文本，在光标位置插入
            val caretOffset = editor.caretModel.offset
            document.insertString(caretOffset, code)
            caretOffset
        }
        
        // 移动光标到插入位置之后
        editor.caretModel.moveToOffset(insertPosition + code.length)
    }
}
