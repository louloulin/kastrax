package ai.kastrax.graal

import ai.kastrax.core.kastrax
import ai.kastrax.integrations.deepseek.DeepSeekException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

private val logger = KotlinLogging.logger {}

// 加载配置
val config = loadConfig()

// 使用 KastraX DSL 创建实例
val kastraxInstance = kastrax {
    // 这里可以注册代理
}

/**
 * KastraX GraalVM Native应用程序入口点
 */
fun main(args: Array<String>) {
    logger.info { "启动 KastraX GraalVM Native 应用程序..." }
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
 * 获取平台信息
 */
fun getPlatformInfo(): String {
    val os = System.getProperty("os.name")
    val version = System.getProperty("os.version")
    val arch = System.getProperty("os.arch")
    val javaVersion = System.getProperty("java.version")
    
    return "运行平台: $os $version ($arch), Java版本: $javaVersion"
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
    println("欢迎使用 KastraX GraalVM Native 命令行界面！")
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
        KastraX GraalVM Native 应用程序

        用法:
          kastrax [命令]

        命令:
          server    启动服务器模式
          cli       启动命令行界面模式
          config    显示当前配置
          help      显示帮助信息

        示例:
          kastrax server
    """.trimIndent())
}

/**
 * 应用程序配置
 */
@Serializable
data class AppConfig(
    val appName: String = "KastraX GraalVM Native",
    val version: String = "0.1.0",
    val logging: Logging = Logging(),
    val apiKeys: ApiKeys = ApiKeys()
) {
    @Serializable
    data class Logging(
        val level: String = "INFO",
        val file: String = "logs/kastrax.log"
    )

    @Serializable
    data class ApiKeys(
        val deepseek: String = "",
        val anthropic: String = "",
        val openai: String = ""
    )
}

/**
 * 加载配置
 */
fun loadConfig(): AppConfig {
    val configDir = File("config")
    if (!configDir.exists()) {
        configDir.mkdirs()
    }

    val configFile = File(configDir, "kastrax.json")
    
    return if (configFile.exists()) {
        try {
            Json.decodeFromString<AppConfig>(configFile.readText())
        } catch (e: Exception) {
            logger.error(e) { "加载配置文件失败，使用默认配置: ${e.message}" }
            val defaultConfig = AppConfig()
            saveConfig(defaultConfig)
            defaultConfig
        }
    } else {
        logger.info { "配置文件不存在，创建默认配置" }
        val defaultConfig = AppConfig()
        saveConfig(defaultConfig)
        defaultConfig
    }
}

/**
 * 保存配置
 */
fun saveConfig(config: AppConfig) {
    val configDir = File("config")
    if (!configDir.exists()) {
        configDir.mkdirs()
    }

    val configFile = File(configDir, "kastrax.json")
    val json = Json { prettyPrint = true }
    configFile.writeText(json.encodeToString(config))
}
