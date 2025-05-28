package ai.kastrax.examples.mcp

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking

/**
 * 将代理功能暴露为 MCP 服务的示例
 */
fun main() = runBlocking {
    println("KastraX MCP 代理服务器示例")
    println("========================")

    // 创建一个 KastraX 代理
    println("创建代理...")
    val weatherAgent = agent {
        name = "Weather Agent"
        instructions = "你是一个天气助手，可以提供天气信息。"
        // 注意：这里的模型配置仅作示例，实际使用时需要替换为正确的模型
        // model = openAi("gpt-4")
    }
    
    // 创建 MCP 服务器，暴露代理功能
    println("\n创建 MCP 服务器...")
    val mcpServer = mcpServer {
        name("agent-server")
        version("1.0.0")
        
        // 添加资源
        resource {
            name = "agent-info"
            description = "代理信息"
            content = """
                # Weather Agent
                
                这是一个天气助手，可以提供天气信息。
                
                ## 使用方法
                
                使用 `askWeatherAgent` 工具向代理提问。
            """.trimIndent()
        }
        
        // 添加代理工具
        tool {
            name = "askWeatherAgent"
            description = "向天气代理询问天气信息"
            parameters {
                parameter {
                    name = "question"
                    type = "string"
                    description = "要询问的天气问题"
                    required = true
                }
            }
            
            handler { params ->
                val question = params["question"] as? String ?: "今天天气怎么样？"
                
                // 使用代理生成回答
                val response = weatherAgent.generate(question)
                
                mapOf(
                    "answer" to response.text,
                    "timestamp" to System.currentTimeMillis()
                )
            }
        }
    }
    
    // 启动服务器
    println("\n启动 MCP 服务器...")
    mcpServer.start()
    
    println("MCP 服务器已启动，等待客户端连接...")
    println("按 Enter 键停止服务器")
    readLine()
    
    // 停止服务器
    println("\n停止服务器...")
    mcpServer.stop()
    println("服务器已停止")
}