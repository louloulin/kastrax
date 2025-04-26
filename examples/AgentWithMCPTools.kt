package examples

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.integration.mcpTools
import kotlinx.coroutines.runBlocking

/**
 * 使用 MCP 工具的代理示例
 */
fun main() = runBlocking {
    println("KastraX MCP 代理示例")
    println("========================")

    // 创建 MCP 客户端
    val weatherClient = mcpClient {
        name("weather-client")
        server {
            stdio {
                command = "node"
                args = listOf("examples/weather-server.js")
                env = mapOf("API_KEY" to "your-api-key")
            }
        }
    }
    
    // 连接到服务器
    println("连接到服务器...")
    weatherClient.connect()
    
    try {
        // 创建一个使用 MCP 工具的代理
        println("\n创建代理...")
        val agent = agent {
            name = "Weather Assistant"
            instructions = "你是一个天气助手，可以提供天气信息。"
            
            // 使用 OpenAI 模型
            // 注意：这里的模型配置仅作示例，实际使用时需要替换为正确的模型
            // model = openAi("gpt-4")
            
            // 添加 MCP 工具
            apply {
                runBlocking {
                    mcpTools(weatherClient)
                }
            }
        }
        
        // 使用代理
        println("\n使用代理...")
        val response = agent.generate("纽约的天气怎么样？")
        println("\n回答:\n${response.text}")
        
        // 打印工具调用
        println("\n工具调用:")
        response.toolCalls.forEach { toolCall ->
            println("- ${toolCall.name}: ${toolCall.arguments}")
        }
    } finally {
        // 断开连接
        println("\n断开连接...")
        weatherClient.disconnect()
    }
    
    println("\n示例完成!")
}
