package ai.kastrax.examples

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking

/**
 * 计算器输入。
 */
data class CalculatorInput(
    val operation: String,
    val a: Double,
    val b: Double
)

/**
 * 计算器输出。
 */
data class CalculatorOutput(
    val result: Double
)

/**
 * 使用 kastrax-zod 的计算器工具示例。
 */
fun main() {
    // 定义计算器输入模式
    val calculatorInputSchema = objectInput("Calculator input") {
        stringField("operation", "Operation to perform (add, subtract, multiply, divide)") {
            enum("add", "subtract", "multiply", "divide")
        }
        numberField("a", "First operand")
        numberField("b", "Second operand")
    }
    
    // 定义计算器输出模式
    val calculatorOutputSchema = objectOutput("Calculator output") {
        numberField("result", "Result of the operation")
    }
    
    // 创建计算器工具
    val calculatorTool = zodTool<Map<String, Any?>, Map<String, Any?>> {
        id = "calculator"
        name = "Calculator"
        description = "Performs basic arithmetic operations"
        inputSchema = calculatorInputSchema
        outputSchema = calculatorOutputSchema
        
        execute = { input ->
            val operation = input["operation"] as String
            val a = (input["a"] as Number).toDouble()
            val b = (input["b"] as Number).toDouble()
            
            val result = when (operation) {
                "add" -> a + b
                "subtract" -> a - b
                "multiply" -> a * b
                "divide" -> a / b
                else -> throw IllegalArgumentException("Unknown operation: $operation")
            }
            
            mapOf("result" to result)
        }
    }
    
    // 使用计算器工具
    val input = mapOf(
        "operation" to "add",
        "a" to 5,
        "b" to 3
    )
    
    // 验证输入
    val validationResult = calculatorTool.inputSchema.safeParse(input)
    if (validationResult is SchemaResult.Success) {
        println("Input is valid")
    } else {
        println("Input is invalid: ${(validationResult as SchemaResult.Failure).error}")
        return
    }
    
    // 执行工具
    val output = runBlocking {
        calculatorTool.execute(input)
    }
    
    println("Result: ${output["result"]}")
    
    // 使用数据类的示例
    val calculatorInputSchemaWithTransform = calculatorInputSchema.transform { input ->
        CalculatorInput(
            operation = input["operation"] as String,
            a = (input["a"] as Number).toDouble(),
            b = (input["b"] as Number).toDouble()
        )
    }
    
    val calculatorOutputSchemaWithTransform = calculatorOutputSchema.transform { output ->
        CalculatorOutput(
            result = (output["result"] as Number).toDouble()
        )
    }
    
    // 创建使用数据类的计算器工具
    val calculatorToolWithDataClass = zodTool<CalculatorInput, CalculatorOutput> {
        id = "calculator_data_class"
        name = "Calculator (Data Class)"
        description = "Performs basic arithmetic operations using data classes"
        inputSchema = calculatorInputSchemaWithTransform
        outputSchema = calculatorOutputSchemaWithTransform
        
        execute = { input ->
            val result = when (input.operation) {
                "add" -> input.a + input.b
                "subtract" -> input.a - input.b
                "multiply" -> input.a * input.b
                "divide" -> input.a / input.b
                else -> throw IllegalArgumentException("Unknown operation: ${input.operation}")
            }
            
            CalculatorOutput(result)
        }
    }
    
    // 使用数据类的计算器工具
    val inputDataClass = CalculatorInput(
        operation = "multiply",
        a = 4.0,
        b = 7.0
    )
    
    // 验证输入
    val validationResultDataClass = calculatorToolWithDataClass.inputSchema.safeParse(inputDataClass)
    if (validationResultDataClass is SchemaResult.Success) {
        println("Input data class is valid")
    } else {
        println("Input data class is invalid: ${(validationResultDataClass as SchemaResult.Failure).error}")
        return
    }
    
    // 执行工具
    val outputDataClass = runBlocking {
        calculatorToolWithDataClass.execute(inputDataClass)
    }
    
    println("Result (data class): ${outputDataClass.result}")
}
