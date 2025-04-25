package ai.kastrax.mcp.examples

import ai.kastrax.mcp.client.mcpClient
import kotlinx.coroutines.runBlocking

/**
 * 真实Mastra MCP示例
 * 这个示例演示如何使用Kastrax MCP客户端连接到真实的Mastra MCP服务器。
 * 注意：这个示例需要一个有效的Mastra API密钥才能运行。
 */
fun main() = runBlocking {
    println("启动真实Mastra MCP示例...")

    // Mastra API密钥（需要替换为有效的密钥）
    val apiKey = "YOUR_MASTRA_API_KEY"

    // 创建MCP客户端
    val client = mcpClient {
        // 设置客户端名称和版本
        name("RealMastraMCPClient")
        version("1.0.0")

        // 使用Mastra服务器
        server {
            sse {
                url = "https://api.mastra.ai/v1/mcp?api_key=$apiKey"
            }
        }
    }

    try {
        // 连接到服务器
        println("连接到Mastra MCP服务器...")
        client.connect()
        println("已连接到Mastra MCP服务器")

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

        // 使用search工具（如果可用）
        if (tools.any { it.name == "search" }) {
            println("\n使用search工具搜索'Mastra AI'...")
            val searchResult = client.callTool("search", mapOf("query" to "Mastra AI", "num_results" to 3))
            println("搜索结果:")
            println(searchResult)
        }

        // 使用fetch工具（如果可用）
        if (tools.any { it.name == "fetch" }) {
            println("\n使用fetch工具获取'https://mastra.ai'...")
            val fetchResult = client.callTool("fetch", mapOf("url" to "https://mastra.ai", "max_length" to 1000))
            println("获取结果:")
            println(fetchResult)
        }

    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        println("已断开与Mastra MCP服务器的连接")

        // 添加一个延迟，确保所有资源都被释放
        kotlinx.coroutines.delay(1000)

        // 强制退出程序
        kotlin.system.exitProcess(0)
    }
}
