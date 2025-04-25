package ai.kastrax.mcp.examples

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.integration.mcpTools
import ai.kastrax.mcp.protocol.Tool
import ai.kastrax.mcp.protocol.ToolParameterProperty
import ai.kastrax.mcp.protocol.ToolParameters
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject

/**
 * MCP与KastraX代理集成示例
 */
fun main() = runBlocking {
    println("启动MCP与KastraX代理集成示例...")

    // 创建MCP服务器
    val server = mcpServer {
        // 设置服务器名称和版本
        name("MCPAgentServer")
        version("1.0.0")

        // 添加一个天气工具
        tool {
            name = "get_weather"
            description = "获取指定城市的天气信息"

            // 添加参数
            parameters {
                parameter {
                    name = "city"
                    description = "城市名称"
                    type = "string"
                    required = true
                }
            }

            // 设置执行函数
            handler { params ->
                val city = params["city"] as? String ?: "未知城市"
                println("执行get_weather工具，城市: $city")
                "晴天，温度25°C，湿度60%"
            }
        }

        // 添加一个搜索工具
        tool {
            name = "search"
            description = "搜索指定关键词的信息"

            // 添加参数
            parameters {
                parameter {
                    name = "query"
                    description = "搜索关键词"
                    type = "string"
                    required = true
                }
            }

            // 设置执行函数
            handler { params ->
                val query = params["query"] as? String ?: "未知查询"
                println("执行search工具，查询: $query")
                "关于'$query'的搜索结果：这是一个模拟的搜索结果。"
            }
        }
    }

    // 启动服务器
    server.start()
    println("MCP服务器已启动")

    // 创建MCP客户端
    val client = mcpClient {
        // 设置客户端名称和版本
        name("MCPAgentClient")
        version("1.0.0")

        // 使用本地服务器
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

        // 创建KastraX代理
        val agent = agent {
            // 添加MCP工具
            runBlocking {
                mcpTools(client)
            }
        }

        // 使用代理生成回答
        println("\n使用代理生成回答...")
        val prompt = """
            你是一个有用的助手。请使用可用的工具来回答以下问题：

            1. 北京的天气怎么样？
            2. 搜索关于"人工智能"的信息。

            请使用工具来获取这些信息，然后提供一个综合的回答。
        """.trimIndent()

        val response = agent.generate(prompt, AgentGenerateOptions())

        println("\n代理回答:")
        println(response)

    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        server.stop()
        println("已停止MCP服务器和客户端")
    }
}
