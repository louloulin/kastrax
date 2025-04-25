package ai.kastrax.mcp.examples

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.mcp.client.mcpClient
import ai.kastrax.mcp.integration.mcpTools
import ai.kastrax.mcp.protocol.ResourceType
import ai.kastrax.mcp.server.mcpServer
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * 高级MCP与KastraX代理集成示例
 * 演示如何将MCP工具、资源和提示与KastraX代理集成
 */
fun main() = runBlocking {
    println("启动高级MCP与KastraX代理集成示例...")

    // 创建MCP服务器
    val server = mcpServer {
        // 设置服务器名称和版本
        name("AdvancedMCPAgentServer")
        version("1.0.0")

        // 添加知识库资源
        resource {
            name = "knowledge_base"
            description = "包含各种主题的知识"
            content = """
                {
                  "topics": {
                    "人工智能": {
                      "definition": "人工智能（AI）是计算机科学的一个分支，致力于创建能够执行通常需要人类智能的任务的系统。",
                      "applications": ["自然语言处理", "计算机视觉", "机器人技术", "专家系统"],
                      "history": "人工智能研究始于20世纪50年代，当时计算机科学家开始探索计算机是否可以'思考'。"
                    },
                    "机器学习": {
                      "definition": "机器学习是人工智能的一个子领域，专注于开发能够从数据中学习的算法和统计模型。",
                      "types": ["监督学习", "无监督学习", "强化学习"],
                      "algorithms": ["线性回归", "决策树", "神经网络", "支持向量机"]
                    },
                    "自然语言处理": {
                      "definition": "自然语言处理（NLP）是人工智能的一个分支，专注于使计算机能够理解、解释和生成人类语言。",
                      "applications": ["机器翻译", "情感分析", "文本摘要", "问答系统"],
                      "challenges": ["歧义性", "上下文理解", "语言多样性"]
                    }
                  }
                }
            """.trimIndent()
        }

        // 添加城市数据资源
        resource {
            name = "city_data"
            description = "全球主要城市的信息"
            content = """
                {
                  "cities": {
                    "北京": {
                      "country": "中国",
                      "population": 21540000,
                      "area": 16410,
                      "landmarks": ["故宫", "长城", "天坛", "颐和园"],
                      "weather": {"temperature": 25, "condition": "晴天", "humidity": 60}
                    },
                    "上海": {
                      "country": "中国",
                      "population": 24280000,
                      "area": 6340,
                      "landmarks": ["外滩", "东方明珠", "豫园", "静安寺"],
                      "weather": {"temperature": 28, "condition": "多云", "humidity": 70}
                    },
                    "纽约": {
                      "country": "美国",
                      "population": 8380000,
                      "area": 783.8,
                      "landmarks": ["自由女神像", "中央公园", "帝国大厦", "时代广场"],
                      "weather": {"temperature": 20, "condition": "晴天", "humidity": 55}
                    },
                    "伦敦": {
                      "country": "英国",
                      "population": 8980000,
                      "area": 1572,
                      "landmarks": ["大本钟", "伦敦眼", "白金汉宫", "大英博物馆"],
                      "weather": {"temperature": 15, "condition": "多云", "humidity": 75}
                    }
                  }
                }
            """.trimIndent()
        }

        // 添加提示模板
        prompt {
            name = "city_info_template"
            description = "用于生成城市信息报告的模板"
            content = """
                {{city}}城市信息报告：

                国家：{{country}}
                人口：{{population}}人
                面积：{{area}}平方公里

                著名地标：
                {{landmarks}}

                当前天气：
                温度：{{temperature}}°C
                天气状况：{{condition}}
                湿度：{{humidity}}%

                希望这些信息对您有所帮助！
            """.trimIndent()
        }

        prompt {
            name = "topic_info_template"
            description = "用于生成主题信息报告的模板"
            content = """
                关于"{{topic}}"的信息：

                定义：
                {{definition}}

                {{additional_info}}

                希望这些信息对您有所帮助！
            """.trimIndent()
        }

        // 添加知识查询工具
        tool {
            name = "query_knowledge"
            description = "查询知识库中的信息"

            // 添加参数
            parameters {
                parameter {
                    name = "topic"
                    description = "要查询的主题"
                    type = "string"
                    required = true
                }
                parameter {
                    name = "aspect"
                    description = "要查询的特定方面（可选）"
                    type = "string"
                    required = false
                }
            }

            // 设置执行函数
            handler { params ->
                val topic = params["topic"] as? String ?: "未知主题"
                val aspect = params["aspect"] as? String
                println("执行query_knowledge工具，主题: $topic, 方面: $aspect")

                "关于${topic}的信息：人工智能是计算机科学的一个分支，致力于创建能够执行通常需要人类智能的任务的系统。"
            }
        }

        // 添加城市信息工具
        tool {
            name = "city_info"
            description = "获取城市的详细信息"

            // 添加参数
            parameters {
                parameter {
                    name = "city"
                    description = "城市名称"
                    type = "string"
                    required = true
                }
                parameter {
                    name = "info_type"
                    description = "要获取的信息类型（可选）"
                    type = "string"
                    required = false
                }
            }

            // 设置执行函数
            handler { params ->
                val city = params["city"] as? String ?: "未知城市"
                val infoType = params["info_type"] as? String
                println("执行city_info工具，城市: $city, 信息类型: $infoType")

                "${city}城市信息报告：\n国家：中国\n人口：21540000人\n面积：16410平方公里\n著名地标：故宫、长城、天坛、颐和园"
            }
        }

        // 添加计算器工具
        tool {
            name = "calculator"
            description = "执行数学计算"

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
    server.startSSE(port = 8080)
    println("MCP服务器已启动在端口8080")

    // 等待服务器启动完成
    Thread.sleep(1000)

    // 创建MCP客户端
    val client = mcpClient {
        // 设置客户端名称和版本
        name("AdvancedMCPAgentClient")
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
            你是一个有用的助手，可以回答用户的问题。请使用可用的工具来回答以下问题：

            1. 北京的详细信息是什么？
            2. 什么是人工智能，它有哪些应用？
            3. 计算 (15 + 25) * 2 的结果是多少？
            4. 上海有哪些著名地标？
            5. 自然语言处理的定义和应用是什么？

            请使用适当的工具来获取这些信息，然后提供一个综合的回答。
        """.trimIndent()

        val response = agent.generate(prompt, AgentGenerateOptions())

        println("\n代理回答:")
        println(response.text)

        // 显示工具调用
        println("\n工具调用:")
        response.toolCalls.forEach { toolCall ->
            println("- ${toolCall.name}: ${toolCall.arguments}")
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
