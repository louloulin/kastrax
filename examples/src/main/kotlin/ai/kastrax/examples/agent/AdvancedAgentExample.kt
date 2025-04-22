package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.ToolChoice
import ai.kastrax.core.agent.agent
import ai.kastrax.core.tools.tool
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * 高级Agent示例，展示新增的Agent功能
 */
fun main() = runBlocking {
    // 创建计算器工具
    val calculatorTool = tool {
        id = "calculator"
        name = "Calculator"
        description = "Perform mathematical calculations"
        inputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("expression") {
                    put("type", "string")
                    put("description", "The mathematical expression to evaluate")
                }
            }
            putJsonArray("required") {
                add("expression")
            }
        }
        execute = { input ->
            val expression = input.toString()
            val result = evaluateExpression(expression)
            buildJsonObject {
                put("result", result)
            }
        }
    }

    // 创建天气工具
    val weatherTool = tool {
        id = "weather"
        name = "Weather"
        description = "Get weather information for a location"
        inputSchema = buildJsonObject {
            put("type", "object")
            putJsonObject("properties") {
                putJsonObject("location") {
                    put("type", "string")
                    put("description", "The location to get weather for")
                }
            }
            putJsonArray("required") {
                add("location")
            }
        }
        execute = { input ->
            buildJsonObject {
                put("temperature", 22)
                put("condition", "sunny")
                put("humidity", 65)
            }
        }
    }

    // 创建高级Agent
    val agent = agent {
        name = "AdvancedAgent"
        instructions = "You are an advanced agent that can help with calculations and weather information."
        model = openAi("gpt-4o")
        
        // 添加基础工具
        tools {
            tool(calculatorTool)
        }
        
        // 添加工具集
        toolset("weather") {
            tool(weatherTool)
        }
        
        // 配置默认生成选项
        defaultGenerateOptions {
            temperature(0.7)
            maxTokens(500)
            topP(0.9)
        }
        
        // 配置默认流式选项
        defaultStreamOptions {
            temperature(0.6)
            maxTokens(500)
        }
    }

    println("=== 基本生成示例 ===")
    val response1 = agent.generate("Tell me a short joke.")
    println("响应: ${response1.text}")
    println()

    println("=== 使用特定工具示例 ===")
    val response2 = agent.generate(
        "What is 25 + 17?",
        AgentGenerateOptions(
            toolChoice = ToolChoice.specific("calculator")
        )
    )
    println("响应: ${response2.text}")
    if (response2.toolCalls.isNotEmpty()) {
        println("工具调用: ${response2.toolCalls[0].name}")
        println("工具结果: ${response2.toolResults[response2.toolCalls[0].id]?.result}")
    }
    println()

    println("=== 使用工具集示例 ===")
    val response3 = agent.generate(
        "What's the weather like in New York?",
        AgentGenerateOptions(
            toolsets = mapOf("weather" to mapOf(weatherTool.id to weatherTool))
        )
    )
    println("响应: ${response3.text}")
    if (response3.toolCalls.isNotEmpty()) {
        println("工具调用: ${response3.toolCalls[0].name}")
        println("工具结果: ${response3.toolResults[response3.toolCalls[0].id]?.result}")
    }
    println()

    println("=== 流式响应示例 ===")
    val response4 = agent.stream(
        "Tell me about the benefits of exercise.",
        AgentStreamOptions(
            temperature = 0.8
        )
    )
    
    println("流式响应:")
    val fullText = StringBuilder()
    response4.textStream?.collect { chunk ->
        print(chunk)
        fullText.append(chunk)
    }
    println("\n\n完整响应长度: ${fullText.length} 字符")
}

/**
 * 简单的表达式计算函数
 */
private fun evaluateExpression(expression: String): Int {
    // 这是一个非常简化的计算器，仅用于演示
    val cleanExpr = expression.replace("\"", "").replace("expression=", "")
    val parts = cleanExpr.split("+", "-", "*", "/")
    if (parts.size != 2) return 0

    val a = parts[0].trim().toIntOrNull() ?: 0
    val b = parts[1].trim().toIntOrNull() ?: 0

    return when {
        "+" in cleanExpr -> a + b
        "-" in cleanExpr -> a - b
        "*" in cleanExpr -> a * b
        "/" in cleanExpr -> if (b != 0) a / b else 0
        else -> 0
    }
}
