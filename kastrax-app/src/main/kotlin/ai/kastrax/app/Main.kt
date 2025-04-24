package ai.kastrax.app

import ai.kastrax.core.kastrax
import ai.kastrax.app.agents.assistantAgent
import ai.kastrax.app.agents.expertAgent
import ai.kastrax.app.config.loadConfig
import io.github.oshai.kotlinlogging.KotlinLogging

private val logger = KotlinLogging.logger {}

// 加载配置
val config = loadConfig()

// 使用 KastraX DSL 创建实例
val kastraxInstance = kastrax {
    // 注册代理
    agent(assistantAgent.id, assistantAgent)
    agent(expertAgent.id, expertAgent)
    
    // 注意：KastraX 核心目前只支持注册代理
    // 工具和工作流通过代理使用
}

/**
 * KastraX 应用程序入口点。
 */
fun main(args: Array<String>) {
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
    } catch (e: Exception) {
        logger.error(e) { "启动应用程序时发生错误" }
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
    // 在这里实现命令行界面逻辑
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
