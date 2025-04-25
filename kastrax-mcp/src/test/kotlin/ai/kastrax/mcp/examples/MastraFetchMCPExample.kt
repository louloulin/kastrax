package ai.kastrax.mcp.examples

import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Mastra Fetch MCP Example
 * 这个示例演示如何使用Kastrax MCP客户端连接到Mastra的Fetch MCP服务器进行网页内容获取。
 */
fun main() = runBlocking {
    println("启动Mastra Fetch MCP示例...")

    // 创建本地MCP服务器（模拟Mastra的Fetch服务器）
    val server = mcpServer {
        // 设置服务器名称和版本
        name("MastraFetchMCPServer")
        version("1.0.0")

        // 添加fetch工具
        tool {
            name = "fetch"
            description = "从互联网获取URL并提取其内容为markdown格式"

            // 添加参数
            parameters {
                parameter {
                    name = "url"
                    description = "要获取的URL"
                    type = "string"
                    required = true
                }
                parameter {
                    name = "max_length"
                    description = "返回的最大字符数（默认：5000）"
                    type = "integer"
                    required = false
                }
                parameter {
                    name = "start_index"
                    description = "从此字符索引开始内容（默认：0）"
                    type = "integer"
                    required = false
                }
                parameter {
                    name = "raw"
                    description = "获取原始内容而不进行markdown转换（默认：false）"
                    type = "boolean"
                    required = false
                }
            }

            // 设置执行函数
            handler { params ->
                val url = params["url"] as? String ?: throw IllegalArgumentException("URL is required")
                val maxLength = (params["max_length"] as? Int) ?: 5000
                val startIndex = (params["start_index"] as? Int) ?: 0
                val raw = (params["raw"] as? Boolean) ?: false
                
                println("执行fetch工具，URL: $url, maxLength: $maxLength, startIndex: $startIndex, raw: $raw")
                
                try {
                    // 创建URL连接
                    val connection = URL(url).openConnection() as HttpURLConnection
                    connection.requestMethod = "GET"
                    connection.setRequestProperty("User-Agent", "ModelContextProtocol/1.0 (Autonomous; +https://github.com/modelcontextprotocol/servers)")
                    
                    // 获取响应
                    val responseCode = connection.responseCode
                    if (responseCode == HttpURLConnection.HTTP_OK) {
                        val content = connection.inputStream.bufferedReader().use { it.readText() }
                        
                        // 处理内容
                        val processedContent = if (raw) {
                            content
                        } else {
                            // 简单的HTML到Markdown转换（实际应用中应使用更复杂的转换）
                            convertHtmlToMarkdown(content)
                        }
                        
                        // 截取内容
                        val startPos = if (startIndex < processedContent.length) startIndex else 0
                        val endPos = if (startPos + maxLength < processedContent.length) startPos + maxLength else processedContent.length
                        val truncatedContent = processedContent.substring(startPos, endPos)
                        
                        // 返回结果
                        if (truncatedContent.length < processedContent.length - startPos) {
                            "$truncatedContent\n\n[Content truncated. Use start_index=${endPos} to continue reading]"
                        } else {
                            truncatedContent
                        }
                    } else {
                        "Error: Unable to fetch URL. Response code: $responseCode"
                    }
                } catch (e: Exception) {
                    "Error fetching URL: ${e.message}"
                }
            }
        }

        // 添加search工具
        tool {
            name = "search"
            description = "使用搜索引擎搜索信息"

            // 添加参数
            parameters {
                parameter {
                    name = "query"
                    description = "搜索查询"
                    type = "string"
                    required = true
                }
                parameter {
                    name = "num_results"
                    description = "返回的结果数量（默认：5）"
                    type = "integer"
                    required = false
                }
            }

            // 设置执行函数
            handler { params ->
                val query = params["query"] as? String ?: throw IllegalArgumentException("Query is required")
                val numResults = (params["num_results"] as? Int) ?: 5
                
                println("执行search工具，查询: $query, 结果数量: $numResults")
                
                try {
                    // 模拟搜索结果
                    val searchResults = listOf(
                        mapOf(
                            "title" to "Mastra - 人工智能助手",
                            "url" to "https://mastra.ai",
                            "snippet" to "Mastra是一个强大的人工智能助手，可以帮助您完成各种任务。"
                        ),
                        mapOf(
                            "title" to "Model Context Protocol (MCP) - 官方文档",
                            "url" to "https://modelcontextprotocol.io",
                            "snippet" to "Model Context Protocol (MCP) 是一个开放标准，用于定义大型语言模型与外部工具和数据源的交互。"
                        ),
                        mapOf(
                            "title" to "Kastrax - 开源AI框架",
                            "url" to "https://kastrax.ai",
                            "snippet" to "Kastrax是一个开源的AI框架，提供了丰富的工具和库，帮助开发者构建智能应用。"
                        ),
                        mapOf(
                            "title" to "MCP服务器示例 - GitHub",
                            "url" to "https://github.com/modelcontextprotocol/servers",
                            "snippet" to "这个仓库包含了Model Context Protocol (MCP)的参考实现和示例服务器。"
                        ),
                        mapOf(
                            "title" to "Docker MCP服务器 - GitHub",
                            "url" to "https://github.com/docker/mcp-servers",
                            "snippet" to "Docker官方维护的MCP服务器集合，包括Fetch、GitHub、PostgreSQL等多种服务器实现。"
                        )
                    )
                    
                    // 根据查询过滤结果（实际应用中应使用真实的搜索API）
                    val filteredResults = searchResults.filter { 
                        it["title"]!!.contains(query, ignoreCase = true) || 
                        it["snippet"]!!.contains(query, ignoreCase = true) 
                    }.take(numResults)
                    
                    // 格式化结果
                    if (filteredResults.isEmpty()) {
                        "No results found for query: $query"
                    } else {
                        val formattedResults = filteredResults.mapIndexed { index, result ->
                            "${index + 1}. [${result["title"]}](${result["url"]})\n   ${result["snippet"]}"
                        }.joinToString("\n\n")
                        
                        "Search results for: $query\n\n$formattedResults"
                    }
                } catch (e: Exception) {
                    "Error performing search: ${e.message}"
                }
            }
        }
    }

    // 启动服务器
    server.startSSE(port = 8080)
    println("MCP服务器已启动在端口8080")

    // 等待服务器启动完成
    Thread.sleep(1000)

    // 创建MCP客户端
    val client = mcpClient {
        // 设置客户端名称和版本
        name("MastraFetchMCPClient")
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

        // 使用search工具
        println("\n使用search工具搜索'MCP'...")
        val searchResult = client.callTool("search", mapOf("query" to "MCP", "num_results" to 3))
        println("搜索结果:")
        println(searchResult)

        // 使用fetch工具
        println("\n使用fetch工具获取'https://modelcontextprotocol.io'...")
        val fetchResult = client.callTool("fetch", mapOf("url" to "https://modelcontextprotocol.io", "max_length" to 1000))
        println("获取结果:")
        println(fetchResult)

        // 使用fetch工具获取更多内容
        println("\n使用fetch工具获取更多内容（start_index=1000）...")
        val moreFetchResult = client.callTool("fetch", mapOf("url" to "https://modelcontextprotocol.io", "max_length" to 1000, "start_index" to 1000))
        println("获取更多结果:")
        println(moreFetchResult)

        // 使用fetch工具获取原始内容
        println("\n使用fetch工具获取原始内容...")
        val rawFetchResult = client.callTool("fetch", mapOf("url" to "https://modelcontextprotocol.io", "max_length" to 500, "raw" to true))
        println("原始内容结果:")
        println(rawFetchResult)

    } catch (e: Exception) {
        println("发生错误: ${e.message}")
        e.printStackTrace()
    } finally {
        // 断开连接
        client.disconnect()
        server.stop()
        println("已停止MCP服务器和客户端")
        
        // 添加一个延迟，确保所有资源都被释放
        kotlinx.coroutines.delay(1000)
        
        // 强制退出程序
        kotlin.system.exitProcess(0)
    }
}

