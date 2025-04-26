package examples

import ai.kastrax.mcp.server.mcpServer
import ai.kastrax.mcp.protocol.ResourceType
import kotlinx.coroutines.runBlocking

/**
 * 简单的 MCP 服务器示例
 */
fun main() = runBlocking {
    println("KastraX MCP 简单服务器示例")
    println("========================")

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
                text
            }
        }
        
        // 添加另一个工具
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
                text.reversed()
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
}
