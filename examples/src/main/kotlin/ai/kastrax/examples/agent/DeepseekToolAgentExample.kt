package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.ToolCallResult
import ai.kastrax.integrations.deepseek.DeepSeekModel
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.pow
import kotlin.math.sqrt

/**
 * Deepseek Tool Agent 示例
 *
 * 展示如何使用 Deepseek 作为 LLM 提供商创建带工具的 Agent
 */
fun deepseekToolAgentExample() = runBlocking {
    // 使用 DSL 创建 Deepseek LLM 提供商
    val llm = deepSeek {
        // 设置 API 密钥（也可以从环境变量 DEEPSEEK_API_KEY 获取）
        apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key")

        // 设置模型
        model(DeepSeekModel.DEEPSEEK_CHAT)

        // 设置生成参数
        temperature(0.7)
        maxTokens(2000)
        topP(0.95)

        // 设置超时时间（秒）
        timeout(60)
    }

    // 使用 DSL 创建 Agent
    val agent = agent {
        // 设置 Agent 名称
        name = "DeepseekToolAgent"

        // 设置 Agent 指令
        instructions = """
            你是一个由 Deepseek 驱动的智能助手，可以使用各种工具来帮助用户。
            你应该：
            1. 理解用户的请求，并决定是否需要使用工具
            2. 如果需要使用工具，选择合适的工具并正确调用
            3. 根据工具的结果提供有用的回答
            4. 保持礼貌和专业
        """.trimIndent()

        // 设置 LLM 模型
        model = llm

        // 添加工具
        tools {
            // 计算器工具
            val calculatorTool = ai.kastrax.core.tools.tool {
                id = "calculator"
                name = "计算器"
                description = "执行数学计算，支持加减乘除、平方根和幂运算"

                // 定义参数模式
                inputSchema = kotlinx.serialization.json.Json.parseToJsonElement("""
                    {
                        "type": "object",
                        "properties": {
                            "expression": {
                                "type": "string",
                                "description": "要计算的数学表达式，例如 '2 + 2' 或 'sqrt(16)'"
                            }
                        },
                        "required": ["expression"]
                    }
                """)

                // 实现工具逻辑
                execute = { params ->
                    try {
                        val expression = params.jsonObject["expression"]?.jsonPrimitive?.content
                            ?: throw IllegalArgumentException("缺少表达式参数")

                        // 简单的表达式解析和计算
                        val result = when {
                            "+" in expression -> {
                                val (a, b) = expression.split("+").map { it.trim().toDouble() }
                                a + b
                            }
                            "-" in expression -> {
                                val (a, b) = expression.split("-").map { it.trim().toDouble() }
                                a - b
                            }
                            "*" in expression -> {
                                val (a, b) = expression.split("*").map { it.trim().toDouble() }
                                a * b
                            }
                            "/" in expression -> {
                                val (a, b) = expression.split("/").map { it.trim().toDouble() }
                                if (b == 0.0) {
                                    throw IllegalArgumentException("除数不能为零")
                                }
                                a / b
                            }
                            expression.startsWith("sqrt(") && expression.endsWith(")") -> {
                                val num = expression.substring(5, expression.length - 1).toDouble()
                                if (num < 0) {
                                    throw IllegalArgumentException("不能对负数开平方根")
                                }
                                sqrt(num)
                            }
                            "^" in expression -> {
                                val (a, b) = expression.split("^").map { it.trim().toDouble() }
                                a.pow(b)
                            }
                            else -> {
                                throw IllegalArgumentException("不支持的表达式: $expression")
                            }
                        }

                        kotlinx.serialization.json.JsonPrimitive("计算结果: $result")
                    } catch (e: Exception) {
                        throw e
                    }
                }
            }

            tool(calculatorTool)

            // 日期时间工具
            val datetimeTool = ai.kastrax.core.tools.tool {
                id = "datetime"
                name = "日期时间"
                description = "获取当前日期和时间"

                // 定义参数模式
                inputSchema = kotlinx.serialization.json.Json.parseToJsonElement("""
                    {
                        "type": "object",
                        "properties": {
                            "format": {
                                "type": "string",
                                "description": "日期时间格式，例如 'yyyy-MM-dd HH:mm:ss'",
                                "default": "yyyy-MM-dd HH:mm:ss"
                            }
                        }
                    }
                """)

                // 实现工具逻辑
                execute = { params ->
                    try {
                        val format = params.jsonObject["format"]?.jsonPrimitive?.content ?: "yyyy-MM-dd HH:mm:ss"
                        val formatter = DateTimeFormatter.ofPattern(format)
                        val now = LocalDateTime.now()
                        val formattedDateTime = now.format(formatter)

                        kotlinx.serialization.json.JsonPrimitive("当前日期时间: $formattedDateTime")
                    } catch (e: Exception) {
                        throw e
                    }
                }
            }

            tool(datetimeTool)

            // 天气查询工具（模拟）
            val weatherTool = ai.kastrax.core.tools.tool {
                id = "weather"
                name = "天气查询"
                description = "查询指定城市的天气信息"

                // 定义参数模式
                inputSchema = kotlinx.serialization.json.Json.parseToJsonElement("""
                    {
                        "type": "object",
                        "properties": {
                            "city": {
                                "type": "string",
                                "description": "要查询天气的城市名称"
                            }
                        },
                        "required": ["city"]
                    }
                """)

                // 实现工具逻辑（模拟数据）
                execute = { params ->
                    val city = params.jsonObject["city"]?.jsonPrimitive?.content
                        ?: throw IllegalArgumentException("缺少城市参数")

                    // 模拟天气数据
                    val weatherData = mapOf(
                        "北京" to "晴朗，温度 25°C，湿度 45%，微风",
                        "上海" to "多云，温度 28°C，湿度 60%，微风",
                        "广州" to "小雨，温度 30°C，湿度 75%，微风",
                        "深圳" to "阵雨，温度 29°C，湿度 70%，微风",
                        "杭州" to "晴朗，温度 26°C，湿度 50%，微风"
                    )

                    val weather = weatherData[city] ?: "无法获取 $city 的天气信息"
                    kotlinx.serialization.json.JsonPrimitive("$city 天气: $weather")
                }
            }

            tool(weatherTool)
        }

        // 配置默认生成选项
        defaultGenerateOptions {
            temperature(0.7)
            maxTokens(2000)
        }
    }

    println("=== Deepseek Tool Agent 示例 ===")

    // 准备提问
    val questions = listOf(
        "计算 15 * 7 的结果是多少？",
        "现在的日期和时间是？",
        "北京今天的天气怎么样？",
        "计算 25 的平方根，然后告诉我现在的时间"
    )

    // 逐个提问并获取回答
    questions.forEachIndexed { index, question ->
        println("\n问题 ${index + 1}: $question")

        // 创建生成选项
        val options = AgentGenerateOptions()

        // 生成回答
        val response = agent.generate(question, options)

        println("回答:")
        println(response.text)
    }
}
