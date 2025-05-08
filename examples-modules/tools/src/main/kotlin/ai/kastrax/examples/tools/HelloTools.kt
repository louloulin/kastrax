package ai.kastrax.examples.tools

import ai.kastrax.core.agent.agent
import ai.kastrax.core.tool.Tool
import ai.kastrax.core.tool.ToolDefinition
import ai.kastrax.core.tool.ToolParameter
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive

/**
 * 一个简单的HelloTools示例
 */
fun main() = runBlocking {
    println("Hello Tools 示例")
    println("-------------------")

    // 创建一个简单的计算器工具
    val calculatorTool = object : Tool {
        override val definition = ToolDefinition(
            name = "calculator",
            description = "一个简单的计算器，可以进行基本的数学运算。",
            parameters = listOf(
                ToolParameter(
                    name = "expression",
                    description = "要计算的数学表达式，如 '2 + 2'",
                    type = "string",
                    required = true
                )
            )
        )

        override suspend fun execute(parameters: Map<String, Any?>): Map<String, Any?> {
            val expression = parameters["expression"]?.toString() ?: ""

            // 简单的计算器实现，只支持基本运算
            val result = try {
                when {
                    "+" in expression -> {
                        val parts = expression.split("+")
                        val num1 = parts[0].trim().toDouble()
                        val num2 = parts[1].trim().toDouble()
                        num1 + num2
                    }
                    "-" in expression -> {
                        val parts = expression.split("-")
                        val num1 = parts[0].trim().toDouble()
                        val num2 = parts[1].trim().toDouble()
                        num1 - num2
                    }
                    "*" in expression -> {
                        val parts = expression.split("*")
                        val num1 = parts[0].trim().toDouble()
                        val num2 = parts[1].trim().toDouble()
                        num1 * num2
                    }
                    "/" in expression -> {
                        val parts = expression.split("/")
                        val num1 = parts[0].trim().toDouble()
                        val num2 = parts[1].trim().toDouble()
                        if (num2 == 0.0) throw ArithmeticException("除数不能为零")
                        num1 / num2
                    }
                    else -> throw IllegalArgumentException("不支持的运算: $expression")
                }
            } catch (e: Exception) {
                return mapOf("error" to "\u8ba1\u7b97\u9519\u8bef: ${e.message}")
            }

            return mapOf("result" to result)
        }
    }

    // 创建一个使用 Deepseek 的代理
    val myAgent = agent {
        name = "工具助手"
        instructions = "你是一个有用的助手，可以使用各种工具来帮助用户。"

        // 使用 Deepseek 模型
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key-here")
            temperature(0.7)
            maxTokens(2000)
            timeout(60000) // 60秒超时
        }

        // 添加工具
        tools {
            tool(calculatorTool)
        }
    }

    // 测试工具
    println("\n直接测试计算器工具:")
    val result = calculatorTool.execute(mapOf("expression" to "5 + 3"))
    println("计算结果: ${result["result"]}")

    // 使用代理生成回答
    println("\n使用代理生成回答:")
    val response = myAgent.generate("请帮我计算 12 * 7 是多少？")

    // 打印回答
    println("\n代理回答:")
    println(response.text)

    println("\nHello Tools 示例完成")
}
