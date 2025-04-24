package ai.kastrax.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import ai.kastrax.cli.commands.NewCommand
import ai.kastrax.cli.commands.PlaygroundCommand
import ai.kastrax.cli.commands.DeployCommand
import ai.kastrax.cli.commands.DevCommand
import ai.kastrax.cli.commands.CreateCommand
import com.github.ajalt.mordant.rendering.TextColors
import com.github.ajalt.mordant.terminal.Terminal
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

/**
 * KastraX CLI 主命令。
 */
class KastraXCli : CliktCommand(
    name = "kastrax",
    help = "KastraX CLI - 用于创建和管理 KastraX 项目的命令行工具"
) {
    private val terminal = Terminal()

    init {
        subcommands(
            NewCommand(),
            PlaygroundCommand(),
            DeployCommand(),
            DevCommand(),
            CreateCommand()
        )
    }

    override fun run() {
        if (currentContext.invokedSubcommand == null) {
            showBanner()
            echo("使用 --help 查看帮助信息")
        }
    }

    private fun showBanner() {
        val banner = """
            ██╗  ██╗ █████╗ ███████╗████████╗██████╗  █████╗ ██╗  ██╗
            ██║ ██╔╝██╔══██╗██╔════╝╚══██╔══╝██╔══██╗██╔══██╗╚██╗██╔╝
            █████╔╝ ███████║███████╗   ██║   ██████╔╝███████║ ╚███╔╝
            ██╔═██╗ ██╔══██║╚════██║   ██║   ██╔══██╗██╔══██║ ██╔██╗
            ██║  ██╗██║  ██║███████║   ██║   ██║  ██║██║  ██║██╔╝ ██╗
            ╚═╝  ╚═╝╚═╝  ╚═╝╚══════╝   ╚═╝   ╚═╝  ╚═╝╚═╝  ╚═╝╚═╝  ╚═╝

            KastraX CLI v0.1.0 - 用于创建和管理 KastraX 项目的命令行工具
        """.trimIndent()

        terminal.println(TextColors.brightBlue(banner))
        terminal.println()
    }
}

/**
 * CLI 入口点。
 */
fun main(args: Array<String>) {
    try {
        KastraXCli().main(args)
    } catch (e: Exception) {
        logger.error(e) { "CLI 执行过程中发生错误" }
        Terminal().println(TextColors.red("错误: ${e.message}"))
        System.exit(1)
    }
}
