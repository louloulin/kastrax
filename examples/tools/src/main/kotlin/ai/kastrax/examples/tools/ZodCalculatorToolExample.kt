package ai.kastrax.examples.tools

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking
import kotlin.math.*

/**
 * 高级计算器输入
 */
data class AdvancedCalculatorInput(
    val operation: String,
    val a: Double,
    val b: Double? = null
)

/**
 * 高级计算器输出
 */
data class AdvancedCalculatorOutput(
    val result: Double,
    val operation: String,
    val explanation: String
)

/**
 * Zod 计算器工具示例
 */
fun main() = runBlocking {
    println("Zod 高级计算器工具示例")
    println("=====================")

    // 创建高级计算器工具
    val advancedCalculatorTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "advanced_calculator"
        name = "高级计算器"
        description = """
            执行各种数学运算，包括基本运算和高级函数。

            支持的操作:
            - add: 加法 (a + b)
            - subtract: 减法 (a - b)
            - multiply: 乘法 (a * b)
            - divide: 除法 (a / b)
            - power: 幂运算 (a ^ b)
            - sqrt: 平方根 (√a)
            - sin: 正弦函数 (sin(a))，a 以弧度为单位
            - cos: 余弦函数 (cos(a))，a 以弧度为单位
            - tan: 正切函数 (tan(a))，a 以弧度为单位
            - log: 自然对数 (ln(a))
            - log10: 常用对数 (log10(a))
        """.trimIndent()

        // 定义输入模式
        inputSchema = obj {
            field("operation", string {
                enum(
                    "add", "subtract", "multiply", "divide",
                    "power", "sqrt", "sin", "cos", "tan",
                    "log", "log10"
                )
            })
            field("a", number())
            field("b", number(), required = false)
        }

        // 定义输出模式
        outputSchema = obj {
            field("result", number())
            field("operation", string())
            field("explanation", string())
        }

        // 实现执行逻辑
        execute = { input ->
            val operation = input["operation"] as String
            val a = (input["a"] as Number).toDouble()
            val b = (input["b"] as? Number)?.toDouble()

            // 验证需要两个操作数的操作
            if (operation in listOf("add", "subtract", "multiply", "divide", "power") && b == null) {
                throw IllegalArgumentException("操作 '$operation' 需要两个操作数")
            }

            val result: Double
            val explanation: String

            when (operation) {
                "add" -> {
                    result = a + b!!
                    explanation = "$a + $b = $result"
                }
                "subtract" -> {
                    result = a - b!!
                    explanation = "$a - $b = $result"
                }
                "multiply" -> {
                    result = a * b!!
                    explanation = "$a × $b = $result"
                }
                "divide" -> {
                    if (b == 0.0) {
                        throw IllegalArgumentException("除数不能为零")
                    }
                    result = a / b!!
                    explanation = "$a ÷ $b = $result"
                }
                "power" -> {
                    result = a.pow(b!!)
                    explanation = "$a ^ $b = $result"
                }
                "sqrt" -> {
                    if (a < 0) {
                        throw IllegalArgumentException("不能对负数进行平方根运算")
                    }
                    result = sqrt(a)
                    explanation = "√$a = $result"
                }
                "sin" -> {
                    result = sin(a)
                    explanation = "sin($a) = $result"
                }
                "cos" -> {
                    result = cos(a)
                    explanation = "cos($a) = $result"
                }
                "tan" -> {
                    result = tan(a)
                    explanation = "tan($a) = $result"
                }
                "log" -> {
                    if (a <= 0) {
                        throw IllegalArgumentException("对数运算的参数必须为正数")
                    }
                    result = ln(a)
                    explanation = "ln($a) = $result"
                }
                "log10" -> {
                    if (a <= 0) {
                        throw IllegalArgumentException("对数运算的参数必须为正数")
                    }
                    result = log10(a)
                    explanation = "log10($a) = $result"
                }
                else -> {
                    throw IllegalArgumentException("不支持的操作: $operation")
                }
            }

            mapOf(
                "result" to result,
                "operation" to operation,
                "explanation" to explanation
            )
        }
    }

    // 测试基本运算
    println("\n测试基本运算:")
    val basicOperations = listOf(
        mapOf("operation" to "add", "a" to 10.0, "b" to 5.0),
        mapOf("operation" to "subtract", "a" to 10.0, "b" to 5.0),
        mapOf("operation" to "multiply", "a" to 10.0, "b" to 5.0),
        mapOf("operation" to "divide", "a" to 10.0, "b" to 5.0)
    )

    for (input in basicOperations) {
        val output = advancedCalculatorTool.execute(input)
        println("${output["explanation"]}")
    }

    // 测试高级函数
    println("\n测试高级函数:")
    val advancedOperations = listOf(
        mapOf("operation" to "power", "a" to 2.0, "b" to 3.0),
        mapOf("operation" to "sqrt", "a" to 16.0),
        mapOf("operation" to "sin", "a" to PI/2),
        mapOf("operation" to "cos", "a" to 0.0),
        mapOf("operation" to "log", "a" to 2.718281828459045),
        mapOf("operation" to "log10", "a" to 100.0)
    )

    for (input in advancedOperations) {
        val output = advancedCalculatorTool.execute(input)
        println("${output["explanation"]}")
    }

    // 测试错误处理
    println("\n测试错误处理:")
    try {
        val errorInput = mapOf("operation" to "divide", "a" to 10.0, "b" to 0.0)
        advancedCalculatorTool.execute(errorInput)
    } catch (e: Exception) {
        println("捕获到异常: ${e.message}")
    }

    try {
        val errorInput = mapOf("operation" to "sqrt", "a" to -4.0)
        advancedCalculatorTool.execute(errorInput)
    } catch (e: Exception) {
        println("捕获到异常: ${e.message}")
    }

    println("\nZod 高级计算器工具示例完成")
}
