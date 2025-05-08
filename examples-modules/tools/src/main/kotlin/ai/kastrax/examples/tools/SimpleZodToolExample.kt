package ai.kastrax.examples

import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.coroutines.runBlocking

/**
 * 简单的 ZodTool 示例。
 */
fun main() {
    // 创建一个简单的字符串反转工具
    val reverseStringTool = zodTool<String, String> {
        id = "reverse_string"
        name = "Reverse String"
        description = "Reverses the input string"

        // 使用 stringInput 和 stringOutput 辅助函数创建模式
        inputSchema = stringInput("The string to reverse")
        outputSchema = stringOutput("The reversed string")

        // 实现执行逻辑
        execute = { input ->
            input.reversed()
        }
    }

    // 使用工具
    val input = "Hello, World!"

    // 验证输入
    val validationResult = reverseStringTool.inputSchema.safeParse(input)
    if (validationResult is SchemaResult.Success) {
        println("Input is valid")
    } else {
        println("Input is invalid: ${(validationResult as SchemaResult.Failure).error}")
        return
    }

    // 执行工具
    val output = runBlocking {
        reverseStringTool.execute(input)
    }

    println("Original: $input")
    println("Reversed: $output")

    // 将 ZodTool 转换为传统的 Tool
    val legacyTool = reverseStringTool.toTool()

    println("\nUsing legacy tool:")

    // 使用传统工具
    val legacyOutput = runBlocking {
        legacyTool.execute(kotlinx.serialization.json.JsonPrimitive(input))
    }

    println("Legacy output: ${legacyOutput.toString()}")
}
