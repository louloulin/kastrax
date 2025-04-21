package ai.kastrax.core.agent

import ai.kastrax.core.tools.tool
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for Agent using real DeepSeek API.
 * These tests require a valid DeepSeek API key in the DEEPSEEK_API_KEY environment variable.
 */
class DeepSeekAgentTest {

    @Test
    fun `test agent with DeepSeek`() = runBlocking {
        // Create a calculator tool
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
            outputSchema = buildJsonObject {
                put("type", "object")
                putJsonObject("properties") {
                    putJsonObject("result") {
                        put("type", "number")
                        put("description", "The result of the calculation")
                    }
                }
            }
            execute = { input ->
                val expression = input.jsonObject["expression"]?.jsonPrimitive?.content ?: "0"
                val result = evaluateExpression(expression)
                buildJsonObject {
                    put("result", result)
                }
            }
        }

        // Create agent with DeepSeek
        val testAgent = agent {
            name = "TestAgent"
            instructions = "You are a helpful assistant that can answer questions and perform calculations."
            model = deepSeek {
                model("deepseek-chat")
                // API key from environment variable DEEPSEEK_API_KEY
            }
            tools {
                tool(calculatorTool)
            }
        }

        // Test generate method
        val response = testAgent.generate("What is 2 + 2?")
        
        println("Response: ${response.text}")
        println("Tool calls: ${response.toolCalls}")
        println("Tool results: ${response.toolResults}")
        
        assertNotNull(response.text)
        assertTrue(response.text.isNotEmpty())
        
        // Test stream method
        val streamResponse = testAgent.stream("Calculate the square root of 16")
        val streamContent = StringBuilder()
        
        streamResponse.textStream?.collect { chunk ->
            streamContent.append(chunk)
            print(chunk)
        }
        
        println("\nStream response: $streamContent")
        assertTrue(streamContent.isNotEmpty())
    }
    
    // Helper function to evaluate simple expressions
    private fun evaluateExpression(expression: String): Double {
        // This is a very simplified calculator for testing
        return when {
            "+" in expression -> {
                val parts = expression.split("+")
                parts[0].trim().toDouble() + parts[1].trim().toDouble()
            }
            "-" in expression -> {
                val parts = expression.split("-")
                parts[0].trim().toDouble() - parts[1].trim().toDouble()
            }
            "*" in expression -> {
                val parts = expression.split("*")
                parts[0].trim().toDouble() * parts[1].trim().toDouble()
            }
            "/" in expression -> {
                val parts = expression.split("/")
                parts[0].trim().toDouble() / parts[1].trim().toDouble()
            }
            "sqrt" in expression.lowercase() -> {
                val number = expression.lowercase().replace("sqrt", "").trim()
                    .replace("(", "").replace(")", "")
                Math.sqrt(number.toDouble())
            }
            else -> expression.trim().toDouble()
        }
    }
}
