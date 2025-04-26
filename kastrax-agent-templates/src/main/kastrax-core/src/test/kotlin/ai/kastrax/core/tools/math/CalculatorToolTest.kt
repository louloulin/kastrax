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
import kotlin.math.abs

/**
 * 计算器工具的测试类。
 */
class CalculatorToolTest {

    private val calculatorTool = CalculatorTool.create()

    /**
     * 测试加法操作。
     */
    @Test
    fun testAdd() = runBlocking {
        val input = buildJsonObject {
            put("operation", "add")
            put("a", 5.0)
            put("b", 3.0)
        }

        val result = calculatorTool.execute(input) as JsonObject
        assertEquals(8.0, result["result"]?.let { (it as JsonPrimitive).double })
        assertTrue(result["expression"]?.let { (it as JsonPrimitive).content.contains("5.0 + 3.0 = 8.0") } == true)
    }

    /**
     * 测试减法操作。
     */
    @Test
    fun testSubtract() = runBlocking {
        val input = buildJsonObject {
            put("operation", "subtract")
            put("a", 5.0)
            put("b", 3.0)
        }

        val result = calculatorTool.execute(input) as JsonObject
        assertEquals(2.0, result["result"]?.let { (it as JsonPrimitive).double })
        assertTrue(result["expression"]?.let { (it as JsonPrimitive).content.contains("5.0 - 3.0 = 2.0") } == true)
    }

    /**
     * 测试乘法操作。
     */
    @Test
    fun testMultiply() = runBlocking {
        val input = buildJsonObject {
            put("operation", "multiply")
            put("a", 5.0)
            put("b", 3.0)
        }

        val result = calculatorTool.execute(input) as JsonObject
        assertEquals(15.0, result["result"]?.let { (it as JsonPrimitive).double })
        assertTrue(result["expression"]?.let { (it as JsonPrimitive).content.contains("5.0 * 3.0 = 15.0") } == true)
    }

    /**
     * 测试除法操作。
     */
    @Test
    fun testDivide() = runBlocking {
        val input = buildJsonObject {
            put("operation", "divide")
            put("a", 6.0)
            put("b", 3.0)
        }

        val result = calculatorTool.execute(input) as JsonObject
        assertEquals(2.0, result["result"]?.let { (it as JsonPrimitive).double })
        assertTrue(result["expression"]?.let { (it as JsonPrimitive).content.contains("6.0 / 3.0 = 2.0") } == true)
    }

    /**
     * 测试除以零的情况。
     */
    @Test
    fun testDivideByZero() = runBlocking {
        val input = buildJsonObject {
            put("operation", "divide")
            put("a", 6.0)
            put("b", 0.0)
        }

        assertThrows<IllegalArgumentException> {
            runBlocking {
                calculatorTool.execute(input)
            }
        }
    }

    /**
     * 测试幂运算。
     */
    @Test
    fun testPower() = runBlocking {
        val input = buildJsonObject {
            put("operation", "power")
            put("a", 2.0)
            put("b", 3.0)
        }

        val result = calculatorTool.execute(input) as JsonObject
        assertEquals(8.0, result["result"]?.let { (it as JsonPrimitive).double })
        assertTrue(result["expression"]?.let { (it as JsonPrimitive).content.contains("2.0 ^ 3.0 = 8.0") } == true)
    }

    /**
     * 测试平方根操作。
     */
    @Test
    fun testSqrt() = runBlocking {
        val input = buildJsonObject {
            put("operation", "sqrt")
            put("a", 9.0)
        }

        val result = calculatorTool.execute(input) as JsonObject
        assertEquals(3.0, result["result"]?.let { (it as JsonPrimitive).double })
        assertTrue(result["expression"]?.let { (it as JsonPrimitive).content.contains("√9.0 = 3.0") } == true)
    }

    /**
     * 测试负数平方根的情况。
     */
    @Test
    fun testSqrtOfNegative() = runBlocking {
        val input = buildJsonObject {
            put("operation", "sqrt")
            put("a", -1.0)
        }

        assertThrows<IllegalArgumentException> {
            runBlocking {
                calculatorTool.execute(input)
            }
        }
    }

    /**
     * 测试正弦函数。
     */
    @Test
    fun testSin() = runBlocking {
        val input = buildJsonObject {
            put("operation", "sin")
            put("a", 0.0)
        }

        val result = calculatorTool.execute(input) as JsonObject
        assertEquals(0.0, result["result"]?.let { (it as JsonPrimitive).double })
        assertTrue(result["expression"]?.let { (it as JsonPrimitive).content.contains("sin(0.0) = 0.0") } == true)
    }

    /**
     * 测试余弦函数。
     */
    @Test
    fun testCos() = runBlocking {
        val input = buildJsonObject {
            put("operation", "cos")
            put("a", 0.0)
        }

        val result = calculatorTool.execute(input) as JsonObject
        assertEquals(1.0, result["result"]?.let { (it as JsonPrimitive).double })
        assertTrue(result["expression"]?.let { (it as JsonPrimitive).content.contains("cos(0.0) = 1.0") } == true)
    }

    /**
     * 测试正切函数。
     */
    @Test
    fun testTan() = runBlocking {
        val input = buildJsonObject {
            put("operation", "tan")
            put("a", 0.0)
        }

        val result = calculatorTool.execute(input) as JsonObject
        assertEquals(0.0, result["result"]?.let { (it as JsonPrimitive).double })
        assertTrue(result["expression"]?.let { (it as JsonPrimitive).content.contains("tan(0.0) = 0.0") } == true)
    }

    /**
     * 测试常用对数。
     */
    @Test
    fun testLog10() = runBlocking {
        val input = buildJsonObject {
            put("operation", "log10")
            put("a", 100.0)
        }

        val result = calculatorTool.execute(input) as JsonObject
        assertEquals(2.0, result["result"]?.let { (it as JsonPrimitive).double })
        assertTrue(result["expression"]?.let { (it as JsonPrimitive).content.contains("log10(100.0) = 2.0") } == true)
    }

    /**
     * 测试自然对数。
     */
    @Test
    fun testLn() = runBlocking {
        val input = buildJsonObject {
            put("operation", "ln")
            put("a", 1.0)
        }

        val result = calculatorTool.execute(input) as JsonObject
        assertEquals(0.0, result["result"]?.let { (it as JsonPrimitive).double })
        assertTrue(result["expression"]?.let { (it as JsonPrimitive).content.contains("ln(1.0) = 0.0") } == true)
    }

    /**
     * 测试不支持的操作。
     */
    @Test
    fun testUnsupportedOperation() = runBlocking {
        val input = buildJsonObject {
            put("operation", "unsupported")
            put("a", 1.0)
        }

        assertThrows<IllegalArgumentException> {
            runBlocking {
                calculatorTool.execute(input)
            }
        }
    }
}
