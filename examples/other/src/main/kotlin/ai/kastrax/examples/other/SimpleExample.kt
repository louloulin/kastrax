package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.core.kastrax
import ai.kastrax.core.tools.jsonObject
import ai.kastrax.core.tools.tool
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*

fun main() = runBlocking {
    // Create a simple calculator tool
    val calculatorTool = tool {
        id = "calculator"
        name = "Calculator"
        description = "Perform mathematical calculations"
        inputSchema = jsonObject {
            "type" to "object"
            "properties" to jsonObject {
                "expression" to jsonObject {
                    "type" to "string"
                    "description" to "The mathematical expression to evaluate"
                }
            }
            "required" to JsonArray(listOf(JsonPrimitive("expression")))
        }
        outputSchema = jsonObject {
            "type" to "object"
            "properties" to jsonObject {
                "result" to jsonObject {
                    "type" to "number"
                    "description" to "The result of the calculation"
                }
            }
        }
        execute = { input ->
            val expression = input.jsonObject["expression"]?.jsonPrimitive?.content ?: "0"
            val result = evaluateExpression(expression)
            jsonObject {
                "result" to result
            }
        }
    }

    // Create an agent with the calculator tool
    val calculatorAgent = agent {
        name = "MathHelper"
        instructions = """
            You are a helpful math assistant that can solve mathematical problems.
            When asked to perform calculations, use the calculator tool.
            Explain your reasoning step by step.
        """.trimIndent()
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            // 显式设置 API 密钥
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }
        tools {
            tool(calculatorTool)
        }
    }

    // Create the KastraX instance
    val kastrax = kastrax {
        agent("math", calculatorAgent)
    }

    // Get the agent and use it
    val agent = kastrax.getAgent("math")

    println("KastraX Math Helper Example")
    println("---------------------------")
    println("Enter 'exit' to quit")

    while (true) {
        print("\nYour question: ")
        val input = readLine() ?: ""

        if (input.equals("exit", ignoreCase = true)) {
            break
        }

        if (input.isNotBlank()) {
            println("\nThinking...")

            try {
                // Generate a response
                val response = agent.generate(input)

                println("\nResponse:")
                println(response.text)

                // Show tool usage if any
                if (response.toolCalls.isNotEmpty()) {
                    println("\nTools used:")
                    response.toolCalls.forEachIndexed { index, toolCall ->
                        println("  ${index + 1}. ${toolCall.name}: ${toolCall.arguments}")
                        val result = response.toolResults[toolCall.id]
                        if (result != null) {
                            println("     Result: ${result.result}")
                        }
                    }
                }
            } catch (e: Exception) {
                println("\nError: ${e.message}")
            }
        }
    }

    println("\nThank you for using KastraX Math Helper!")
}

// Helper function to evaluate simple expressions
private fun evaluateExpression(expression: String): Int {
    try {
        // This is a very simplified calculator
        val sanitized = expression.replace(" ", "")

        // Handle addition
        if ("+" in sanitized) {
            val parts = sanitized.split("+")
            return parts.sumOf { it.toInt() }
        }

        // Handle subtraction
        if ("-" in sanitized) {
            val parts = sanitized.split("-")
            return parts.first().toInt() - parts.drop(1).sumOf { it.toInt() }
        }

        // Handle multiplication
        if ("*" in sanitized) {
            val parts = sanitized.split("*")
            return parts.fold(1) { acc, part -> acc * part.toInt() }
        }

        // Handle division
        if ("/" in sanitized) {
            val parts = sanitized.split("/")
            return parts.drop(1).fold(parts.first().toInt()) { acc, part ->
                if (part.toInt() != 0) acc / part.toInt() else acc
            }
        }

        // If no operators, just return the number
        return sanitized.toInt()
    } catch (e: Exception) {
        return 0
    }
}
