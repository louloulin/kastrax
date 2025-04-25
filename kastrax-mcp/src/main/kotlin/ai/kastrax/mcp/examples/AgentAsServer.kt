package ai.kastrax.mcp.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.AgentBuilder
import ai.kastrax.mcp.server.mcpServer
import ai.kastrax.mcp.server.MCPServer
import kotlinx.coroutines.runBlocking

/**
 * 将代理功能暴露为 MCP 服务的示例
 */
fun main() = runBlocking {
    println("KastraX MCP 代理服务器示例")
    println("========================")

    try {
        // 创建一个 KastraX 代理
        println("创建代理...")
        val assistantAgent = agent {
            name = "Assistant Agent"
            instructions = "你是一个友好的助手，可以回答各种问题。"
            // 注意：这里的模型配置仅作示例，实际使用时需要替换为正确的模型
            // model = openAi("gpt-4")
        }

        // 创建 MCP 服务器，暴露代理功能
        println("\n创建 MCP 服务器...")
        val mcpServer = mcpServer {
            name("agent-server")
            version("1.0.0")

            // 添加资源
            resource {
                name = "agent-info"
                description = "代理信息"
                content = """
                    # Assistant Agent

                    这是一个友好的助手，可以回答各种问题。

                    ## 使用方法

                    使用 `askAgent` 工具向代理提问。
                """.trimIndent()
            }

            // 添加代理工具
            tool {
                name = "askAgent"
                description = "向代理提问"
                parameters {
                    parameter {
                        name = "question"
                        type = "string"
                        description = "问题"
                        required = true
                    }
                }
                handler { params ->
                    val question = params["question"] as String

                    // 使用代理生成回答
                    val response = assistantAgent.generate(question)

                    // 返回代理的回答
                    response.text
                }
            }
        }

        // 启动服务器
        println("\n启动服务器...")
        mcpServer.startSSE(port = 8080)

        println("\n服务器已启动，访问 http://localhost:8080/mcp/sse")
        println("按 Enter 键停止服务器...")
        readLine()

        // 停止服务器
        println("\n停止服务器...")
        mcpServer.stop()

        println("\n示例完成!")
    } catch (e: Exception) {
        println("\n发生错误: ${e.message}")
        e.printStackTrace()
    }
}
