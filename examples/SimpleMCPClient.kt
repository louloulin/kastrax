package examples

import ai.kastrax.mcp.client.mcpClient
import kotlinx.coroutines.runBlocking

/**
 * 简单的 MCP 客户端示例
 */
fun main() = runBlocking {
    println("KastraX MCP 简单客户端示例")
    println("========================")

    // 创建 MCP 客户端
    val mcpClient = mcpClient {
        name("simple-client")
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
        // 获取可用资源
        println("\n获取可用资源:")
        val resources = mcpClient.resources()
        println("Available resources: ${resources.joinToString()}")
        
        // 获取可用工具
        println("\n获取可用工具:")
        val tools = mcpClient.tools()
        println("Available tools: ${tools.joinToString()}")
        
        // 调用工具
        println("\n调用工具:")
        val result = mcpClient.callTool("echo", mapOf("text" to "Hello, MCP!"))
        println("Echo result: $result")
    } finally {
        // 断开连接
        println("\n断开连接...")
        mcpClient.disconnect()
    }
    
    println("\n示例完成!")
}
