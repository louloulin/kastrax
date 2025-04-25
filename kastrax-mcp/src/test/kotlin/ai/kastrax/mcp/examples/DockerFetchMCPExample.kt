package ai.kastrax.mcp.examples

import ai.kastrax.mcp.client.MCPClientImpl
import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.transport.SSETransport
import kotlinx.coroutines.runBlocking

/**
 * Docker Fetch MCP示例
 * 这个示例演示如何使用Kastrax MCP客户端连接到Docker的Fetch MCP服务器。
 * 注意：这个示例需要先运行Docker的Fetch MCP服务器。
 *
 * 运行Docker的Fetch MCP服务器的命令：
 * docker run -p 8080:8080 ghcr.io/docker/mcp-servers/fetch:latest
 */
fun main() = runBlocking {
    println("启动Docker Fetch MCP示例...")

    // 创建MCP客户端
    val client = mcpClient {
        // 设置客户端名称和版本
        name("DockerFetchMCPClient")
        version("1.0.0")

        // 使用SSE服务器
        server {
            sse {
                url = "http://localhost:8080"
            }
        }
    }

    try {
        // 连接到服务器
        println("连接到Docker Fetch MCP服务器...")
        client.connect()
        println("已连接到Docker Fetch MCP服务器")

        // 获取服务器信息
        println("\n服务器信息:")
        println("名称: ${client.name}")
        println("版本: ${client.version}")
        println("功能:")
        println("- 资源: ${client.supportsCapability("resources")}")
        println("- 工具: ${client.supportsCapability("tools")}")
        println("- 提示: ${client.supportsCapability("prompts")}")

        // 获取可用工具
        val tools = client.tools()
        println("\n可用工具:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
            println("  参数:")
            tool.parameters.properties.forEach { (name, prop) ->
                println("    - $name: ${prop.description} (${prop.type}${if (tool.parameters.required.contains(name)) ", 必需" else ""})")
            }
        }

        // 使用fetch工具
        println("\n使用fetch工具获取'https://modelcontextprotocol.io'...")
        val fetchResult = client.callTool("fetch", mapOf("url" to "https://modelcontextprotocol.io", "max_length" to 1000))
        println("获取结果:")
        println(fetchResult)

        // 使用fetch工具获取GitHub页面
        println("\n使用fetch工具获取'https://github.com/docker/mcp-servers'...")
        val githubResult = client.callTool("fetch", mapOf("url" to "https://github.com/docker/mcp-servers", "max_length" to 1000))
        println("GitHub结果:")
        println(githubResult)

        // 使用fetch工具获取原始内容
        println("\n使用fetch工具获取原始内容...")
        val rawResult = client.callTool("fetch", mapOf("url" to "https://modelcontextprotocol.io", "max_length" to 500, "raw" to true))
        println("原始内容结果:")
        println(rawResult)

    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        println("已断开与Docker Fetch MCP服务器的连接")

        // 添加一个延迟，确保所有资源都被释放
        kotlinx.coroutines.delay(1000)

        // 强制退出程序
        kotlin.system.exitProcess(0)
    }
}
