package ai.kastrax.native

import ai.kastrax.core.kastrax
import ai.kastrax.integrations.deepseek.DeepSeekException
import ai.kastrax.native.config.loadConfig
import ai.kastrax.native.config.saveConfig
import ai.kastrax.native.config.AppConfig
import ai.kastrax.native.network.createHttpClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

// 加载配置
val config = loadConfig()

// 使用 KastraX DSL 创建实例
val kastraxInstance = kastrax {
    // 这里可以注册代理
}

/**
 * KastraX Native应用程序的JVM入口点
 */
fun main(args: Array<String>) {
    logger.info { "启动 KastraX Native 应用程序 (JVM版本)..." }
    logger.info { "应用名称: ${config.appName} v${config.version}" }
    logger.info { getPlatformInfo() }

    try {
        // 解析命令行参数
        val command = if (args.isNotEmpty()) args[0] else "help"

        when (command) {
            "server" -> startServer()
            "cli" -> startCli()
            "config" -> showConfig()
            else -> printHelp()
        }
    } catch (e: DeepSeekException) {
        logger.error(e) { "DeepSeek API 错误: ${e.message}" }
        println("DeepSeek API 错误: ${e.message}")
        println("请检查 API 密钥是否正确，或者尝试增加超时时间")
    } catch (e: Exception) {
        logger.error(e) { "启动应用程序时发生错误: ${e.message}" }
        println("启动应用程序时发生错误: ${e.message}")
    }
}

/**
 * 启动服务器模式
 */
private fun startServer() {
    logger.info { "启动服务器模式..." }
    // 在这里实现服务器启动逻辑
    println("服务器模式尚未实现")
}

/**
 * 启动命令行界面模式
 */
private fun startCli() {
    logger.info { "启动命令行界面模式..." }
    println("欢迎使用 KastraX Native 命令行界面 (JVM版本)！")
    println("输入 'exit' 或 'quit' 退出\n")

    // 简单的命令行交互
    var running = true
    while (running) {
        print("\n请输入命令: ")

        val input = readLine() ?: ""

        when (input.trim().lowercase()) {
            "exit", "quit" -> {
                println("再见！")
                running = false
            }
            "help" -> printHelp()
            else -> {
                println("处理命令: $input")
                println("命令执行完成")
            }
        }
    }
}

/**
 * 显示配置信息
 */
private fun showConfig() {
    logger.info { "显示当前配置" }
    println("当前配置:")
    println("应用名称: ${config.appName}")
    println("版本: ${config.version}")
    println("日志级别: ${config.logging.level}")
    println("API密钥:")
    println("  DeepSeek: ${if (config.apiKeys.deepseek.isNotEmpty()) "已设置" else "未设置"}")
    println("  Anthropic: ${if (config.apiKeys.anthropic.isNotEmpty()) "已设置" else "未设置"}")
    println("  OpenAI: ${if (config.apiKeys.openai.isNotEmpty()) "已设置" else "未设置"}")
}

/**
 * 打印帮助信息
 */
private fun printHelp() {
    println("""
        KastraX Native 应用程序 (JVM版本)

        用法:
          java -jar kastrax-native.jar [命令]

        命令:
          server    启动服务器模式
          cli       启动命令行界面模式
          config    显示当前配置
          help      显示帮助信息

        示例:
          java -jar kastrax-native.jar server
    """.trimIndent())
}
