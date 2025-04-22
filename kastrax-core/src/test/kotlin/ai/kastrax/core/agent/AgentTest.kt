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
import kotlin.test.assertTrue

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

    @Test
    fun `test agent with default options and toolsets`() = runTest {
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

        // Mock tools
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
                buildJsonObject {
                    put("result", 4)
                }
            }
        }

        val weatherTool = tool {
            id = "weather"
            name = "Weather"
            description = "Get weather information"
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
                }
            }
        }

        // Create agent with default options and toolsets
        val testAgent = agent {
            name = "AdvancedAgent"
            instructions = "You are an advanced agent."
            model = mockLlmProvider

            // Add base tools
            tools {
                tool(calculatorTool)
            }

            // Add toolset
            toolset("weather") {
                tool(weatherTool)
            }

            // Configure default generate options
            defaultGenerateOptions {
                temperature(0.5)
                maxTokens(100)
                topP(0.9)
                toolChoice(ToolChoice.Auto)
            }

            // Configure default stream options
            defaultStreamOptions {
                temperature(0.6)
                maxTokens(200)
            }
        }

        // Test generate with custom options
        val customOptions = AgentGenerateOptions(
            temperature = 0.3,
            toolChoice = ToolChoice.specific("calculator"),
            toolsets = mapOf("weather" to mapOf(weatherTool.id to weatherTool))
        )

        testAgent.generate("Hello", customOptions)

        // Verify LLM was called with merged options
        coVerify {
            mockLlmProvider.generate(
                any(),
                match { options ->
                    // Verify temperature was overridden
                    options.temperature == 0.3 &&
                    // Verify maxTokens was inherited from default options
                    options.maxTokens == 100 &&
                    // Verify toolChoice was set correctly
                    options.toolChoice is JsonObject &&
                    // Verify tools include both base tools and toolset tools
                    options.tools.size == 2
                }
            )
        }
    }

    @Test
    fun `test options merging`() {
        // Create base options
        val baseOptions = AgentGenerateOptions(
            temperature = 0.7,
            maxTokens = 100,
            toolChoice = ToolChoice.Auto,
            topP = 0.9,
            context = listOf(LlmMessage(role = LlmMessageRole.USER, content = "Base context"))
        )

        // Create override options
        val overrideOptions = AgentGenerateOptions(
            temperature = 0.5,
            toolChoice = ToolChoice.Required,
            context = listOf(LlmMessage(role = LlmMessageRole.USER, content = "Override context"))
        )

        // Merge options
        val mergedOptions = baseOptions.merge(overrideOptions)

        // Verify merged options
        assertEquals(0.5, mergedOptions.temperature)
        assertEquals(100, mergedOptions.maxTokens)
        assertEquals(ToolChoice.Required, mergedOptions.toolChoice)
        assertEquals(0.9, mergedOptions.topP)
        assertEquals(2, mergedOptions.context?.size)
        assertEquals("Base context", mergedOptions.context?.get(0)?.content)
        assertEquals("Override context", mergedOptions.context?.get(1)?.content)
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
