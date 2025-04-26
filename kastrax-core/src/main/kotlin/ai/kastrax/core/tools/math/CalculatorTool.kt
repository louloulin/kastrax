package ai.kastrax.core.tools.math

import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
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
class CalculatorTool : Tool {

    private val logger = KotlinLogging.logger {}

    override val id: String = "calculator"
    override val name: String = "计算器"
    override val description: String = """
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

    override val inputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("operation") {
                put("type", "string")
                put("description", "要执行的数学运算")
                putJsonArray("enum") {
                    add(JsonPrimitive("add"))
                    add(JsonPrimitive("subtract"))
                    add(JsonPrimitive("multiply"))
                    add(JsonPrimitive("divide"))
                    add(JsonPrimitive("power"))
                    add(JsonPrimitive("sqrt"))
                    add(JsonPrimitive("sin"))
                    add(JsonPrimitive("cos"))
                    add(JsonPrimitive("tan"))
                    add(JsonPrimitive("log10"))
                    add(JsonPrimitive("ln"))
                }
            }
            putJsonObject("a") {
                put("type", "number")
                put("description", "第一个操作数")
            }
            putJsonObject("b") {
                put("type", "number")
                put("description", "第二个操作数（对于二元运算是必需的）")
            }
        }
        putJsonArray("required") {
            add(JsonPrimitive("operation"))
            add(JsonPrimitive("a"))
        }
    }

    override val outputSchema: JsonObject = buildJsonObject {
        put("type", "object")
        putJsonObject("properties") {
            putJsonObject("result") {
                put("type", "number")
                put("description", "计算结果")
            }
            putJsonObject("expression") {
                put("type", "string")
                put("description", "计算表达式")
            }
        }
    }

    override suspend fun execute(input: JsonElement): JsonElement = withContext(Dispatchers.Default) {
        logger.info { "执行计算器操作: $input" }

        val inputObj = input as? JsonObject ?: throw IllegalArgumentException("Input must be a JSON object")

        val operation = inputObj["operation"]?.let {
            when (it) {
                is JsonPrimitive -> it.content
                else -> it.toString()
            }
        } ?: throw IllegalArgumentException("Operation is required")

        val a = inputObj["a"]?.let {
            when (it) {
                is JsonPrimitive -> it.content.toDoubleOrNull() ?: throw IllegalArgumentException("'a' must be a number")
                else -> throw IllegalArgumentException("'a' must be a number")
            }
        } ?: throw IllegalArgumentException("'a' is required")

        val b = inputObj["b"]?.let {
            when (it) {
                is JsonPrimitive -> it.content.toDoubleOrNull() ?: throw IllegalArgumentException("'b' must be a number")
                else -> throw IllegalArgumentException("'b' must be a number")
            }
        }

        val (result, expression) = when (operation) {
            "add" -> {
                requireNotNull(b) { "加法操作需要两个操作数" }
                val res = a + b
                Pair(res, "$a + $b = $res")
            }
            "subtract" -> {
                requireNotNull(b) { "减法操作需要两个操作数" }
                val res = a - b
                Pair(res, "$a - $b = $res")
            }
            "multiply" -> {
                requireNotNull(b) { "乘法操作需要两个操作数" }
                val res = a * b
                Pair(res, "$a * $b = $res")
            }
            "divide" -> {
                requireNotNull(b) { "除法操作需要两个操作数" }
                require(b != 0.0) { "除数不能为零" }
                val res = a / b
                Pair(res, "$a / $b = $res")
            }
            "power" -> {
                requireNotNull(b) { "幂运算需要两个操作数" }
                val res = a.pow(b)
                Pair(res, "$a ^ $b = $res")
            }
            "sqrt" -> {
                require(a >= 0) { "平方根操作需要非负数" }
                val res = sqrt(a)
                Pair(res, "√$a = $res")
            }
            "sin" -> {
                val res = sin(a)
                Pair(res, "sin($a) = $res")
            }
            "cos" -> {
                val res = cos(a)
                Pair(res, "cos($a) = $res")
            }
            "tan" -> {
                val res = tan(a)
                Pair(res, "tan($a) = $res")
            }
            "log10" -> {
                require(a > 0) { "对数操作需要正数" }
                val res = log10(a)
                Pair(res, "log10($a) = $res")
            }
            "ln" -> {
                require(a > 0) { "自然对数操作需要正数" }
                val res = ln(a)
                Pair(res, "ln($a) = $res")
            }
            else -> throw IllegalArgumentException("不支持的操作: $operation")
        }

        buildJsonObject {
            put("result", result)
            put("expression", expression)
        }
    }

    companion object {
        /**
         * 创建计算器工具。
         */
        fun create(): Tool {
            return CalculatorTool()
        }
    }
}
