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

// 创建一个简单的计算器工具
val calculatorTool = ai.kastrax.core.tools.tool {
    id = "calculator"
    name = "calculator"
    description = "执行简单的数学计算，支持加减乘除"

    // 设置输入和输出模式
    inputSchema = ai.kastrax.core.tools.jsonObject {
        "type" to "string"
        "description" to "数学表达式，如 2+2"
    }

    outputSchema = ai.kastrax.core.tools.jsonObject {
        "type" to "string"
        "description" to "计算结果"
    }

    execute = { input ->
        val inputStr = (input as? kotlinx.serialization.json.JsonPrimitive)?.content ?: ""
        val result = try {
            // 简单的计算器实现
            val trimmedInput = inputStr.trim()

            if (trimmedInput.contains('+')) {
                val parts = trimmedInput.split('+')
                if (parts.size == 2) {
                    val a = parts[0].trim().toDoubleOrNull()
                    val b = parts[1].trim().toDoubleOrNull()
                    if (a != null && b != null) {
                        (a + b).toString()
                    } else {
                        "无法解析数字"
                    }
                } else {
                    "格式错误"
                }
            } else if (trimmedInput.contains('-')) {
                val parts = trimmedInput.split('-')
                if (parts.size == 2) {
                    val a = parts[0].trim().toDoubleOrNull()
                    val b = parts[1].trim().toDoubleOrNull()
                    if (a != null && b != null) {
                        (a - b).toString()
                    } else {
                        "无法解析数字"
                    }
                } else {
                    "格式错误"
                }
            } else if (trimmedInput.contains('*')) {
                val parts = trimmedInput.split('*')
                if (parts.size == 2) {
                    val a = parts[0].trim().toDoubleOrNull()
                    val b = parts[1].trim().toDoubleOrNull()
                    if (a != null && b != null) {
                        (a * b).toString()
                    } else {
                        "无法解析数字"
                    }
                } else {
                    "格式错误"
                }
            } else if (trimmedInput.contains('/')) {
                val parts = trimmedInput.split('/')
                if (parts.size == 2) {
                    val a = parts[0].trim().toDoubleOrNull()
                    val b = parts[1].trim().toDoubleOrNull()
                    if (a != null && b != null) {
                        if (b == 0.0) {
                            "错误：除数不能为零"
                        } else {
                            (a / b).toString()
                        }
                    } else {
                        "无法解析数字"
                    }
                } else {
                    "格式错误"
                }
            } else {
                "无法解析表达式：$trimmedInput"
            }
        } catch (e: Exception) {
            "计算错误：${e.message}"
        }

        kotlinx.serialization.json.JsonPrimitive(result)
    }
}

/**
 * KastraX GraalVM Native应用程序入口点
 */
fun main(args: Array<String>) {
    // 初始化序列化模块
    ai.kastrax.graal.serialization.SerializationInitializer.initialize()

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
            "deepseek" -> startDeepSeekAgent()
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
 * 创建一个简单的Agent
 */
private fun createSimpleAgent(): ai.kastrax.core.agent.Agent {
    return ai.kastrax.core.agent.agent {
        name = "calculator-agent"
        instructions = "你是一个数学助手，可以帮助用户进行计算。"
        model = ai.kastrax.integrations.deepseek.DeepSeekProvider(
            model = "deepseek-coder",
            apiKey = config.apiKeys.deepseek
        )
        tools {
            tool(calculatorTool)
        }
    }
}

/**
 * 启动命令行界面模式
 */
private fun startCli() {
    logger.info { "启动命令行界面模式..." }
    println("欢迎使用 KastraX GraalVM Native 命令行界面！")
    println("输入 'exit' 或 'quit' 退出")
    println("输入 'calc' 进入计算器模式")
    println("输入 'help' 查看帮助\n")

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
            "calc" -> startCalculatorMode()
            else -> {
                println("未知命令: $input")
                println("输入 'help' 查看可用命令")
            }
        }
    }
}

/**
 * 启动计算器模式
 */
private fun startCalculatorMode() {
    println("进入计算器模式，输入数学表达式进行计算")
    println("例如：2+2、2*3、等等")
    println("输入 'back' 返回主菜单\n")

    // 创建一个简单的Agent
    val agent = createSimpleAgent()

    var calculating = true
    while (calculating) {
        print("\n请输入表达式: ")

        val input = readLine() ?: ""

        if (input.trim().lowercase() == "back") {
            println("返回主菜单")
            calculating = false
        } else {
            try {
                // 直接使用计算器工具
                val jsonInput = kotlinx.serialization.json.JsonPrimitive(input)
                val jsonResult = runBlocking { calculatorTool.execute(jsonInput) }
                val result = (jsonResult as? kotlinx.serialization.json.JsonPrimitive)?.content ?: "无法解析结果"
                println("结果: $result")

                // 使用Agent进行更复杂的计算
                if (input.length > 5) { // 假设较复杂的表达式
                    println("正在使用Agent进行分析...")
                    val agentResponse = runBlocking {
                        agent.generate(input)
                    }
                    println("智能助手: $agentResponse")
                }
            } catch (e: Exception) {
                println("计算错误: ${e.message}")
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
 * 启动 DeepSeek Agent
 */
private fun startDeepSeekAgent() {
    logger.info { "启动 DeepSeek Agent..." }

    // 检查 API 密钥
    if (config.apiKeys.deepseek.isEmpty()) {
        println("错误: 未设置 DeepSeek API 密钥")
        println("请在配置文件中设置 DeepSeek API 密钥")
        return
    }

    println("启动 DeepSeek Agent...")

    // 使用简化版本的 DeepSeek Agent 示例
    val apiKey = config.apiKeys.deepseek
    ai.kastrax.graal.agent.SimpleDeepSeekAgent.run(apiKey)
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
          deepseek  启动 DeepSeek Agent 示例
          help      显示帮助信息

        CLI 模式命令:
          calc      进入计算器模式
          help      显示帮助信息
          exit      退出应用程序

        示例:
          kastrax cli
          > calc
          > 2+2
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
