package ai.kastrax.mcp.examples

import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking

/**
 * 简单的 MCP 服务器示例
 */
fun main() = runBlocking {
    println("KastraX MCP 简单服务器示例")
    println("========================")

    try {
        // 创建 MCP 服务器
        val mcpServer = mcpServer {
            name("simple-server")
            version("1.0.0")

            // 添加资源
            resource {
                name = "greeting"
                description = "问候消息"
                content = "Hello, MCP!"
            }

            resource {
                name = "documentation"
                description = "KastraX MCP 文档"
                content = """
                    # KastraX MCP

                    KastraX MCP 是 Model Context Protocol (MCP) 在 KastraX 框架中的实现。
                    它允许 KastraX 代理与支持 MCP 的应用程序和服务进行无缝集成。
                """.trimIndent()
            }

            // 添加工具
            tool {
                name = "echo"
                description = "回显输入的文本"
                parameters {
                    parameter {
                        name = "text"
                        type = "string"
                        description = "要回显的文本"
                        required = true
                    }
                }
                handler { params ->
                    val text = params["text"] as String
                    "Echo: $text"
                }
            }

            tool {
                name = "reverse"
                description = "反转输入的文本"
                parameters {
                    parameter {
                        name = "text"
                        type = "string"
                        description = "要反转的文本"
                        required = true
                    }
                }
                handler { params ->
                    val text = params["text"] as String
                    "Reversed: ${text.reversed()}"
                }
            }

            // 添加提示
            prompt {
                name = "greeting"
                description = "问候提示"
                content = "你好，{{name}}！"
                parameters {
                    parameter {
                        name = "name"
                        type = "string"
                        description = "用户名称"
                        required = true
                    }
                }
            }

            prompt {
                name = "introduction"
                description = "介绍提示"
                content = """
                    # 介绍

                    我是 {{name}}，我是一个{{profession}}。

                    我喜欢{{hobby}}。
                """.trimIndent()
                parameters {
                    parameter {
                        name = "name"
                        type = "string"
                        description = "用户名称"
                        required = true
                    }
                    parameter {
                        name = "profession"
                        type = "string"
                        description = "职业"
                        required = true
                    }
                    parameter {
                        name = "hobby"
                        type = "string"
                        description = "爱好"
                        required = true
                    }
                }
            }
        }

        // 启动服务器
        println("启动服务器...")
        mcpServer.start()

        println("\n服务器已启动，按 Enter 键停止服务器...")
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
