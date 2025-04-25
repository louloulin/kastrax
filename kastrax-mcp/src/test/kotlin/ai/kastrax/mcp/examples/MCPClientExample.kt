package ai.kastrax.mcp.examples

import ai.kastrax.mcp.client.mcpClient
import kotlinx.coroutines.runBlocking

/**
 * MCP客户端示例
 */
fun main() = runBlocking {
    println("启动MCP客户端示例...")
    
    // 创建MCP客户端
    val client = mcpClient {
        name("ExampleMCPClient")
        version("1.0.0")
        
        server {
            sse {
                url = "http://localhost:8080"
            }
        }
    }
    
    try {
        // 连接到服务器
        println("连接到MCP服务器...")
        client.connect()
        println("已连接到MCP服务器")
        
        // 获取服务器能力
        val capabilities = client.supportsCapability("tools")
        println("服务器支持工具: $capabilities")
        
        // 列出可用工具
        val tools = client.tools()
        println("可用工具:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
        }
        
        // 调用echo工具
        println("\n调用echo工具...")
        val echoResult = client.callTool("echo", mapOf("message" to "Hello, MCP!"))
        println("echo结果: $echoResult")
        
        // 调用add工具
        println("\n调用add工具...")
        val addResult = client.callTool("add", mapOf("a" to 5, "b" to 3))
        println("add结果: $addResult")
        
    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        println("已断开与MCP服务器的连接")
    }
}
