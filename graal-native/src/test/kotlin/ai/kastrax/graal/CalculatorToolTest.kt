package ai.kastrax.graal

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.JsonPrimitive
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class CalculatorToolTest {

    @Test
    fun testAddition() = runBlocking {
        val input = JsonPrimitive("2+2")
        val result = calculatorTool.execute(input)
        assertEquals("4.0", (result as JsonPrimitive).content)
    }

    @Test
    fun testSubtraction() = runBlocking {
        val input = JsonPrimitive("5-3")
        val result = calculatorTool.execute(input)
        assertEquals("2.0", (result as JsonPrimitive).content)
    }

    @Test
    fun testMultiplication() = runBlocking {
        val input = JsonPrimitive("3*4")
        val result = calculatorTool.execute(input)
        assertEquals("12.0", (result as JsonPrimitive).content)
    }

    @Test
    fun testDivision() = runBlocking {
        val input = JsonPrimitive("10/2")
        val result = calculatorTool.execute(input)
        assertEquals("5.0", (result as JsonPrimitive).content)
    }

    @Test
    fun testDivisionByZero() = runBlocking {
        val input = JsonPrimitive("5/0")
        val result = calculatorTool.execute(input)
        assertEquals("错误：除数不能为零", (result as JsonPrimitive).content)
    }

    @Test
    fun testInvalidExpression() = runBlocking {
        val input = JsonPrimitive("invalid")
        val result = calculatorTool.execute(input)
        assertEquals("无法解析表达式：invalid", (result as JsonPrimitive).content)
    }
}