/**
 * 简单的HTML到Markdown转换函数
 * 注意：这只是一个非常基础的实现，实际应用中应使用更复杂的库
 */
fun convertHtmlToMarkdown(html: String): String {
    var markdown = html
    
    // 移除HTML标签
    markdown = markdown.replace(Regex("<head>.*?</head>", RegexOption.DOT_MATCHES_ALL), "")
    markdown = markdown.replace(Regex("<script>.*?</script>", RegexOption.DOT_MATCHES_ALL), "")
    markdown = markdown.replace(Regex("<style>.*?</style>", RegexOption.DOT_MATCHES_ALL), "")
    
    // 转换标题
    markdown = markdown.replace(Regex("<h1[^>]*>(.*?)</h1>", RegexOption.DOT_MATCHES_ALL), "# $1\n\n")
    markdown = markdown.replace(Regex("<h2[^>]*>(.*?)</h2>", RegexOption.DOT_MATCHES_ALL), "## $1\n\n")
    markdown = markdown.replace(Regex("<h3[^>]*>(.*?)</h3>", RegexOption.DOT_MATCHES_ALL), "### $1\n\n")
    markdown = markdown.replace(Regex("<h4[^>]*>(.*?)</h4>", RegexOption.DOT_MATCHES_ALL), "#### $1\n\n")
    markdown = markdown.replace(Regex("<h5[^>]*>(.*?)</h5>", RegexOption.DOT_MATCHES_ALL), "##### $1\n\n")
    markdown = markdown.replace(Regex("<h6[^>]*>(.*?)</h6>", RegexOption.DOT_MATCHES_ALL), "###### $1\n\n")
    
    // 转换段落
    markdown = markdown.replace(Regex("<p[^>]*>(.*?)</p>", RegexOption.DOT_MATCHES_ALL), "$1\n\n")
    
    // 转换链接
    markdown = markdown.replace(Regex("<a[^>]*href=[\"'](.*?)[\"'][^>]*>(.*?)</a>", RegexOption.DOT_MATCHES_ALL), "[$2]($1)")
    
    // 转换列表
    markdown = markdown.replace(Regex("<li[^>]*>(.*?)</li>", RegexOption.DOT_MATCHES_ALL), "* $1\n")
    
    // 转换强调
    markdown = markdown.replace(Regex("<strong[^>]*>(.*?)</strong>", RegexOption.DOT_MATCHES_ALL), "**$1**")
    markdown = markdown.replace(Regex("<b[^>]*>(.*?)</b>", RegexOption.DOT_MATCHES_ALL), "**$1**")
    markdown = markdown.replace(Regex("<em[^>]*>(.*?)</em>", RegexOption.DOT_MATCHES_ALL), "*$1*")
    markdown = markdown.replace(Regex("<i[^>]*>(.*?)</i>", RegexOption.DOT_MATCHES_ALL), "*$1*")
    
    // 移除其他HTML标签
    markdown = markdown.replace(Regex("<[^>]*>"), "")
    
    // 解码HTML实体
    markdown = markdown.replace("&lt;", "<")
    markdown = markdown.replace("&gt;", ">")
    markdown = markdown.replace("&amp;", "&")
    markdown = markdown.replace("&quot;", "\"")
    markdown = markdown.replace("&apos;", "'")
    markdown = markdown.replace("&nbsp;", " ")
    
    // 移除多余的空行
    markdown = markdown.replace(Regex("\n{3,}"), "\n\n")
    
    return markdown.trim()
}
