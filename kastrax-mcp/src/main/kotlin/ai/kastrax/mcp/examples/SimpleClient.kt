package ai.kastrax.mcp.examples

import ai.kastrax.mcp.client.mcpClient
import kotlinx.coroutines.runBlocking

/**
 * 简单的 MCP 客户端示例
 */
fun main() = runBlocking {
    println("KastraX MCP 简单客户端示例")
    println("========================")

    try {
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
            resources.forEach { resource ->
                println("- ${resource.name}: ${resource.description}")
            }

            // 获取资源内容
            if (resources.isNotEmpty()) {
                val resource = resources.first()
                println("\n获取资源内容 (${resource.name}):")
                val content = mcpClient.getResource(resource.id)
                println(content)
            }

            // 获取可用工具
            println("\n获取可用工具:")
            val tools = mcpClient.tools()
            tools.forEach { tool ->
                println("- ${tool.name}: ${tool.description}")
            }

            // 调用工具
            if (tools.isNotEmpty()) {
                val tool = tools.first()
                println("\n调用工具 (${tool.name}):")
                val result = mcpClient.callTool(tool.id, mapOf("text" to "Hello, MCP!"))
                println("结果: $result")
            }

            // 获取可用提示
            println("\n获取可用提示:")
            val prompts = mcpClient.prompts()
            prompts.forEach { prompt ->
                println("- ${prompt.name}: ${prompt.description}")
            }

            // 获取提示内容
            if (prompts.isNotEmpty()) {
                val prompt = prompts.first()
                println("\n获取提示内容 (${prompt.name}):")
                val content = mcpClient.getPrompt(prompt.id)
                println(content)
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
