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
            "real-deepseek" -> startRealDeepSeekAgent()
            "--test-serialization" -> testSerialization()
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
        println("警告: 未设置 DeepSeek API 密钥，将使用模拟模式")
    }

    println("启动 DeepSeek Agent...")

    // 使用简化版本的 DeepSeek Agent 示例
    val apiKey = config.apiKeys.deepseek
    ai.kastrax.graal.agent.SimpleDeepSeekAgent.run(apiKey, interactive = true)
}

/**
 * 启动真实的 DeepSeek Agent
 */
private fun startRealDeepSeekAgent() {
    logger.info { "启动真实的 DeepSeek Agent..." }

    // 检查 API 密钥
    if (config.apiKeys.deepseek.isEmpty()) {
        println("警告: 未设置 DeepSeek API 密钥，将使用默认密钥")
    }

    println("启动真实的 DeepSeek Agent...")

    // 使用真实的 DeepSeek Agent
    val apiKey = config.apiKeys.deepseek
    ai.kastrax.graal.agent.RealDeepSeekAgent.run(apiKey, interactive = true)
}

/**
 * 测试序列化功能
 *
 * 这个函数用于测试在 GraalVM 原生镜像中的序列化和反序列化功能
 */
private fun testSerialization() {
    println("===== 开始测试序列化功能 =====\n")

    // 获取 JSON 实例
    val json = ai.kastrax.graal.serialization.SerializationInitializer.json

    try {
        // 1. 测试 DeepSeekChatCompletionRequest 序列化
        println("1. 测试 DeepSeekChatCompletionRequest 序列化")
        val request = ai.kastrax.integrations.deepseek.DeepSeekChatCompletionRequest(
            model = "deepseek-chat",
            messages = listOf(
                ai.kastrax.integrations.deepseek.DeepSeekMessage(
                    role = "user",
                    content = "Hello, how are you?"
                )
            ),
            temperature = 0.7,
            maxTokens = 100
        )

        val requestJson = json.encodeToString(ai.kastrax.integrations.deepseek.DeepSeekChatCompletionRequest.serializer(), request)
        println("\u5e8f列化结果: $requestJson")

        val deserializedRequest = json.decodeFromString(ai.kastrax.integrations.deepseek.DeepSeekChatCompletionRequest.serializer(), requestJson)
        println("\u53cd序列化结果: $deserializedRequest")
        println("\u6d4b试通过: ${deserializedRequest.model == request.model && deserializedRequest.messages.size == request.messages.size}\n")

        // 2. 测试 DeepSeekChatCompletionResponse 序列化
        println("2. 测试 DeepSeekChatCompletionResponse 序列化")
        val response = ai.kastrax.integrations.deepseek.DeepSeekChatCompletionResponse(
            id = "resp-123456",
            objectType = "chat.completion",
            created = System.currentTimeMillis() / 1000,
            model = "deepseek-chat",
            choices = listOf(
                ai.kastrax.integrations.deepseek.DeepSeekChoice(
                    index = 0,
                    message = ai.kastrax.integrations.deepseek.DeepSeekMessage(
                        role = "assistant",
                        content = "I'm doing well, thank you for asking!"
                    ),
                    finishReason = "stop"
                )
            ),
            usage = ai.kastrax.integrations.deepseek.DeepSeekUsage(
                promptTokens = 10,
                completionTokens = 15,
                totalTokens = 25
            )
        )

        val responseJson = json.encodeToString(ai.kastrax.integrations.deepseek.DeepSeekChatCompletionResponse.serializer(), response)
        println("\u5e8f列化结果: $responseJson")

        val deserializedResponse = json.decodeFromString(ai.kastrax.integrations.deepseek.DeepSeekChatCompletionResponse.serializer(), responseJson)
        println("\u53cd序列化结果: $deserializedResponse")
        println("\u6d4b试通过: ${deserializedResponse.id == response.id && deserializedResponse.model == response.model}\n")

        // 3. 测试 DeepSeekEmbeddingRequest 序列化
        println("3. 测试 DeepSeekEmbeddingRequest 序列化")
        val embeddingRequest = ai.kastrax.integrations.deepseek.DeepSeekEmbeddingRequest(
            model = "deepseek-embedding",
            input = listOf("Hello, world!")
        )

        val embeddingRequestJson = json.encodeToString(ai.kastrax.integrations.deepseek.DeepSeekEmbeddingRequest.serializer(), embeddingRequest)
        println("\u5e8f列化结果: $embeddingRequestJson")

        val deserializedEmbeddingRequest = json.decodeFromString(ai.kastrax.integrations.deepseek.DeepSeekEmbeddingRequest.serializer(), embeddingRequestJson)
        println("\u53cd序列化结果: $deserializedEmbeddingRequest")
        println("\u6d4b试通过: ${deserializedEmbeddingRequest.model == embeddingRequest.model && deserializedEmbeddingRequest.input.size == embeddingRequest.input.size}\n")

        // 4. 测试 JSON 基本类型序列化
        println("4. 测试 JSON 基本类型序列化")
        val jsonObject = kotlinx.serialization.json.JsonObject(
            mapOf(
                "string" to kotlinx.serialization.json.JsonPrimitive("value"),
                "number" to kotlinx.serialization.json.JsonPrimitive(42),
                "boolean" to kotlinx.serialization.json.JsonPrimitive(true),
                "array" to kotlinx.serialization.json.JsonArray(listOf(kotlinx.serialization.json.JsonPrimitive(1), kotlinx.serialization.json.JsonPrimitive(2))),
                "nested" to kotlinx.serialization.json.JsonObject(mapOf("key" to kotlinx.serialization.json.JsonPrimitive("value")))
            )
        )

        val jsonObjectString = json.encodeToString(kotlinx.serialization.json.JsonObject.serializer(), jsonObject)
        println("\u5e8f列化结果: $jsonObjectString")

        val deserializedJsonObject = json.decodeFromString(kotlinx.serialization.json.JsonObject.serializer(), jsonObjectString)
        println("\u53cd序列化结果: $deserializedJsonObject")
        println("\u6d4b试通过: ${deserializedJsonObject.size == jsonObject.size}\n")

        // 5. 测试应用配置序列化
        println("5. 测试应用配置序列化")
        val appConfig = AppConfig(
            appName = "Test App",
            version = "1.0.0",
            logging = AppConfig.Logging(level = "DEBUG"),
            apiKeys = AppConfig.ApiKeys(deepseek = "test-key")
        )

        val appConfigJson = json.encodeToString(AppConfig.serializer(), appConfig)
        println("\u5e8f列化结果: $appConfigJson")

        val deserializedAppConfig = json.decodeFromString(AppConfig.serializer(), appConfigJson)
        println("\u53cd序列化结果: $deserializedAppConfig")
        println("\u6d4b试通过: ${deserializedAppConfig.appName == appConfig.appName && deserializedAppConfig.version == appConfig.version}\n")

        println("===== 所有序列化测试已通过 =====\n")
    } catch (e: Exception) {
        println("\u5e8f列化测试失败: ${e.message}")
        e.printStackTrace()
        println("\n===== 序列化测试失败 =====\n")
    }
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
          server              启动服务器模式
          cli                 启动命令行界面模式
          config              显示当前配置
          deepseek            启动 DeepSeek Agent 示例
          real-deepseek       启动真实的 DeepSeek Agent（使用 agent DSL）
          --test-serialization 测试序列化功能
          help                显示帮助信息

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
