package ai.kastrax.examples.tools

import ai.kastrax.core.tools.zodTool
import ai.kastrax.zod.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

/**
 * 简单的 Zod 工具示例
 */
fun main() = runBlocking {
    println("Simple Zod Tool 示例")
    println("-------------------")
    
    // 创建一个简单的字符串反转工具
    val reverseStringTool = zodTool<String, String> {
        id = "reverse_string"
        name = "字符串反转"
        description = "反转输入的字符串"
        
        // 定义输入模式
        inputSchema = z.string().describe("要反转的字符串")
        
        // 定义输出模式
        outputSchema = z.string().describe("反转后的字符串")
        
        // 实现执行逻辑
        execute = { input ->
            input.reversed()
        }
    }
    
    // 使用工具
    val input = "Hello, World!"
    val output = reverseStringTool.execute(input)
    
    println("输入: $input")
    println("输出: $output")
    
    println("\nSimple Zod Tool 示例完成")
}
