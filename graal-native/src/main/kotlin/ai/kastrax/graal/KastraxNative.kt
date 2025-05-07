package ai.kastrax.graal

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.core.kastrax
import ai.kastrax.core.tools.tool
import ai.kastrax.graal.http.DeepSeekHttpClient
import ai.kastrax.graal.http.NativeDeepSeekProvider
import ai.kastrax.integrations.deepseek.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import java.io.File
import java.time.LocalDateTime
import java.util.Scanner
import kotlin.system.exitProcess

/**
 * KastraX 原生镜像入口点
 * 支持所有 KastraX AI Agent 功能
 */
object KastraxNative {
    // 配置
    private val config = loadConfig()

    // 计算器工具
    private val calculatorTool = tool {
        id = "calculator"
        name = "calculator"
        description = "执行简单的数学计算，支持加减乘除"

        // 设置输入和输出模式
        inputSchema = buildJsonObject {
            put("type", "string")
            put("description", "数学表达式，如 2+2")
        }

        outputSchema = buildJsonObject {
            put("type", "string")
            put("description", "计算结果")
        }

        execute = { input ->
            val inputStr = (input as? JsonPrimitive)?.content ?: ""
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

            JsonPrimitive(result)
        }
    }

    // 天气工具
    private val weatherTool = tool {
        id = "weather"
        name = "weather"
        description = "获取指定城市的天气信息"

        // 设置输入和输出模式
        inputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("city") {
                    put("type", "string")
                    put("description", "城市名称")
                }
            }
            putJsonArray("required") {
                add(JsonPrimitive("city"))
            }
        }

        outputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("temperature") {
                    put("type", "string")
                    put("description", "温度")
                }
                putJsonObject("condition") {
                    put("type", "string")
                    put("description", "天气状况")
                }
                putJsonObject("humidity") {
                    put("type", "string")
                    put("description", "湿度")
                }
            }
        }

        execute = { input ->
            val city = (input as? JsonObject)?.get("city")?.jsonPrimitive?.content ?: "未知城市"

            // 模拟天气数据
            val weatherData = when (city) {
                "北京" -> buildJsonObject {
                    put("temperature", "25°C")
                    put("condition", "晴朗")
                    put("humidity", "45%")
                }
                "上海" -> buildJsonObject {
                    put("temperature", "28°C")
                    put("condition", "多云")
                    put("humidity", "60%")
                }
                "广州" -> buildJsonObject {
                    put("temperature", "30°C")
                    put("condition", "阵雨")
                    put("humidity", "75%")
                }
                else -> buildJsonObject {
                    put("temperature", "未知")
                    put("condition", "未知")
                    put("humidity", "未知")
                }
            }

            weatherData
        }
    }

    // 创建助手代理
    private val assistantAgent = agent {
        name = "助手代理"
        instructions = """
            你是一个有用的助手，可以回答用户的问题并使用工具来获取信息。

            当用户询问数学计算时，使用计算器工具。
            当用户询问天气时，使用天气工具。

            始终以友好、专业的方式回答，并提供准确的信息。
        """.trimIndent()

        model = NativeDeepSeekProvider(
            apiKey = System.getenv("DEEPSEEK_API_KEY") ?: config.apiKeys.deepseek,
            model = DeepSeekModel.DEEPSEEK_CHAT,
            temperature = 0.7,
            maxTokens = 1000,
            timeout = 120000
        )

        tools {
            tool(calculatorTool)
            tool(weatherTool)
        }

        defaultGenerateOptions {
            temperature(0.7)
            maxTokens(1000)
        }
    }

    // 创建专家代理
    private val expertAgent = agent {
        name = "专家代理"
        instructions = """
            你是一个人工智能和机器学习领域的专家。你对深度学习、神经网络、自然语言处理和计算机视觉等领域有深入的了解。

            当用户询问这些领域的问题时，提供详细、准确和最新的信息。使用专业术语，但也要确保解释清楚，使非专业人士也能理解。

            如果你不确定某个问题的答案，坦诚地承认，而不是提供可能不准确的信息。
        """.trimIndent()

        model = NativeDeepSeekProvider(
            apiKey = System.getenv("DEEPSEEK_API_KEY") ?: config.apiKeys.deepseek,
            model = DeepSeekModel.DEEPSEEK_CHAT,
            temperature = 0.3,
            maxTokens = 2000,
            timeout = 120000
        )

        defaultGenerateOptions {
            temperature(0.3)
            maxTokens(2000)
        }
    }

    // 创建KastraX实例
    private val kastraxInstance = kastrax {
        agent("assistant", assistantAgent)
        agent("expert", expertAgent)
    }

    /**
     * 主入口点
     */
    @JvmStatic
    fun main(args: Array<String>) {
        println("KastraX Native Image")
        println("===================")
        println("版本: ${config.version}")
        println("构建时间: ${LocalDateTime.now()}")
        println(getPlatformInfo())
        println()

        try {
            // 初始化序列化模块
            ai.kastrax.graal.serialization.SerializationInitializer.initialize()

            // 解析命令行参数
            val command = if (args.isNotEmpty()) args[0] else "help"

            when (command) {
                "help" -> printHelp()
                "version" -> printVersion()
                "config" -> showConfig()
                "cli" -> startCli()
                "agent" -> {
                    if (args.size > 1) {
                        val agentId = args[1]
                        if (args.size > 2) {
                            val prompt = args.slice(2 until args.size).joinToString(" ")
                            runAgent(agentId, prompt)
                        } else {
                            startAgentInteractive(agentId)
                        }
                    } else {
                        println("错误: 缺少代理ID")
                        println("用法: kastrax agent <agent-id> [prompt]")
                        println("可用代理: assistant, expert")
                    }
                }
                "stream" -> {
                    if (args.size > 1) {
                        val agentId = args[1]
                        if (args.size > 2) {
                            val prompt = args.slice(2 until args.size).joinToString(" ")
                            streamAgent(agentId, prompt)
                        } else {
                            println("错误: 缺少提示")
                            println("用法: kastrax stream <agent-id> <prompt>")
                        }
                    } else {
                        println("错误: 缺少代理ID")
                        println("用法: kastrax stream <agent-id> <prompt>")
                        println("可用代理: assistant, expert")
                    }
                }
                "tools" -> listTools()
                "agents" -> listAgents()
                "calculator" -> startCalculator()
                "weather" -> {
                    if (args.size > 1) {
                        val city = args[1]
                        getWeather(city)
                    } else {
                        println("错误: 缺少城市名称")
                        println("用法: kastrax weather <city>")
                    }
                }
                else -> {
                    println("未知命令: $command")
                    printHelp()
                }
            }
        } catch (e: Exception) {
            println("错误: ${e.message}")
            e.printStackTrace()
        }
    }

    /**
     * 打印帮助信息
     */
    private fun printHelp() {
        println("""
            KastraX 原生镜像 - AI Agent 框架

            用法:
              kastrax [命令] [参数]

            命令:
              help                     显示帮助信息
              version                  显示版本信息
              config                   显示配置信息
              cli                      启动交互式命令行界面
              agent <id> [prompt]      运行指定代理，可选提供提示
              stream <id> <prompt>     以流式方式运行指定代理
              tools                    列出可用工具
              agents                   列出可用代理
              calculator               启动计算器工具
              weather <city>           获取指定城市的天气

            示例:
              kastrax agent assistant "北京今天的天气怎么样？"
              kastrax stream expert "解释一下神经网络的工作原理"
              kastrax weather 北京
        """.trimIndent())
    }

    /**
     * 打印版本信息
     */
    private fun printVersion() {
        println("KastraX 版本: ${config.version}")
        println("构建时间: ${LocalDateTime.now()}")
        println("Java 版本: ${System.getProperty("java.version")}")
        println("OS: ${System.getProperty("os.name")}")
    }

    /**
     * 获取平台信息
     */
    private fun getPlatformInfo(): String {
        val os = System.getProperty("os.name")
        val version = System.getProperty("os.version")
        val arch = System.getProperty("os.arch")
        val javaVersion = System.getProperty("java.version")

        return "运行平台: $os $version ($arch), Java版本: $javaVersion"
    }

    /**
     * 显示配置信息
     */
    private fun showConfig() {
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
     * 启动交互式命令行界面
     */
    private fun startCli() {
        println("欢迎使用 KastraX 命令行界面！")
        println("输入 'exit' 或 'quit' 退出\n")

        val scanner = Scanner(System.`in`)

        while (true) {
            print("\n请选择代理 (assistant/expert): ")
            System.out.flush()

            val agentType = scanner.nextLine().trim().lowercase()

            if (agentType == "exit" || agentType == "quit") {
                println("再见！")
                break
            }

            val agent = when (agentType) {
                "assistant" -> kastraxInstance.getAgent("assistant")
                "expert" -> kastraxInstance.getAgent("expert")
                else -> {
                    println("无效的代理类型，请选择 'assistant' 或 'expert'")
                    continue
                }
            }

            print("输入您的问题: ")
            System.out.flush()

            val question = scanner.nextLine().trim()

            if (question == "exit" || question == "quit") {
                println("再见！")
                break
            }

            try {
                println("正在生成回答...")
                val response = runBlocking { agent.generate(question) }
                println("\n回答:\n${response.text}")
            } catch (e: Exception) {
                println("生成回答时发生错误: ${e.message}")
            }
        }
    }

    /**
     * 运行指定代理
     */
    private fun runAgent(agentId: String, prompt: String) {
        try {
            val agent = when (agentId) {
                "assistant" -> kastraxInstance.getAgent("assistant")
                "expert" -> kastraxInstance.getAgent("expert")
                else -> {
                    println("错误: 未知代理ID '$agentId'")
                    println("可用代理: assistant, expert")
                    return
                }
            }

            println("正在使用 ${agent.name} 生成回答...")
            val response = runBlocking { agent.generate(prompt) }
            println("\n${response.text}")
        } catch (e: Exception) {
            println("错误: ${e.message}")
        }
    }

    /**
     * 以流式方式运行指定代理
     */
    private fun streamAgent(agentId: String, prompt: String) {
        try {
            val agent = when (agentId) {
                "assistant" -> kastraxInstance.getAgent("assistant")
                "expert" -> kastraxInstance.getAgent("expert")
                else -> {
                    println("错误: 未知代理ID '$agentId'")
                    println("可用代理: assistant, expert")
                    return
                }
            }

            println("正在使用 ${agent.name} 生成回答...\n")

            runBlocking {
                val response = agent.stream(prompt, AgentStreamOptions())
                response.textStream?.collect { chunk ->
                    print(chunk)
                    System.out.flush()
                }
                println()
            }
        } catch (e: Exception) {
            println("\n错误: ${e.message}")
        }
    }

    /**
     * 启动代理交互模式
     */
    private fun startAgentInteractive(agentId: String) {
        val agent = when (agentId) {
            "assistant" -> kastraxInstance.getAgent("assistant")
            "expert" -> kastraxInstance.getAgent("expert")
            else -> {
                println("错误: 未知代理ID '$agentId'")
                println("可用代理: assistant, expert")
                return
            }
        }

        println("已启动 ${agent.name} 交互模式")
        println("输入 'exit' 或 'quit' 退出\n")

        val scanner = Scanner(System.`in`)

        while (true) {
            print("\n> ")
            System.out.flush()

            val input = scanner.nextLine().trim()

            if (input.lowercase() == "exit" || input.lowercase() == "quit") {
                println("再见！")
                break
            }

            try {
                println("正在生成回答...")
                val response = runBlocking { agent.generate(input) }
                println("\n${response.text}")
            } catch (e: Exception) {
                println("错误: ${e.message}")
            }
        }
    }

    /**
     * 列出可用工具
     */
    private fun listTools() {
        println("可用工具:")
        println("1. 计算器 (calculator)")
        println("   描述: 执行简单的数学计算，支持加减乘除")
        println("   用法: kastrax calculator")
        println()
        println("2. 天气 (weather)")
        println("   描述: 获取指定城市的天气信息")
        println("   用法: kastrax weather <city>")
    }

    /**
     * 列出可用代理
     */
    private fun listAgents() {
        println("可用代理:")
        println("1. 助手代理 (assistant)")
        println("   描述: 通用助手，可以回答问题并使用工具")
        println("   工具: calculator, weather")
        println("   用法: kastrax agent assistant <prompt>")
        println()
        println("2. 专家代理 (expert)")
        println("   描述: AI和机器学习领域的专家")
        println("   用法: kastrax agent expert <prompt>")
    }

    /**
     * 启动计算器工具
     */
    private fun startCalculator() {
        println("计算器工具")
        println("输入 'exit' 或 'quit' 退出\n")

        val scanner = Scanner(System.`in`)

        while (true) {
            print("\n请输入表达式: ")
            System.out.flush()

            val input = scanner.nextLine().trim()

            if (input.lowercase() == "exit" || input.lowercase() == "quit") {
                println("再见！")
                break
            }

            try {
                val jsonInput = JsonPrimitive(input)
                val jsonResult = runBlocking { calculatorTool.execute(jsonInput) }
                val result = (jsonResult as? JsonPrimitive)?.content ?: "无法解析结果"
                println("结果: $result")
            } catch (e: Exception) {
                println("计算错误: ${e.message}")
            }
        }
    }

    /**
     * 获取天气信息
     */
    private fun getWeather(city: String) {
        try {
            val input = buildJsonObject {
                put("city", city)
            }

            val result = runBlocking { weatherTool.execute(input) }
            val resultObj = result as? JsonObject
            val temperature = resultObj?.get("temperature")?.jsonPrimitive?.content ?: "未知"
            val condition = resultObj?.get("condition")?.jsonPrimitive?.content ?: "未知"
            val humidity = resultObj?.get("humidity")?.jsonPrimitive?.content ?: "未知"

            println("${city}天气:")
            println("温度: $temperature")
            println("天气状况: $condition")
            println("湿度: $humidity")
        } catch (e: Exception) {
            println("获取天气信息失败: ${e.message}")
        }
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
    private fun loadConfig(): AppConfig {
        val configDir = File("config")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }

        val configFile = File(configDir, "kastrax.json")

        return if (configFile.exists()) {
            try {
                Json.decodeFromString<AppConfig>(configFile.readText())
            } catch (e: Exception) {
                println("加载配置文件失败，使用默认配置: ${e.message}")
                val defaultConfig = AppConfig()
                saveConfig(defaultConfig)
                defaultConfig
            }
        } else {
            println("配置文件不存在，创建默认配置")
            val defaultConfig = AppConfig()
            saveConfig(defaultConfig)
            defaultConfig
        }
    }

    /**
     * 保存配置
     */
    private fun saveConfig(config: AppConfig) {
        val configDir = File("config")
        if (!configDir.exists()) {
            configDir.mkdirs()
        }

        val configFile = File(configDir, "kastrax.json")
        val json = Json { prettyPrint = true }
        configFile.writeText(json.encodeToString(config))
    }
}
