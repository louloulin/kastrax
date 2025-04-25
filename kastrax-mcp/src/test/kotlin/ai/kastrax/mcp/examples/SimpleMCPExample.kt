package ai.kastrax.mcp.examples

import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.protocol.Tool
import ai.kastrax.mcp.protocol.ToolParameterProperty
import ai.kastrax.mcp.protocol.ToolParameters
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch

/**
 * 简单的MCP示例
 */
fun main() = runBlocking {
    println("启动MCP示例...")

    // 创建MCP服务器
    val server = mcpServer {
        name("SimpleMCPServer")
        version("1.0.0")

        // 添加一个简单的工具
        tool {
            name = "echo"
            description = "回显输入的消息"

            // 添加参数
            parameters {
                parameter {
                    name = "message"
                    description = "要回显的消息"
                    type = "string"
                    required = true
                }
            }

            // 设置执行函数
            handler { params ->
                val message = params["message"] as? String ?: "No message provided"
                println("执行echo工具，消息: $message")
                message
            }
        }
    }

    // 启动服务器
    server.startSSE(port = 8080)
    println("MCP服务器已启动在端口8080")

    // 等待服务器启动完成
    Thread.sleep(1000)

    // 创建MCP客户端
    val client = mcpClient {
        name("SimpleMCPClient")
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

        // 获取可用工具
        val tools = client.tools()
        println("可用工具:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
        }

        // 调用echo工具
        println("\n调用echo工具...")
        val result = client.callTool("echo", mapOf("message" to "Hello, MCP!"))
        println("echo结果: $result")

    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        server.stop()
        println("已停止MCP服务器和客户端")

        // 添加一个延迟，确保所有资源都被释放
        kotlinx.coroutines.delay(1000)

        // 强制退出程序
        kotlin.system.exitProcess(0)
    }
}
