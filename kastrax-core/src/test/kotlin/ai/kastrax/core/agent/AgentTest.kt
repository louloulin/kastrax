package ai.kastrax.core.agent

import ai.kastrax.core.llm.*
import ai.kastrax.core.tools.Tool
import ai.kastrax.core.tools.tool
import io.mockk.*
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class AgentTest {

    private lateinit var mockLlmProvider: LlmProvider

    @BeforeEach
    fun setup() {
        mockLlmProvider = mockk()
    }

    @Test
    fun `test agent creation with DSL`() {
        val testAgent = agent {
            name = "TestAgent"
            instructions = "You are a test agent."
            model = mockLlmProvider
        }

        assertEquals("TestAgent", testAgent.name)
    }

    @Test
    fun `test agent generate method`() = runTest {
        // Mock LLM response
        val mockResponse = LlmResponse(
            content = "This is a test response",
            usage = LlmUsage(
                promptTokens = 10,
                completionTokens = 5,
                totalTokens = 15
            )
        )

        coEvery {
            mockLlmProvider.generate(any(), any())
        } returns mockResponse

        coEvery { mockLlmProvider.model } returns "test-model"

        // Create agent
        val testAgent = agent {
            name = "TestAgent"
            instructions = "You are a test agent."
            model = mockLlmProvider
        }

        // Test generate method
        val response = testAgent.generate("Hello, agent!")

        assertEquals("This is a test response", response.text)
        assertNotNull(response.usage)
        assertEquals(15, response.usage?.totalTokens)

        // Verify LLM was called with correct parameters
        coVerify {
            mockLlmProvider.generate(
                match { messages ->
                    messages.size == 2 &&
                    messages[0].role == LlmMessageRole.SYSTEM &&
                    messages[0].content == "You are a test agent." &&
                    messages[1].role == LlmMessageRole.USER &&
                    messages[1].content == "Hello, agent!"
                },
                any()
            )
        }
    }

    @Test
    fun `test agent stream method`() = runTest {
        // Mock LLM streaming response
        coEvery {
            mockLlmProvider.streamGenerate(any(), any())
        } returns flowOf("This ", "is ", "a ", "streaming ", "response")

        coEvery { mockLlmProvider.model } returns "test-model"

        // Create agent
        val testAgent = agent {
            name = "TestAgent"
            instructions = "You are a test agent."
            model = mockLlmProvider
        }

        // Test stream method
        val response = testAgent.stream("Hello, agent!")

        assertNotNull(response.textStream)

        // Collect the stream to verify content
        val streamContent = StringBuilder()
        response.textStream?.collect { chunk ->
            streamContent.append(chunk)
        }

        assertEquals("This is a streaming response", streamContent.toString())

        // Verify LLM was called with correct parameters
        coVerify {
            mockLlmProvider.streamGenerate(
                match { messages ->
                    messages.size == 2 &&
                    messages[0].role == LlmMessageRole.SYSTEM &&
                    messages[0].content == "You are a test agent." &&
                    messages[1].role == LlmMessageRole.USER &&
                    messages[1].content == "Hello, agent!"
                },
                any()
            )
        }
    }

    @Test
    fun `test agent with tools`() = runTest {
        // Mock tool
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

        // Mock LLM response with tool calls
        val toolCall = LlmToolCall(
            id = "call_123",
            name = "calculator",
            arguments = """{"expression": "2 + 2"}"""
        )

        val mockResponse = LlmResponse(
            content = "",
            toolCalls = listOf(toolCall),
            usage = LlmUsage(
                promptTokens = 15,
                completionTokens = 10,
                totalTokens = 25
            )
        )

        coEvery {
            mockLlmProvider.generate(any(), any())
        } returns mockResponse

        coEvery { mockLlmProvider.model } returns "test-model"

        // Create agent with tool
        val testAgent = agent {
            name = "CalculatorAgent"
            instructions = "You are a calculator agent."
            model = mockLlmProvider
            tools {
                tool(calculatorTool)
            }
        }

        // Test generate method with tool call
        val response = testAgent.generate("Calculate 2 + 2")

        // Verify tool was called
        assertEquals(1, response.toolCalls.size)
        assertEquals("calculator", response.toolCalls[0].name)
        assertEquals(1, response.toolResults.size)

        val toolResult = response.toolResults["call_123"]
        assertNotNull(toolResult)
        assertEquals(true, toolResult.success)

        val resultValue = toolResult.result?.jsonObject?.get("result")?.jsonPrimitive?.int
        assertEquals(4, resultValue)
    }

    // Helper function to evaluate simple expressions
    private fun evaluateExpression(expression: String): Int {
        // This is a very simplified calculator for testing
        val parts = expression.split("+", "-", "*", "/")
        if (parts.size != 2) return 0

        val a = parts[0].trim().toIntOrNull() ?: 0
        val b = parts[1].trim().toIntOrNull() ?: 0

        return when {
            "+" in expression -> a + b
            "-" in expression -> a - b
            "*" in expression -> a * b
            "/" in expression -> if (b != 0) a / b else 0
            else -> 0
        }
    }
}
