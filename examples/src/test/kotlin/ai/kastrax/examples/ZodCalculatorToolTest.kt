package ai.kastrax.examples

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * ZodCalculatorTool 测试类
 */
class ZodCalculatorToolTest {
    
    /**
     * 测试基本计算器工具
     */
    @Test
    fun testBasicCalculatorTool() = runBlocking {
        // 创建计算器工具
        val calculatorTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
            id = "calculator"
            name = "计算器"
            description = "执行基本的数学运算"
            
            // 定义输入模式
            inputSchema = objectInput("计算器输入") {
                stringField("operation", "要执行的运算") {
                    enum("add", "subtract", "multiply", "divide")
                }
                numberField("a", "第一个操作数")
                numberField("b", "第二个操作数")
            }
            
            // 定义输出模式
            outputSchema = objectOutput("计算器输出") {
                numberField("result", "运算结果")
                stringField("expression", "运算表达式")
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
                    else -> throw IllegalArgumentException("不支持的运算: $operation")
                }
                
                val expressionSymbol = when (operation) {
                    "add" -> "+"
                    "subtract" -> "-"
                    "multiply" -> "*"
                    "divide" -> "/"
                    else -> "?"
                }
                
                val expression = "$a $expressionSymbol $b = $result"
                
                mapOf(
                    "result" to result,
                    "expression" to expression
                )
            }
        }
        
        // 测试加法
        val addInput = mapOf(
            "operation" to "add",
            "a" to 5,
            "b" to 3
        )
        
        val addResult = calculatorTool.execute(addInput)
        assertEquals(8.0, addResult["result"])
        assertEquals("5.0 + 3.0 = 8.0", addResult["expression"])
        
        // 测试减法
        val subtractInput = mapOf(
            "operation" to "subtract",
            "a" to 10,
            "b" to 4
        )
        
        val subtractResult = calculatorTool.execute(subtractInput)
        assertEquals(6.0, subtractResult["result"])
        assertEquals("10.0 - 4.0 = 6.0", subtractResult["expression"])
        
        // 测试乘法
        val multiplyInput = mapOf(
            "operation" to "multiply",
            "a" to 7,
            "b" to 6
        )
        
        val multiplyResult = calculatorTool.execute(multiplyInput)
        assertEquals(42.0, multiplyResult["result"])
        assertEquals("7.0 * 6.0 = 42.0", multiplyResult["expression"])
        
        // 测试除法
        val divideInput = mapOf(
            "operation" to "divide",
            "a" to 20,
            "b" to 5
        )
        
        val divideResult = calculatorTool.execute(divideInput)
        assertEquals(4.0, divideResult["result"])
        assertEquals("20.0 / 5.0 = 4.0", divideResult["expression"])
    }
    
    /**
     * 测试使用数据类的计算器工具
     */
    @Test
    fun testDataClassCalculatorTool(): Unit = runBlocking {
        // 定义输入模式（带转换）
        val calculatorInputSchema = objectInput("计算器输入") {
            stringField("operation", "要执行的运算") {
                enum("add", "subtract", "multiply", "divide")
            }
            numberField("a", "第一个操作数")
            numberField("b", "第二个操作数")
        }.transform { input ->
            CalculatorInput(
                operation = input["operation"] as String,
                a = (input["a"] as Number).toDouble(),
                b = (input["b"] as Number).toDouble()
            )
        }
        
        // 定义输出模式（带转换）
        val calculatorOutputSchema = objectOutput("计算器输出") {
            numberField("result", "运算结果")
            stringField("expression", "运算表达式")
        }.transform { output ->
            CalculatorOutput(
                result = (output["result"] as Number).toDouble(),
                expression = output["expression"] as String
            )
        }
        
        // 创建使用数据类的计算器工具
        val calculatorTool = zodTool<CalculatorInput, CalculatorOutput> {
            id = "calculator_data_class"
            name = "计算器（数据类）"
            description = "使用数据类执行基本的数学运算"
            
            // 使用 unsafeCast 来处理类型转换
            @Suppress("UNCHECKED_CAST")
            inputSchema = calculatorInputSchema as Schema<CalculatorInput, CalculatorInput>
            
            @Suppress("UNCHECKED_CAST")
            outputSchema = calculatorOutputSchema as Schema<CalculatorOutput, CalculatorOutput>
            
            execute = { input ->
                val result = when (input.operation) {
                    "add" -> input.a + input.b
                    "subtract" -> input.a - input.b
                    "multiply" -> input.a * input.b
                    "divide" -> input.a / input.b
                    else -> throw IllegalArgumentException("不支持的运算: ${input.operation}")
                }
                
                val expressionSymbol = when (input.operation) {
                    "add" -> "+"
                    "subtract" -> "-"
                    "multiply" -> "*"
                    "divide" -> "/"
                    else -> "?"
                }
                
                val expression = "${input.a} $expressionSymbol ${input.b} = $result"
                
                CalculatorOutput(
                    result = result,
                    expression = expression
                )
            }
        }
        
        // 测试加法
        val addInput = CalculatorInput(
            operation = "add",
            a = 15.0,
            b = 7.0
        )
        
        val addResult = calculatorTool.execute(addInput)
        assertEquals(22.0, addResult.result)
        assertEquals("15.0 + 7.0 = 22.0", addResult.expression)
        
        // 测试除法
        val divideInput = CalculatorInput(
            operation = "divide",
            a = 100.0,
            b = 4.0
        )
        
        val divideResult = calculatorTool.execute(divideInput)
        assertEquals(25.0, divideResult.result)
        assertEquals("100.0 / 4.0 = 25.0", divideResult.expression)
        
        // 测试除以零
        val divideByZeroInput = CalculatorInput(
            operation = "divide",
            a = 10.0,
            b = 0.0
        )
        
        assertThrows<ArithmeticException> {
            runBlocking {
                calculatorTool.execute(divideByZeroInput)
            }
        }
    }
    
    /**
     * 测试工具转换
     */
    @Test
    fun testToolConversion() = runBlocking {
        // 创建计算器工具
        val calculatorTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
            id = "calculator"
            name = "计算器"
            description = "执行基本的数学运算"
            
            // 定义输入模式
            inputSchema = objectInput("计算器输入") {
                stringField("operation", "要执行的运算") {
                    enum("add", "subtract", "multiply", "divide")
                }
                numberField("a", "第一个操作数")
                numberField("b", "第二个操作数")
            }
            
            // 定义输出模式
            outputSchema = objectOutput("计算器输出") {
                numberField("result", "运算结果")
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
                    else -> throw IllegalArgumentException("不支持的运算: $operation")
                }
                
                mapOf("result" to result)
            }
        }
        
        // 将 ZodTool 转换为传统 Tool
        val legacyTool = calculatorTool.toTool()
        
        // 验证转换后的工具属性
        assertEquals("calculator", legacyTool.id)
        assertEquals("计算器", legacyTool.name)
        assertEquals("执行基本的数学运算", legacyTool.description)
        assertNotNull(legacyTool.inputSchema)
        assertNotNull(legacyTool.outputSchema)
    }
}
