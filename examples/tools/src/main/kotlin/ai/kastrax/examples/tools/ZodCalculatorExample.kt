package ai.kastrax.examples.tools

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking

/**
 * 计算器输入数据类
 */
data class CalculatorInput(
    val operation: String,
    val a: Double,
    val b: Double
)

/**
 * 计算器输出数据类
 */
data class CalculatorOutput(
    val result: Double
)

/**
 * Zod 计算器工具示例
 */
fun main() = runBlocking {
    println("Zod 计算器工具示例")
    println("=================")

    // 创建一个使用数据类的计算器工具
    val calculatorTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "calculator"
        name = "计算器"
        description = "执行基本的数学运算"

        // 定义输入模式
        inputSchema = obj {
            field("operation", string {
                enum("add", "subtract", "multiply", "divide")
            })
            field("a", number())
            field("b", number())
        }

        // 定义输出模式
        outputSchema = obj {
            field("result", number())
        }

        // 实现执行逻辑
        execute = { input ->
            val operation = input["operation"] as String
            val a = (input["a"] as Number).toDouble()
            val b = (input["b"] as Number).toDouble()

            val result = when (operation) {
                "add" -> a + b
                "subtract" -> a - b
                "multiply" -> a * b
                "divide" -> a / b
                else -> throw IllegalArgumentException("不支持的操作: $operation")
            }

            mapOf("result" to result)
        }
    }

    // 测试计算器工具
    println("\n测试加法:")
    val addInput = mapOf("operation" to "add", "a" to 5.0, "b" to 3.0)
    val addResult = calculatorTool.execute(addInput)
    println("5 + 3 = ${addResult["result"]}")

    println("\n测试减法:")
    val subtractInput = mapOf("operation" to "subtract", "a" to 10.0, "b" to 4.0)
    val subtractResult = calculatorTool.execute(subtractInput)
    println("10 - 4 = ${subtractResult["result"]}")

    println("\n测试乘法:")
    val multiplyInput = mapOf("operation" to "multiply", "a" to 6.0, "b" to 7.0)
    val multiplyResult = calculatorTool.execute(multiplyInput)
    println("6 * 7 = ${multiplyResult["result"]}")

    println("\n测试除法:")
    val divideInput = mapOf("operation" to "divide", "a" to 20.0, "b" to 5.0)
    val divideResult = calculatorTool.execute(divideInput)
    println("20 / 5 = ${divideResult["result"]}")

    println("\nZod 计算器工具示例完成")
}
