package ai.kastrax.examples.tools

import ai.kastrax.core.tools.tool
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

/**
 * 简单的工具示例
 */
fun main() = runBlocking {
    println("Hello Tools 示例")
    println("-------------------")
    
    // 创建一个简单的字符串反转工具
    val reverseStringTool = tool {
        id = "reverse_string"
        name = "字符串反转"
        description = "反转输入的字符串"
        
        // 定义输入模式
        inputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("text") {
                    put("type", "string")
                    put("description", "要反转的字符串")
                }
            }
            putJsonArray("required") {
                add("text")
            }
        }
        
        // 定义输出模式
        outputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("result") {
                    put("type", "string")
                    put("description", "反转后的字符串")
                }
            }
        }
        
        // 实现执行逻辑
        execute = { input ->
            val text = input.jsonObject["text"]?.jsonPrimitive?.content ?: ""
            val reversed = text.reversed()
            
            buildJsonObject {
                put("result", reversed)
            }
        }
    }
    
    // 使用工具
    val input = buildJsonObject {
        put("text", "Hello, World!")
    }
    
    val output = reverseStringTool.execute(input)
    
    println("输入: ${input.jsonObject["text"]?.jsonPrimitive?.content}")
    println("输出: ${output.jsonObject["result"]?.jsonPrimitive?.content}")
    
    println("\nHello Tools 示例完成")
}
