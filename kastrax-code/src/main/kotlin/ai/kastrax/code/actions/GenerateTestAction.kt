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
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileTypes.FileTypeManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.Messages
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileFactory
import com.intellij.psi.PsiManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.swing.Swing
import java.io.File

/**
 * 生成测试动作
 * 
 * 在编辑器右键菜单中添加"使用 KastraX 生成测试"选项
 */
class GenerateTestAction : AnAction() {
    
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Swing)
    
    /**
     * 执行动作
     */
    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) ?: return
        
        // 获取选中的文本或整个文件内容
        val selectedText = editor.selectionModel.selectedText
        val codeToTest = if (selectedText.isNullOrBlank()) {
            editor.document.text
        } else {
            selectedText
        }
        
        if (codeToTest.isBlank()) {
            Messages.showWarningDialog(
                project,
                "没有可测试的代码",
                "KastraX 测试生成"
            )
            return
        }
        
        // 选择测试框架
        val frameworks = arrayOf("JUnit", "TestNG", "Spock", "Kotest", "其他")
        val frameworkIndex = Messages.showChooseDialog(
            project,
            "请选择测试框架：",
            "KastraX 测试生成",
            null,
            frameworks,
            frameworks[0]
        )
        
        if (frameworkIndex < 0) return
        val framework = frameworks[frameworkIndex]
        
        // 构建提示
        val prompt = """
            请为以下代码生成 $framework 测试：
            
            ```
            $codeToTest
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
                
                // 生成测试代码
                val testCode = codeAgent.generateTest(codeToTest, framework)
                
                // 创建助手回复消息
                val assistantMessage = ChatMessage(
                    role = MessageRole.ASSISTANT,
                    content = testCode
                )
                
                // 添加到会话
                conversation.addMessage(assistantMessage)
                
                // 询问用户是否创建测试文件
                ApplicationManager.getApplication().invokeLater {
                    val createFile = Messages.showYesNoDialog(
                        project,
                        "是否创建测试文件？",
                        "KastraX 测试生成",
                        "创建",
                        "取消",
                        null
                    )
                    
                    if (createFile == Messages.YES) {
                        createTestFile(project, psiFile, testCode)
                    }
                }
                
            } catch (ex: Exception) {
                // 显示错误消息
                ApplicationManager.getApplication().invokeLater {
                    Messages.showErrorDialog(
                        project,
                        "生成测试时出错：${ex.message}",
                        "KastraX 测试生成错误"
                    )
                }
            }
        }
    }
    
    /**
     * 创建测试文件
     */
    private fun createTestFile(project: Project, psiFile: PsiFile, testCode: String) {
        // 提取代码块中的代码
        val codeBlockPattern = "```(\\w*)\\s*\\n([\\s\\S]*?)```".toRegex()
        val matchResult = codeBlockPattern.find(testCode)
        
        val codeToInsert = if (matchResult != null) {
            matchResult.groupValues[2]
        } else {
            testCode
        }
        
        // 确定测试文件名
        val originalFileName = psiFile.name
        val originalFileNameWithoutExt = originalFileName.substringBeforeLast(".")
        val extension = when {
            originalFileName.endsWith(".kt") -> "kt"
            originalFileName.endsWith(".java") -> "java"
            else -> "kt" // 默认使用 Kotlin
        }
        
        val testFileName = "${originalFileNameWithoutExt}Test.$extension"
        
        // 确定测试文件路径
        val originalFilePath = psiFile.virtualFile.path
        val originalDir = File(originalFilePath).parentFile
        
        // 检查是否有 test 目录
        val testDir = findTestDirectory(originalDir)
        val testFile = File(testDir, testFileName)
        
        // 创建测试文件
        ApplicationManager.getApplication().runWriteAction {
            try {
                // 确保目录存在
                testDir.mkdirs()
                
                // 写入文件
                testFile.writeText(codeToInsert)
                
                // 刷新文件系统
                val virtualFile = LocalFileSystem.getInstance().refreshAndFindFileByIoFile(testFile)
                if (virtualFile != null) {
                    // 打开文件
                    FileEditorManager.getInstance(project).openFile(virtualFile, true)
                }
            } catch (ex: Exception) {
                Messages.showErrorDialog(
                    project,
                    "创建测试文件时出错：${ex.message}",
                    "KastraX 测试生成错误"
                )
            }
        }
    }
    
    /**
     * 查找测试目录
     */
    private fun findTestDirectory(originalDir: File): File {
        // 尝试找到标准的测试目录
        val projectDir = findProjectRoot(originalDir)
        
        // 检查是否有 src/test 目录
        val srcTestDir = File(projectDir, "src/test")
        if (srcTestDir.exists() && srcTestDir.isDirectory) {
            // 检查是否有对应的语言目录
            val kotlinTestDir = File(srcTestDir, "kotlin")
            if (kotlinTestDir.exists() && kotlinTestDir.isDirectory) {
                return kotlinTestDir
            }
            
            val javaTestDir = File(srcTestDir, "java")
            if (javaTestDir.exists() && javaTestDir.isDirectory) {
                return javaTestDir
            }
            
            return srcTestDir
        }
        
        // 如果没有标准测试目录，则在原目录创建 test 子目录
        return File(originalDir, "test")
    }
    
    /**
     * 查找项目根目录
     */
    private fun findProjectRoot(dir: File): File {
        var current = dir
        
        // 向上查找，直到找到包含 .git、.idea 或 build.gradle 的目录
        while (current.parentFile != null) {
            if (File(current, ".git").exists() || 
                File(current, ".idea").exists() || 
                File(current, "build.gradle").exists() ||
                File(current, "build.gradle.kts").exists()) {
                return current
            }
            current = current.parentFile
        }
        
        // 如果找不到项目根目录，则返回原目录
        return dir
    }
}
