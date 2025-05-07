package ai.kastrax.mcp.examples

import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.protocol.ResourceType
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 全面的MCP示例
 * 演示MCP的各种功能，包括工具、资源和提示
 */
fun main() = runBlocking {
    // 查找可用端口
    val serverPort = ai.kastrax.core.utils.NetworkUtils.findAvailablePort()
    println("启动全面的MCP示例...")

    // 创建MCP服务器
    val server = mcpServer {
        // 设置服务器名称和版本
        name("ComprehensiveMCPServer")
        version("1.0.0")

        // 添加资源
        resource {
            name = "weather_data"
            description = "全球主要城市的天气数据"
            content = """
                {
                  "cities": {
                    "北京": {"temperature": 25, "condition": "晴天", "humidity": 60},
                    "上海": {"temperature": 28, "condition": "多云", "humidity": 70},
                    "广州": {"temperature": 30, "condition": "雨", "humidity": 80},
                    "纽约": {"temperature": 20, "condition": "晴天", "humidity": 55},
                    "伦敦": {"temperature": 15, "condition": "多云", "humidity": 75}
                  }
                }
            """.trimIndent()
        }

        resource {
            name = "search_data"
            description = "常见搜索关键词的结果"
            content = """
                {
                  "keywords": {
                    "人工智能": "人工智能（AI）是计算机科学的一个分支，致力于创建能够执行通常需要人类智能的任务的系统。",
                    "机器学习": "机器学习是人工智能的一个子领域，专注于开发能够从数据中学习的算法和统计模型。",
                    "深度学习": "深度学习是机器学习的一个子领域，使用多层神经网络来模拟人脑的学习过程。",
                    "自然语言处理": "自然语言处理（NLP）是人工智能的一个分支，专注于使计算机能够理解、解释和生成人类语言。"
                  }
                }
            """.trimIndent()
        }

        // 添加提示
        prompt {
            name = "weather_prompt"
            description = "用于生成天气报告的提示"
            content = """
                {{city}}的天气情况：
                温度：{{temperature}}°C
                天气状况：{{condition}}
                湿度：{{humidity}}%
            """.trimIndent()
        }

        prompt {
            name = "search_prompt"
            description = "用于生成搜索结果的提示"
            content = """
                关于"{{keyword}}"的搜索结果：

                {{result}}

                希望这对您有所帮助！
            """.trimIndent()
        }

        // 添加天气工具
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

                "${city}的天气情况：\n温度：25°C\n天气状况：晴天\n湿度：60%"
            }
        }

        // 添加搜索工具
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

                "关于\"${query}\"的搜索结果：\n\n人工智能（AI）是计算机科学的一个分支，致力于创建能够执行通常需要人类智能的任务的系统。\n\n希望这对您有所帮助！"
            }
        }

        // 添加计算器工具
        tool {
            name = "calculator"
            description = "执行简单的数学计算"

            // 添加参数
            parameters {
                parameter {
                    name = "expression"
                    description = "数学表达式"
                    type = "string"
                    required = true
                }
            }

            // 设置执行函数
            handler { params ->
                val expression = params["expression"] as? String ?: "0"
                println("执行calculator工具，表达式: $expression")

                try {
                    // 使用简单的表达式求值
                    val result = javax.script.ScriptEngineManager().getEngineByName("JavaScript").eval(expression) as Double
                    "计算结果: $result"
                } catch (e: Exception) {
                    "计算错误: ${e.message}"
                }
            }
        }
    }

    // 启动服务器
    server.startSSE(port = serverPort)
    println("MCP服务器已启动在端口$serverPort")

    // 等待服务器启动完成
    Thread.sleep(1000)

    // 创建MCP客户端
    val client = mcpClient {
        // 设置客户端名称和版本
        name("ComprehensiveMCPClient")
        version("1.0.0")

        // 使用本地服务器
        server {
            sse {
                url = "http://localhost:$serverPort"
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

        // 获取可用资源
        val resources = client.resources()
        println("\n可用资源:")
        resources.forEach { resource ->
            println("- ${resource.id}: ${resource.name} (${resource.description})")
        }

        // 获取资源内容
        val weatherData = client.getResource("weather_data")
        println("\n天气数据资源内容:")
        println(weatherData)

        // 获取可用提示
        val prompts = client.prompts()
        println("\n可用提示:")
        prompts.forEach { prompt ->
            println("- ${prompt.id}: ${prompt.name} (${prompt.description})")
        }

        // 获取提示内容
        val weatherPrompt = client.getPrompt("weather_prompt")
        println("\n天气提示内容:")
        println(weatherPrompt)

        // 获取可用工具
        val tools = client.tools()
        println("\n可用工具:")
        tools.forEach { tool ->
            println("- ${tool.name}: ${tool.description}")
        }

        // 调用天气工具
        println("\n调用天气工具...")
        val weatherResult = client.callTool("get_weather", mapOf("city" to "北京"))
        println("天气工具结果:")
        println(weatherResult)

        // 调用搜索工具
        println("\n调用搜索工具...")
        val searchResult = client.callTool("search", mapOf("query" to "人工智能"))
        println("搜索工具结果:")
        println(searchResult)

        // 调用计算器工具
        println("\n调用计算器工具...")
        val calculatorResult = client.callTool("calculator", mapOf("expression" to "2 + 2 * 3"))
        println("计算器工具结果:")
        println(calculatorResult)

        // 测试错误处理
        println("\n测试错误处理...")
        try {
            val errorResult = client.callTool("nonexistent_tool", mapOf("param" to "value"))
            println("结果: $errorResult")
        } catch (e: Exception) {
            println("预期的错误: ${e.message}")
        }

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
