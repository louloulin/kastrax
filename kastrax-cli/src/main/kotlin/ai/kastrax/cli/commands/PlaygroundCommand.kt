package ai.kastrax.cli.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.help
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.int
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import io.github.oshai.kotlinlogging.KotlinLogging
import java.io.File
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

/**
 * 启动交互式游乐场的命令。
 */
class PlaygroundCommand : CliktCommand(
    name = "playground",
    help = "启动 KastraX 交互式游乐场"
) {
    private val port by option("-p", "--port")
        .int()
        .default(8080)
        .help("游乐场服务器端口")
    
    private val configFile by option("-c", "--config")
        .default("kastrax.json")
        .help("配置文件路径")
    
    private val verbose by option("-v", "--verbose")
        .flag()
        .help("启用详细日志")
    
    private val terminal = Terminal()
    
    override fun run() = runBlocking {
        // 检查配置文件是否存在
        val config = File(configFile)
        if (!config.exists()) {
            terminal.println(TextColors.yellow("警告: 配置文件 '$configFile' 不存在。将使用默认配置。"))
        }
        
        terminal.println(TextColors.brightBlue("启动 KastraX 交互式游乐场..."))
        terminal.println("端口: $port")
        terminal.println("配置文件: ${config.absolutePath}")
        terminal.println("详细日志: ${if (verbose) "启用" else "禁用"}")
        terminal.println()
        
        try {
            // 这里是游乐场服务器的实现
            // 由于这是一个示例，我们只是模拟启动服务器
            terminal.println(TextColors.green("✓ 游乐场服务器已启动"))
            terminal.println("  访问地址: http://localhost:$port")
            terminal.println()
            terminal.println("按 Ctrl+C 停止服务器")
            
            // 模拟服务器运行
            while (true) {
                // 在实际实现中，这里会启动一个 Web 服务器
                // 并等待用户请求
                Thread.sleep(1000)
            }
        } catch (e: InterruptedException) {
            terminal.println()
            terminal.println(TextColors.green("✓ 游乐场服务器已停止"))
        } catch (e: Exception) {
            logger.error(e) { "启动游乐场服务器时发生错误" }
            terminal.println(TextColors.red("错误: 启动游乐场服务器时发生错误: ${e.message}"))
        }
    }
}
