package examples

import ai.kastrax.mcp.protocol.ResourceType
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * 文档服务器示例
 */
fun main() = runBlocking {
    println("KastraX MCP 文档服务器示例")
    println("========================")

    // 创建文档服务器
    val docsServer = mcpServer {
        name("kastrax-docs-server")
        version("1.0.0")
        
        // 添加文档资源
        val docsDir = File("docs")
        docsDir.listFiles()?.filter { it.extension == "md" }?.forEach { file ->
            resource {
                name = file.nameWithoutExtension
                description = "KastraX ${file.nameWithoutExtension} 文档"
                content = file.readText()
            }
        }
        
        // 添加搜索工具
        tool {
            name = "searchDocs"
            description = "搜索文档"
            parameters {
                parameter {
                    name = "query"
                    type = "string"
                    description = "搜索查询"
                    required = true
                }
            }
            handler { params ->
                val query = params["query"] as String
                
                // 搜索文档的逻辑
                val results = docsDir.listFiles()?.filter { it.extension == "md" }
                    ?.map { file -> file.nameWithoutExtension to file.readText() }
                    ?.filter { (_, content) -> content.contains(query, ignoreCase = true) }
                    ?.map { (name, content) ->
                        val snippet = content.lines()
                            .filter { it.contains(query, ignoreCase = true) }
                            .take(3)
                            .joinToString("\n")
                        
                        "文档: $name\n摘要: $snippet"
                    }
                    ?.joinToString("\n\n")
                    ?: "未找到结果"
                
                results
            }
        }
    }
    
    // 启动服务器
    println("\n启动服务器...")
    docsServer.startSSE(port = 8080)
    
    println("\n文档服务器已启动，访问 http://localhost:8080/mcp/sse")
    println("按 Enter 键停止服务器...")
    readLine()
    
    // 停止服务器
    println("\n停止服务器...")
    docsServer.stop()
    
    println("\n示例完成!")
}
