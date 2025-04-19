package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.tools.tool
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.math.sqrt

/**
 * DeepSeek 集成示例，展示如何使用 DeepSeek 模型。
 */
fun main() = runBlocking {
    // 创建一个计算器工具
    val calculatorTool = tool {
        id = "calculator"
        name = "计算器"
        description = "执行数学计算"

        // 定义输入模式
        inputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("expression") {
                    put("type", "string")
                    put("description", "要计算的数学表达式")
                }
            }
            putJsonArray("required") {
                add("expression")
            }
        }

        // 定义输出模式
        outputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("result") {
                    put("type", "number")
                    put("description", "计算结果")
                }
            }
        }

        // 实现执行逻辑
        execute = { input ->
            val expression = input.jsonObject["expression"]?.jsonPrimitive?.content ?: "0"
            val result = evaluateExpression(expression)

            buildJsonObject {
                put("result", result)
            }
        }
    }

    // 创建一个使用 DeepSeek 的代理
    val myAgent = agent {
        name = "DeepSeek 助手"
        instructions = "你是一个有帮助的助手，可以回答问题和执行计算。"

        // 使用 DeepSeek 模型
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            // 从环境变量获取 API 密钥，或者在这里显式设置
            // apiKey("your-api-key-here")
        }

        // 添加工具
        tools {
            tool(calculatorTool)
        }
    }

    println("DeepSeek 代理示例")
    println("-------------------")
    println("输入问题或计算请求，输入 'exit' 退出")

    while (true) {
        print("\n> ")
        val input = readlnOrNull() ?: ""

        if (input.equals("exit", ignoreCase = true)) {
            break
        }

        // 使用流式响应
        println("\nDeepSeek 正在思考...")

        myAgent.stream(input).collect { chunk ->
            print(chunk)
        }
    }
}

/**
 * 简单的表达式计算器。
 * 注意：这只是一个示例实现，仅支持基本运算。
 */
private fun evaluateExpression(expression: String): Double {
    // 这里使用一个简单的方法来计算表达式
    // 在实际应用中，应该使用更健壮的表达式解析器
    return when {
        "+" in expression -> {
            val parts = expression.split("+")
            parts[0].trim().toDouble() + parts[1].trim().toDouble()
        }
        "-" in expression -> {
            val parts = expression.split("-")
            parts[0].trim().toDouble() - parts[1].trim().toDouble()
        }
        "*" in expression -> {
            val parts = expression.split("*")
            parts[0].trim().toDouble() * parts[1].trim().toDouble()
        }
        "/" in expression -> {
            val parts = expression.split("/")
            parts[0].trim().toDouble() / parts[1].trim().toDouble()
        }
        "sqrt" in expression.lowercase() -> {
            val number = expression.lowercase().replace("sqrt", "").trim()
                .replace("(", "").replace(")", "")
            sqrt(number.toDouble())
        }
        else -> expression.trim().toDouble()
    }
}
