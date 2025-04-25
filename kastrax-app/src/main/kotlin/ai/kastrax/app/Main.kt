package ai.kastrax.app

import ai.kastrax.core.kastrax
import ai.kastrax.app.agents.assistantAgent
import ai.kastrax.app.agents.expertAgent
import ai.kastrax.app.config.loadConfig
import ai.kastrax.app.constants.AgentIds
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.integrations.deepseek.DeepSeekException
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking

private val logger = KotlinLogging.logger {}

// 加载配置
val config = loadConfig()

// 使用 KastraX DSL 创建实例
val kastraxInstance = kastrax {
    // 注册代理
    agent(AgentIds.ASSISTANT, assistantAgent)
    agent(AgentIds.EXPERT, expertAgent)

    // 注意：KastraX 核心目前只支持注册代理
    // 工具和工作流通过代理使用
}

/**
 * KastraX 应用程序入口点。
 */
fun main(args: Array<String>) {
    // 设置 UTF-8 编码，确保中文正确显示
    System.setProperty("file.encoding", System.getenv("FILE_ENCODING") ?: "UTF-8")
    System.setProperty("sun.jnu.encoding", System.getenv("SUN_JNU_ENCODING") ?: "UTF-8")

    logger.info { "启动 KastraX 应用程序..." }

    try {
        logger.info { "已加载配置: ${config.appName}" }
        logger.info { "已注册 ${kastraxInstance.getAgents().size} 个代理" }

        // 注册工具和工作流是通过各自的模块完成的，不是通过 KastraX 实例
        // 工具已经在代理定义中使用
        // 工作流需要单独初始化

        logger.info { "KastraX 应用程序已准备就绪！" }
        logger.info { "使用 'kastrax dev' 启动开发服务器" }

        // 如果有命令行参数，可以在这里处理
        if (args.isNotEmpty()) {
            when (args[0]) {
                "server" -> startServer()
                "cli" -> startCli()
                else -> printHelp()
            }
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
 * 启动服务器模式。
 */
private fun startServer() {
    logger.info { "启动服务器模式..." }
    // 在这里实现服务器启动逻辑
}

/**
 * 启动命令行界面模式。
 */
private fun startCli() {
    logger.info { "启动命令行界面模式..." }

    // 简单的命令行界面实现
    println("欢迎使用 KastraX CLI 模式")
    println("输入 'exit' 退出")
    println()

    // 获取代理
    val assistantAgent = kastraxInstance.getAgent("assistant")
    val expertAgent = kastraxInstance.getAgent("expert")

    // 交互循环
    var running = true
    while (running) {
        try {
            print("输入问题 (使用 'assistant' 或 'expert'): ")
            val input = readLine() ?: ""

            if (input.equals("exit", ignoreCase = true)) {
                running = false
                println("再见！")
            } else if (input.isNotBlank()) {
                val parts = input.split(":", limit = 2)
                val agentType = parts[0].trim().lowercase()
                val question = if (parts.size > 1) parts[1].trim() else ""

                if (question.isBlank()) {
                    println("请输入问题，格式为 'agent类型: 问题'")
                    println("例如: 'assistant: 计算 15 * 7 的结果是多少？'")
                    continue
                }

                val agent = when (agentType) {
                    "assistant" -> assistantAgent
                    "expert" -> expertAgent
                    else -> {
                        println("未知代理类型: $agentType，请使用 'assistant' 或 'expert'")
                        continue
                    }
                }

                println("正在生成回答...")
                // 使用 runBlocking 来调用 suspend 函数
                val response = runBlocking {
                    agent.generate(
                        question,
                        AgentGenerateOptions(temperature = 0.7, maxTokens = 1000)
                    )
                }
                println("回答: ${response.text}")
            }
        } catch (e: DeepSeekException) {
            println("DeepSeek API 错误: ${e.message}")
            println("请检查 API 密钥是否正确，或者尝试增加超时时间")
        } catch (e: Exception) {
            println("发生错误: ${e.message}")
            e.printStackTrace()
        }
        println()
    }
}

/**
 * 打印帮助信息。
 */
private fun printHelp() {
    println("""
        KastraX 应用程序

        用法:
          java -jar kastrax-app.jar [命令]

        命令:
          server    启动服务器模式
          cli       启动命令行界面模式

        示例:
          java -jar kastrax-app.jar server
    """.trimIndent())
}
