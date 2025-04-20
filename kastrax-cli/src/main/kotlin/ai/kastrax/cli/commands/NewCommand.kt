package ai.kastrax.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.help
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.choice
import com.github.ajalt.mordant.animation.progressAnimation
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import ai.kastrax.cli.templates.ProjectTemplate
import ai.kastrax.cli.templates.SimpleProjectTemplate
import ai.kastrax.cli.templates.RagProjectTemplate
import ai.kastrax.cli.templates.AgentProjectTemplate
import ai.kastrax.cli.templates.WorkflowProjectTemplate
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

/**
 * 创建新项目的命令。
 */
class NewCommand : CliktCommand(
    name = "new",
    help = "创建一个新的 KastraX 项目"
) {
    private val projectName by argument()
        .help("项目名称")
    
    private val template by option("-t", "--template")
        .choice(
            "simple" to "simple",
            "rag" to "rag",
            "agent" to "agent",
            "workflow" to "workflow"
        )
        .default("simple")
        .help("项目模板类型 (simple, rag, agent, workflow)")
    
    private val outputDir by option("-o", "--output-dir")
        .default(".")
        .help("输出目录")
    
    private val force by option("-f", "--force")
        .flag()
        .help("强制覆盖现有目录")
    
    private val terminal = Terminal()
    
    override fun run() {
        val projectDir = File(outputDir, projectName)
        
        // 检查目录是否已存在
        if (projectDir.exists() && !force) {
            terminal.println(TextColors.red("错误: 目录 '$projectDir' 已存在。使用 --force 选项覆盖。"))
            return
        }
        
        // 创建项目模板
        val projectTemplate = createProjectTemplate()
        
        // 显示进度条
        val progress = terminal.progressAnimation {
            text("创建项目...")
            percentage()
            completed()
            speed("文件/秒")
        }
        
        progress.start()
        
        try {
            // 确保目录存在
            if (!projectDir.exists()) {
                projectDir.mkdirs()
            }
            
            // 生成项目文件
            val fileCount = projectTemplate.generate(projectDir.toPath())
            
            progress.update(completed = 100, total = 100)
            progress.stop()
            
            terminal.println(TextColors.green("✓ 成功创建项目 '$projectName'"))
            terminal.println("  生成了 $fileCount 个文件")
            terminal.println("  项目位置: ${projectDir.absolutePath}")
            terminal.println()
            terminal.println("下一步:")
            terminal.println("  cd ${projectDir.name}")
            terminal.println("  ./gradlew build")
            terminal.println("  ./gradlew run")
        } catch (e: Exception) {
            progress.stop()
            logger.error(e) { "创建项目时发生错误" }
            terminal.println(TextColors.red("错误: 创建项目时发生错误: ${e.message}"))
        }
    }
    
    private fun createProjectTemplate(): ProjectTemplate {
        return when (template) {
            "simple" -> SimpleProjectTemplate(projectName)
            "rag" -> RagProjectTemplate(projectName)
            "agent" -> AgentProjectTemplate(projectName)
            "workflow" -> WorkflowProjectTemplate(projectName)
            else -> SimpleProjectTemplate(projectName)
        }
    }
}
