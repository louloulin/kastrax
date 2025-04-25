package ai.kastrax.mcp.examples

import ai.kastrax.mcp.protocol.Tool
import ai.kastrax.mcp.protocol.ToolParameterProperty
import ai.kastrax.mcp.protocol.ToolParameters
import ai.kastrax.mcp.server.MCPServer
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch

/**
 * 简单的MCP服务器示例
 */
fun main() = runBlocking {
    println("启动MCP服务器示例...")

    // 创建MCP服务器
    val server = mcpServer {
        // 设置服务器名称和版本
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

        // 添加一个计算工具
        tool {
            name = "add"
            description = "将两个数字相加"

            // 添加参数
            parameters {
                parameter {
                    name = "a"
                    description = "第一个数字"
                    type = "number"
                    required = true
                }

                parameter {
                    name = "b"
                    description = "第二个数字"
                    type = "number"
                    required = true
                }
            }

            // 设置执行函数
            handler { params ->
                val a = (params["a"] as? Number)?.toDouble() ?: 0.0
                val b = (params["b"] as? Number)?.toDouble() ?: 0.0
                val result = a + b
                println("执行add工具，计算: $a + $b = $result")
                result.toString()
            }
        }

        // 添加一个提示
        prompt {
            name = "greeting"
            description = "生成问候语"
            content = "你好，{{name}}！欢迎使用KastraX MCP。"

            // 添加参数
            parameters {
                parameter {
                    name = "name"
                    description = "用户名"
                    type = "string"
                    required = true
                }
            }
        }
    }

    // 启动服务器
    server.start()
    println("MCP服务器已启动，按Enter键停止...")

    // 等待用户输入以停止服务器
    val latch = CountDownLatch(1)
    Thread {
        readLine()
        latch.countDown()
    }.start()

    latch.await()

    // 停止服务器
    server.stop()
    println("MCP服务器已停止")
}
