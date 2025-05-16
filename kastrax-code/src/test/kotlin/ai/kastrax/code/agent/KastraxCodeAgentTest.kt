package ai.kastrax.code.agent

import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.model.DetailLevel
import ai.kastrax.code.tools.CodeToolRegistry
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentResponse
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class KastraxCodeAgentTest {
    
    private lateinit var mockAgent: Agent
    private lateinit var mockContextEngine: CodeContextEngine
    private lateinit var mockToolRegistry: CodeToolRegistry
    private lateinit var codeAgent: KastraxCodeAgent
    
    @Before
    fun setup() {
        mockAgent = mockk()
        mockContextEngine = mockk()
        mockToolRegistry = mockk()
        
        codeAgent = KastraxCodeAgent(
            agent = mockAgent,
            contextEngine = mockContextEngine,
            toolRegistry = mockToolRegistry,
            config = CodeAgentConfig()
        )
    }
    
    @Test
    fun `test generateCode returns expected code`() = runBlocking {
        // Arrange
        val prompt = "Create a function to calculate factorial"
        val language = "kotlin"
        val expectedCode = "fun factorial(n: Int): Int {\n    return if (n <= 1) 1 else n * factorial(n - 1)\n}"
        val mockResponse = AgentResponse(
            text = "```kotlin\n$expectedCode\n```",
            usage = null,
            finishReason = "stop"
        )
        
        coEvery { mockAgent.generate(any(), any()) } returns mockResponse
        
        // Act
        val result = codeAgent.generateCode(prompt, language)
        
        // Assert
        assertEquals(expectedCode, result)
    }
    
    @Test
    fun `test explainCode returns explanation`() = runBlocking {
        // Arrange
        val code = "fun factorial(n: Int): Int {\n    return if (n <= 1) 1 else n * factorial(n - 1)\n}"
        val detailLevel = DetailLevel.BASIC
        val expectedExplanation = "This is a recursive function to calculate factorial."
        val mockResponse = AgentResponse(
            text = expectedExplanation,
            usage = null,
            finishReason = "stop"
        )
        
        coEvery { mockAgent.generate(any(), any()) } returns mockResponse
        
        // Act
        val result = codeAgent.explainCode(code, detailLevel)
        
        // Assert
        assertEquals(expectedExplanation, result)
    }
    
    @Test
    fun `test refactorCode returns refactored code`() = runBlocking {
        // Arrange
        val code = "fun factorial(n: Int): Int {\n    return if (n <= 1) 1 else n * factorial(n - 1)\n}"
        val instructions = "Convert to iterative approach"
        val expectedCode = "fun factorial(n: Int): Int {\n    var result = 1\n    for (i in 2..n) {\n        result *= i\n    }\n    return result\n}"
        val mockResponse = AgentResponse(
            text = "```kotlin\n$expectedCode\n```",
            usage = null,
            finishReason = "stop"
        )
        
        coEvery { mockAgent.generate(any(), any()) } returns mockResponse
        
        // Act
        val result = codeAgent.refactorCode(code, instructions)
        
        // Assert
        assertEquals(expectedCode, result)
    }
    
    @Test
    fun `test generateTest returns test code`() = runBlocking {
        // Arrange
        val code = "fun factorial(n: Int): Int {\n    return if (n <= 1) 1 else n * factorial(n - 1)\n}"
        val framework = "JUnit"
        val expectedTestCode = """
            @Test
            fun testFactorial() {
                assertEquals(1, factorial(0))
                assertEquals(1, factorial(1))
                assertEquals(2, factorial(2))
                assertEquals(6, factorial(3))
                assertEquals(24, factorial(4))
                assertEquals(120, factorial(5))
            }
        """.trimIndent()
        val mockResponse = AgentResponse(
            text = "```kotlin\n$expectedTestCode\n```",
            usage = null,
            finishReason = "stop"
        )
        
        coEvery { mockAgent.generate(any(), any()) } returns mockResponse
        
        // Act
        val result = codeAgent.generateTest(code, framework)
        
        // Assert
        assertEquals(expectedTestCode, result)
    }
    
    @Test
    fun `test complete returns completion`() = runBlocking {
        // Arrange
        val code = "fun factorial(n: Int): Int {\n    return if (n <= 1) 1 else"
        val language = "kotlin"
        val expectedCompletion = "n * factorial(n - 1)"
        val mockResponse = AgentResponse(
            text = expectedCompletion,
            usage = null,
            finishReason = "stop"
        )
        
        coEvery { mockAgent.generate(any(), any()) } returns mockResponse
        
        // Act
        val result = codeAgent.complete(code, language)
        
        // Assert
        assertEquals(expectedCompletion, result)
    }
}
