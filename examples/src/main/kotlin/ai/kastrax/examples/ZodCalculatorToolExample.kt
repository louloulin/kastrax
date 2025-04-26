package ai.kastrax.examples

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

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
    val result: Double,
    val expression: String
)

/**
 * ZodTool 计算器示例
 * 
 * 这个示例展示了如何使用 ZodTool 创建一个简单的计算器工具
 */
fun main() = runBlocking {
    println("ZodTool 计算器示例")
    println("------------------")
    
    // 创建计算器工具 - 使用 Map 作为输入和输出
    val calculatorTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "calculator"
        name = "计算器"
        description = "执行基本的数学运算（加、减、乘、除）"
        
        // 定义输入模式
        inputSchema = objectInput("计算器输入") {
            stringField("operation", "要执行的运算") {
                enum("add", "subtract", "multiply", "divide")
                description = "支持的运算：add（加法）、subtract（减法）、multiply（乘法）、divide（除法）"
            }
            numberField("a", "第一个操作数") {
                description = "运算的第一个数字"
            }
            numberField("b", "第二个操作数") {
                description = "运算的第二个数字"
            }
        }
        
        // 定义输出模式
        outputSchema = objectOutput("计算器输出") {
            numberField("result", "运算结果") {
                description = "数学运算的结果"
            }
            stringField("expression", "运算表达式") {
                description = "格式化的运算表达式"
            }
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
    
    // 使用计算器工具 - 加法
    val addInput = mapOf(
        "operation" to "add",
        "a" to 5,
        "b" to 3
    )
    
    println("\n执行加法运算:")
    val addResult = calculatorTool.execute(addInput)
    println("输入: $addInput")
    println("输出: $addResult")
    
    // 使用计算器工具 - 乘法
    val multiplyInput = mapOf(
        "operation" to "multiply",
        "a" to 4,
        "b" to 7
    )
    
    println("\n执行乘法运算:")
    val multiplyResult = calculatorTool.execute(multiplyInput)
    println("输入: $multiplyInput")
    println("输出: $multiplyResult")
    
    // 创建使用数据类的计算器工具
    println("\n创建使用数据类的计算器工具...")
    
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
    val calculatorToolWithDataClass = zodTool<CalculatorInput, CalculatorOutput> {
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
    
    // 使用数据类的计算器工具
    val inputDataClass = CalculatorInput(
        operation = "divide",
        a = 10.0,
        b = 2.0
    )
    
    println("\n使用数据类执行除法运算:")
    val outputDataClass = calculatorToolWithDataClass.execute(inputDataClass)
    println("输入: $inputDataClass")
    println("输出: $outputDataClass")
    
    // 将 ZodTool 转换为传统 Tool
    val legacyTool = calculatorToolWithDataClass.toTool()
    println("\n将 ZodTool 转换为传统 Tool:")
    println("Tool ID: ${legacyTool.id}")
    println("Tool Name: ${legacyTool.name}")
    println("Tool Description: ${legacyTool.description}")
    
    // 使用传统 Tool
    val legacyInput = buildJsonObject {
        put("operation", "subtract")
        put("a", 20)
        put("b", 5)
    }
    
    println("\n使用传统 Tool 执行减法运算:")
    val legacyOutput = legacyTool.execute(legacyInput)
    println("输入: $legacyInput")
    println("输出: $legacyOutput")
    
    println("\nZodTool 计算器示例完成")
}

/**
 * 简单的表达式求值函数
 */
private fun evaluateExpression(expression: String): Double {
    // 这里使用简化的实现，实际应用中可能需要更复杂的表达式解析
    val parts = expression.split(Regex("\\s+"))
    if (parts.size != 3) {
        throw IllegalArgumentException("无效的表达式格式: $expression")
    }
    
    val a = parts[0].toDoubleOrNull() ?: throw IllegalArgumentException("无效的第一个操作数: ${parts[0]}")
    val operator = parts[1]
    val b = parts[2].toDoubleOrNull() ?: throw IllegalArgumentException("无效的第二个操作数: ${parts[2]}")
    
    return when (operator) {
        "+" -> a + b
        "-" -> a - b
        "*" -> a * b
        "/" -> a / b
        else -> throw IllegalArgumentException("不支持的运算符: $operator")
    }
}
