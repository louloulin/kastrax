package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.core.tools.tool
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import kotlin.math.sqrt

/**
 * DeepSeek 流式响应示例，展示如何使用改进的 DeepSeek 流式响应。
 *
 * 该示例使用了细粒度更高的流式处理方法，可以实现真正的逐字符实时显示。
 * 我们修复了 DeepSeekMessage 类中 role 字段的问题，并优化了 SSE 流式处理逻辑。
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

    // 创建 DeepSeek 代理
    val myAgent = agent {
        name = "DeepSeek 流式助手"
        instructions = "你是一个有帮助的助手，可以回答问题和执行计算。"

        // 使用 DeepSeek 模型
        model = deepSeek {
            model("deepseek-chat")
            // 确保 API 密钥格式正确，不包含多余的空格或特殊字符
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79".trim())
            // 模型参数在请求时设置
        }

        // 添加工具
        tools {
            tool(calculatorTool)
        }
    }

    println("DeepSeek 流式响应示例 - 自动执行模式")
    println("-------------------")

    // 预定义的问题列表
    val questions = listOf(
        "2+2等于多少？",
        "什么是人工智能？",
        "计算平方根 16",
        "用中文解释量子力学的基本原理"
    )

    // 自动执行每个问题
    for (question in questions) {
        println("\n问题: $question")
        println("DeepSeek 正在思考...")

        // 使用流式响应
        print("\n回答: ")
        try {
            // 使用 agent 的 stream 方法获取流式响应
            val response = myAgent.stream(question)

            // 收集流式响应的内容
            response.textStream?.collect { chunk ->
                print(chunk)
                System.out.flush()  // 立即刷新输出缓冲区，确保实时显示
            }
        } catch (e: Exception) {
            println("\n流式响应出错: ${e.message}")
            e.printStackTrace()
        }

        println("\n-------------------")

        // 添加延时，避免请求过快
        Thread.sleep(1000)
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
