package ai.kastrax.mcp.examples

import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import java.util.concurrent.CountDownLatch

/**
 * MCP服务器示例
 */
fun main() = runBlocking {
    println("启动MCP服务器示例...")
    
    // 创建MCP服务器
    val server = mcpServer {
        name("ExampleMCPServer")
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
    }
    
    // 启动服务器
    server.startSSE(port = 8080)
    println("MCP服务器已启动在端口8080")
    
    // 等待用户输入以停止服务器
    val latch = CountDownLatch(1)
    println("按Enter键停止服务器...")
    
    Thread {
        readLine()
        latch.countDown()
    }.start()
    
    latch.await()
    
    // 停止服务器
    server.stop()
    println("MCP服务器已停止")
}
