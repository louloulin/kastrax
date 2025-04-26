package ai.kastrax.core.tools.math

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlinx.serialization.json.JsonElement

/**
 * ZodCalculatorTool 测试类。
 */
class ZodCalculatorToolTest {

    private val calculatorTool = ZodCalculatorTool.createZodTool()
    private val legacyTool = calculatorTool.toTool()

    /**
     * 测试直接使用 ZodTool 接口。
     */
    @Test
    fun testZodToolInterface() = runBlocking {
        // 测试加法
        val addInput = ZodCalculatorTool.CalculatorInput(
            operation = "add",
            a = 5.0,
            b = 3.0
        )

        val addResult = calculatorTool.execute(addInput)
        assertEquals(8.0, addResult.result)
        assertEquals("5.0 + 3.0 = 8.0", addResult.expression)

        // 测试减法
        val subtractInput = ZodCalculatorTool.CalculatorInput(
            operation = "subtract",
            a = 10.0,
            b = 4.0
        )

        val subtractResult = calculatorTool.execute(subtractInput)
        assertEquals(6.0, subtractResult.result)
        assertEquals("10.0 - 4.0 = 6.0", subtractResult.expression)

        // 测试乘法
        val multiplyInput = ZodCalculatorTool.CalculatorInput(
            operation = "multiply",
            a = 7.0,
            b = 6.0
        )

        val multiplyResult = calculatorTool.execute(multiplyInput)
        assertEquals(42.0, multiplyResult.result)
        assertEquals("7.0 * 6.0 = 42.0", multiplyResult.expression)

        // 测试除法
        val divideInput = ZodCalculatorTool.CalculatorInput(
            operation = "divide",
            a = 20.0,
            b = 5.0
        )

        val divideResult = calculatorTool.execute(divideInput)
        assertEquals(4.0, divideResult.result)
        assertEquals("20.0 / 5.0 = 4.0", divideResult.expression)

        // 测试幂运算
        val powerInput = ZodCalculatorTool.CalculatorInput(
            operation = "power",
            a = 2.0,
            b = 3.0
        )

        val powerResult = calculatorTool.execute(powerInput)
        assertEquals(8.0, powerResult.result)
        assertEquals("2.0 ^ 3.0 = 8.0", powerResult.expression)

        // 测试平方根
        val sqrtInput = ZodCalculatorTool.CalculatorInput(
            operation = "sqrt",
            a = 9.0
        )

        val sqrtResult = calculatorTool.execute(sqrtInput)
        assertEquals(3.0, sqrtResult.result)
        assertEquals("√9.0 = 3.0", sqrtResult.expression)

        // 测试正弦函数
        val sinInput = ZodCalculatorTool.CalculatorInput(
            operation = "sin",
            a = 0.0
        )

        val sinResult = calculatorTool.execute(sinInput)
        assertEquals(0.0, sinResult.result)
        assertEquals("sin(0.0) = 0.0", sinResult.expression)

        // 测试余弦函数
        val cosInput = ZodCalculatorTool.CalculatorInput(
            operation = "cos",
            a = 0.0
        )

        val cosResult = calculatorTool.execute(cosInput)
        assertEquals(1.0, cosResult.result)
        assertEquals("cos(0.0) = 1.0", cosResult.expression)

        // 测试正切函数
        val tanInput = ZodCalculatorTool.CalculatorInput(
            operation = "tan",
            a = 0.0
        )

        val tanResult = calculatorTool.execute(tanInput)
        assertEquals(0.0, tanResult.result)
        assertEquals("tan(0.0) = 0.0", tanResult.expression)

        // 测试常用对数
        val log10Input = ZodCalculatorTool.CalculatorInput(
            operation = "log10",
            a = 100.0
        )

        val log10Result = calculatorTool.execute(log10Input)
        assertEquals(2.0, log10Result.result)
        assertEquals("log10(100.0) = 2.0", log10Result.expression)

        // 测试自然对数
        val lnInput = ZodCalculatorTool.CalculatorInput(
            operation = "ln",
            a = 1.0
        )

        val lnResult = calculatorTool.execute(lnInput)
        assertEquals(0.0, lnResult.result)
        assertEquals("ln(1.0) = 0.0", lnResult.expression)
    }

    /**
     * 测试异常情况。
     */
    @Test
    fun testExceptions() = runBlocking {
        // 测试除以零
        val divideByZeroInput = ZodCalculatorTool.CalculatorInput(
            operation = "divide",
            a = 10.0,
            b = 0.0
        )

        assertThrows<IllegalArgumentException> {
            runBlocking {
                calculatorTool.execute(divideByZeroInput)
            }
        }

        // 测试负数平方根
        val negativeSqrtInput = ZodCalculatorTool.CalculatorInput(
            operation = "sqrt",
            a = -1.0
        )

        assertThrows<IllegalArgumentException> {
            runBlocking {
                calculatorTool.execute(negativeSqrtInput)
            }
        }

        // 测试负数对数
        val negativeLogInput = ZodCalculatorTool.CalculatorInput(
            operation = "log10",
            a = -10.0
        )

        assertThrows<IllegalArgumentException> {
            runBlocking {
                calculatorTool.execute(negativeLogInput)
            }
        }

        // 测试缺少第二个操作数
        val missingOperandInput = ZodCalculatorTool.CalculatorInput(
            operation = "add",
            a = 5.0
        )

        assertThrows<IllegalArgumentException> {
            runBlocking {
                calculatorTool.execute(missingOperandInput)
            }
        }
    }

    /**
     * 测试转换为传统 Tool 接口。
     */
    @Test
    fun testLegacyToolInterface() = runBlocking {
        // 测试加法
        val addInput = buildJsonObject {
            put("operation", "add")
            put("a", 5.0)
            put("b", 3.0)
        }

        val addResult = legacyTool.execute(addInput) as JsonObject
        assertEquals(8.0, (addResult["result"] as JsonPrimitive).content.toDouble())
        assertTrue((addResult["expression"] as JsonPrimitive).content.contains("5.0 + 3.0 = 8.0"))

        // 测试平方根
        val sqrtInput = buildJsonObject {
            put("operation", "sqrt")
            put("a", 16.0)
        }

        val sqrtResult = legacyTool.execute(sqrtInput) as JsonObject
        assertEquals(4.0, (sqrtResult["result"] as JsonPrimitive).content.toDouble())
        assertTrue((sqrtResult["expression"] as JsonPrimitive).content.contains("√16.0 = 4.0"))
    }
}
