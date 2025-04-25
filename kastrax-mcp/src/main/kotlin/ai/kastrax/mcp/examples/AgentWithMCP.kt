package ai.kastrax.mcp.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.AgentBuilder
import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.client.MCPClient
import ai.kastrax.mcp.integration.mcpTools
import kotlinx.coroutines.runBlocking

/**
 * 使用 MCP 工具的代理示例
 */
fun main() = runBlocking {
    println("KastraX MCP 代理示例")
    println("========================")

    try {
        // 创建 MCP 客户端
        val mcpClient = mcpClient {
            name("agent-client")
            server {
                stdio {
                    command = "node"
                    args = listOf("examples/simple-server.js")
                }
            }
        }

        // 连接到服务器
        println("连接到服务器...")
        mcpClient.connect()

        try {
            // 创建一个使用 MCP 工具的代理
            println("\n创建代理...")
            val agent = agent {
                name = "MCP Assistant"
                instructions = "你是一个助手，可以使用 MCP 提供的工具。"

                // 使用模型
                // 注意：这里的模型配置仅作示例，实际使用时需要替换为正确的模型
                // model = openAi("gpt-4")

                // 添加 MCP 工具
                mcpTools(mcpClient)
            }

            // 使用代理
            println("\n使用代理...")
            val response = agent.generate("请使用 echo 工具发送消息 'Hello, Agent!'，然后使用 reverse 工具反转这个消息。")

            println("\n代理回答:")
            println(response.text)

            // 打印工具调用
            println("\n工具调用:")
            response.toolCalls.forEach { toolCall ->
                println("- ${toolCall.name}: ${toolCall.arguments}")
            }
        } finally {
            // 断开连接
            println("\n断开连接...")
            mcpClient.disconnect()
        }

        println("\n示例完成!")
    } catch (e: Exception) {
        println("\n发生错误: ${e.message}")
        e.printStackTrace()
    }
}
