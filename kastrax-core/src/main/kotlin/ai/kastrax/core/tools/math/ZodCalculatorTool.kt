package ai.kastrax.core.tools.math

import ai.kastrax.core.common.KastraXBase
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.ZodTool
import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.Schema
import ai.kastrax.zod.any
import ai.kastrax.zod.objectInput
import ai.kastrax.zod.objectOutput
import ai.kastrax.zod.stringField
import ai.kastrax.zod.numberField
import kotlinx.serialization.Serializable
import mu.KotlinLogging
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.math.sin
import kotlin.math.cos
import kotlin.math.tan
import kotlin.math.log10
import kotlin.math.ln

/**
 * 计算器工具，用于执行基本的数学运算。
 *
 * 支持的操作包括：
 * - 加法 (add)
 * - 减法 (subtract)
 * - 乘法 (multiply)
 * - 除法 (divide)
 * - 幂运算 (power)
 * - 平方根 (sqrt)
 * - 三角函数 (sin, cos, tan)
 * - 对数 (log10, ln)
 */
class ZodCalculatorTool : KastraXBase(component = "TOOL", name = "计算器") {

    private val customLogger = KotlinLogging.logger {}

    /**
     * 计算器工具的输入参数。
     */
    @Serializable
    data class CalculatorInput(
        val operation: String,
        val a: Double,
        val b: Double? = null
    )

    /**
     * 计算器工具的输出结果。
     */
    @Serializable
    data class CalculatorOutput(
        val result: Double,
        val expression: String
    )

    /**
     * 创建计算器工具的 ZodTool 实例。
     */
    fun createZodTool(): ZodTool<CalculatorInput, CalculatorOutput> {
        return zodTool {
            id = "calculator"
            name = "计算器"
            description = """
                执行基本的数学运算。

                支持的操作包括：
                - add: 加法 (a + b)
                - subtract: 减法 (a - b)
                - multiply: 乘法 (a * b)
                - divide: 除法 (a / b)
                - power: 幂运算 (a ^ b)
                - sqrt: 平方根 (√a)
                - sin: 正弦函数 (sin(a))
                - cos: 余弦函数 (cos(a))
                - tan: 正切函数 (tan(a))
                - log10: 常用对数 (log10(a))
                - ln: 自然对数 (ln(a))
            """.trimIndent()



            // 使用类型安全的方式设置输入和输出模式
            @Suppress("UNCHECKED_CAST")
            this.inputSchema = any() as Schema<CalculatorInput, CalculatorInput>

            @Suppress("UNCHECKED_CAST")
            this.outputSchema = any() as Schema<CalculatorOutput, CalculatorOutput>

            execute = { input ->
                customLogger.info { "执行计算器操作: ${input.operation} 参数: a=${input.a}, b=${input.b}" }

                val operation = input.operation
                val a = input.a
                val b = input.b

                when (operation) {
                    "add" -> {
                        requireNotNull(b) { "加法操作需要两个操作数" }
                        val result = a + b
                        CalculatorOutput(result, "$a + $b = $result")
                    }
                    "subtract" -> {
                        requireNotNull(b) { "减法操作需要两个操作数" }
                        val result = a - b
                        CalculatorOutput(result, "$a - $b = $result")
                    }
                    "multiply" -> {
                        requireNotNull(b) { "乘法操作需要两个操作数" }
                        val result = a * b
                        CalculatorOutput(result, "$a * $b = $result")
                    }
                    "divide" -> {
                        requireNotNull(b) { "除法操作需要两个操作数" }
                        require(b != 0.0) { "除数不能为零" }
                        val result = a / b
                        CalculatorOutput(result, "$a / $b = $result")
                    }
                    "power" -> {
                        requireNotNull(b) { "幂运算需要两个操作数" }
                        val result = a.pow(b)
                        CalculatorOutput(result, "$a ^ $b = $result")
                    }
                    "sqrt" -> {
                        require(a >= 0) { "平方根操作需要非负数" }
                        val result = sqrt(a)
                        CalculatorOutput(result, "√$a = $result")
                    }
                    "sin" -> {
                        val result = sin(a)
                        CalculatorOutput(result, "sin($a) = $result")
                    }
                    "cos" -> {
                        val result = cos(a)
                        CalculatorOutput(result, "cos($a) = $result")
                    }
                    "tan" -> {
                        val result = tan(a)
                        CalculatorOutput(result, "tan($a) = $result")
                    }
                    "log10" -> {
                        require(a > 0) { "对数操作需要正数" }
                        val result = log10(a)
                        CalculatorOutput(result, "log10($a) = $result")
                    }
                    "ln" -> {
                        require(a > 0) { "自然对数操作需要正数" }
                        val result = ln(a)
                        CalculatorOutput(result, "ln($a) = $result")
                    }
                    else -> throw IllegalArgumentException("不支持的操作: $operation")
                }
            }
        }
    }

    /**
     * 创建计算器工具的 Tool 实例。
     */
    fun createTool(): Tool {
        return createZodTool().toTool()
    }

    companion object {
        /**
         * 创建计算器工具。
         *
         * @return 计算器工具实例
         */
        fun create(): Tool {
            return ZodCalculatorTool().createTool()
        }

        /**
         * 创建计算器 ZodTool 实例。
         *
         * @return 计算器 ZodTool 实例
         */
        fun createZodTool(): ZodTool<CalculatorInput, CalculatorOutput> {
            return ZodCalculatorTool().createZodTool()
        }
    }
}
