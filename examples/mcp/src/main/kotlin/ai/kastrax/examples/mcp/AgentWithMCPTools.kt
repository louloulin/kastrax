package ai.kastrax.examples.mcp

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
        
        // 测试代理
        println("\n测试代理...")
        val response = agent.generate("北京今天的天气怎么样？")
        println("代理回复: ${response.text}")
        
        // 流式生成示例
        println("\n=== 流式生成示例 ===")
        println("用户: 上海明天的天气预报如何？")
        
        val streamResponse = agent.stream("上海明天的天气预报如何？")
        
        print("助手: ")
        streamResponse.textStream?.collect { chunk ->
            print(chunk)
        }
        println("\n")
        
    } finally {
        // 断开连接
        println("断开连接...")
        weatherClient.disconnect()
    }
}