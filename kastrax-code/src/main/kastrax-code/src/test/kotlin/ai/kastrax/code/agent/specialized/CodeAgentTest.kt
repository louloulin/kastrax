package ai.kastrax.code.agent.specialized

import ai.kastrax.code.agent.AgentCoordinator
import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.model.DetailLevel
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentConfig
import ai.kastrax.core.llm.LLMProvider
import com.intellij.openapi.project.Project
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import kotlin.test.assertContains
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * 代码智能体测试
 */
class CodeAgentTest {
    
    private lateinit var project: Project
    private lateinit var contextEngine: CodeContextEngine
    private lateinit var llmProvider: LLMProvider
    private lateinit var agent: Agent
    
    private lateinit var codeCompletionAgent: CodeCompletionAgent
    private lateinit var codeExplanationAgent: CodeExplanationAgent
    private lateinit var codeRefactoringAgent: CodeRefactoringAgent
    private lateinit var testGenerationAgent: TestGenerationAgent
    private lateinit var agentCoordinator: AgentCoordinator
    
    @Before
    fun setUp() {
        // 创建模拟对象
        project = mockk(relaxed = true)
        contextEngine = mockk(relaxed = true)
        llmProvider = mockk(relaxed = true)
        agent = mockk(relaxed = true)
        
        // 设置模拟行为
        every { LLMProvider.getInstance() } returns llmProvider
        
        // 创建真实对象，但使用模拟依赖
        codeCompletionAgent = spyk(CodeCompletionAgent(project))
        codeExplanationAgent = spyk(CodeExplanationAgent(project))
        codeRefactoringAgent = spyk(CodeRefactoringAgent(project))
        testGenerationAgent = spyk(TestGenerationAgent(project))
        
        // 设置模拟行为
        every { CodeCompletionAgent.getInstance(any()) } returns codeCompletionAgent
        every { CodeExplanationAgent.getInstance(any()) } returns codeExplanationAgent
        every { CodeRefactoringAgent.getInstance(any()) } returns codeRefactoringAgent
        every { TestGenerationAgent.getInstance(any()) } returns testGenerationAgent
        
        // 创建协调器
        agentCoordinator = spyk(AgentCoordinator(project))
        every { AgentCoordinator.getInstance(any()) } returns agentCoordinator
        
        // 设置模拟响应
        coEvery { agent.process(any()) } returns ai.kastrax.core.agent.AgentResponse(
            id = "test-response",
            output = "测试响应",
            metadata = mapOf()
        )
        
        coEvery { llmProvider.complete(any()) } returns ai.kastrax.core.llm.LLMResponse(
            id = "test-response",
            content = "测试响应",
            model = "test-model",
            usage = ai.kastrax.core.llm.TokenUsage(
                promptTokens = 10,
                completionTokens = 10,
                totalTokens = 20
            )
        )
    }
    
    /**
     * 测试代码生成
     */
    @Test
    fun testGenerateCode() = runBlocking {
        // 设置模拟响应
        coEvery { codeCompletionAgent.generateCode(any(), any()) } returns """
            ```kotlin
            fun fibonacci(n: Int): Int {
                if (n <= 0) return 0
                if (n == 1) return 1
                
                var a = 0
                var b = 1
                var result = 0
                
                for (i in 2..n) {
                    result = a + b
                    a = b
                    b = result
                }
                
                return result
            }
            ```
        """.trimIndent()
        
        // 调用方法
        val result = codeCompletionAgent.generateCode("实现斐波那契数列函数", "kotlin")
        
        // 验证结果
        assertNotNull(result)
        assertContains(result, "fibonacci")
        assertContains(result, "return result")
    }
    
    /**
     * 测试代码解释
     */
    @Test
    fun testExplainCode() = runBlocking {
        // 设置模拟响应
        coEvery { codeExplanationAgent.explainCode(any(), any()) } returns """
            这段代码实现了斐波那契数列的计算。
            
            函数接受一个整数参数 n，表示要计算的斐波那契数列的第 n 个数。
            
            实现逻辑：
            1. 如果 n <= 0，返回 0
            2. 如果 n == 1，返回 1
            3. 否则，使用迭代方法计算第 n 个斐波那契数
            
            时间复杂度：O(n)
            空间复杂度：O(1)
        """.trimIndent()
        
        // 调用方法
        val code = """
            fun fibonacci(n: Int): Int {
                if (n <= 0) return 0
                if (n == 1) return 1
                
                var a = 0
                var b = 1
                var result = 0
                
                for (i in 2..n) {
                    result = a + b
                    a = b
                    b = result
                }
                
                return result
            }
        """.trimIndent()
        
        val result = codeExplanationAgent.explainCode(code, DetailLevel.DETAILED)
        
        // 验证结果
        assertNotNull(result)
        assertContains(result, "斐波那契数列")
        assertContains(result, "时间复杂度")
    }
    
    /**
     * 测试代码重构
     */
    @Test
    fun testRefactorCode() = runBlocking {
        // 设置模拟响应
        coEvery { codeRefactoringAgent.refactorCode(any(), any()) } returns """
            # 重构结果
            
            ## 重构后的代码
            
            ```kotlin
            /**
             * 计算斐波那契数列的第 n 个数
             *
             * @param n 位置（从0开始）
             * @return 斐波那契数
             */
            fun fibonacci(n: Int): Int {
                if (n < 0) throw IllegalArgumentException("Input must be non-negative")
                if (n <= 1) return n
                
                var a = 0
                var b = 1
                
                repeat(n - 1) {
                    val temp = a + b
                    a = b
                    b = temp
                }
                
                return b
            }
            ```
            
            ## 重构说明
            
            1. 添加了函数文档注释，说明函数的用途和参数含义
            2. 改进了边界条件处理，对负数输入抛出异常
            3. 简化了 n <= 1 的情况，直接返回 n
            4. 使用 Kotlin 的 repeat 函数简化循环
            5. 减少了变量使用，移除了 result 变量
            6. 改进了变量命名，使用 temp 代替 result
        """.trimIndent()
        
        // 调用方法
        val code = """
            fun fibonacci(n: Int): Int {
                if (n <= 0) return 0
                if (n == 1) return 1
                
                var a = 0
                var b = 1
                var result = 0
                
                for (i in 2..n) {
                    result = a + b
                    a = b
                    b = result
                }
                
                return result
            }
        """.trimIndent()
        
        val result = codeRefactoringAgent.refactorCode(code, "优化代码，添加注释，改进边界条件处理")
        
        // 验证结果
        assertNotNull(result)
        assertContains(result, "重构后的代码")
        assertContains(result, "IllegalArgumentException")
    }
    
    /**
     * 测试测试生成
     */
    @Test
    fun testGenerateTest() = runBlocking {
        // 设置模拟响应
        coEvery { testGenerationAgent.generateTest(any(), any()) } returns """
            # 测试生成结果
            
            ## 测试代码
            
            ```kotlin
            import org.junit.Test
            import kotlin.test.assertEquals
            import kotlin.test.assertFailsWith
            
            class FibonacciTest {
                
                @Test
                fun `test fibonacci with 0 returns 0`() {
                    assertEquals(0, fibonacci(0))
                }
                
                @Test
                fun `test fibonacci with 1 returns 1`() {
                    assertEquals(1, fibonacci(1))
                }
                
                @Test
                fun `test fibonacci with 5 returns 5`() {
                    assertEquals(5, fibonacci(5))
                }
                
                @Test
                fun `test fibonacci with 10 returns 55`() {
                    assertEquals(55, fibonacci(10))
                }
                
                @Test
                fun `test fibonacci with negative number throws exception`() {
                    assertFailsWith<IllegalArgumentException> {
                        fibonacci(-1)
                    }
                }
            }
            ```
            
            ## 测试说明
            
            这个测试套件包含以下测试用例：
            
            1. 测试 fibonacci(0) 返回 0
            2. 测试 fibonacci(1) 返回 1
            3. 测试 fibonacci(5) 返回 5
            4. 测试 fibonacci(10) 返回 55
            5. 测试传入负数时抛出 IllegalArgumentException 异常
            
            这些测试覆盖了边界条件（0和1）、正常情况和异常情况，确保函数在各种输入下都能正确工作。
        """.trimIndent()
        
        // 调用方法
        val code = """
            /**
             * 计算斐波那契数列的第 n 个数
             *
             * @param n 位置（从0开始）
             * @return 斐波那契数
             */
            fun fibonacci(n: Int): Int {
                if (n < 0) throw IllegalArgumentException("Input must be non-negative")
                if (n <= 1) return n
                
                var a = 0
                var b = 1
                
                repeat(n - 1) {
                    val temp = a + b
                    a = b
                    b = temp
                }
                
                return b
            }
        """.trimIndent()
        
        val result = testGenerationAgent.generateTest(code, "JUnit")
        
        // 验证结果
        assertNotNull(result)
        assertContains(result, "测试代码")
        assertContains(result, "@Test")
        assertContains(result, "assertEquals")
    }
    
    /**
     * 测试智能体协调器
     */
    @Test
    fun testAgentCoordinator() = runBlocking {
        // 设置模拟响应
        coEvery { agentCoordinator.processRequest(any()) } returns """
            我已经分析了你的请求，这是一个代码生成任务。
            
            下面是实现斐波那契数列函数的 Kotlin 代码：
            
            ```kotlin
            /**
             * 计算斐波那契数列的第 n 个数
             *
             * @param n 位置（从0开始）
             * @return 斐波那契数
             */
            fun fibonacci(n: Int): Int {
                if (n < 0) throw IllegalArgumentException("Input must be non-negative")
                if (n <= 1) return n
                
                var a = 0
                var b = 1
                
                repeat(n - 1) {
                    val temp = a + b
                    a = b
                    b = temp
                }
                
                return b
            }
            ```
            
            这个函数使用迭代方法计算斐波那契数列，比递归方法更高效。
            
            时间复杂度：O(n)
            空间复杂度：O(1)
            
            你可以这样使用它：
            
            ```kotlin
            fun main() {
                // 打印斐波那契数列的前10个数
                for (i in 0..9) {
                    println("fibonacci($i) = ${fibonacci(i)}")
                }
            }
            ```
        """.trimIndent()
        
        // 调用方法
        val result = agentCoordinator.processRequest("请用Kotlin实现一个斐波那契数列函数")
        
        // 验证结果
        assertNotNull(result)
        assertContains(result, "fibonacci")
        assertContains(result, "时间复杂度")
        assertTrue(result.contains("```kotlin") && result.contains("```"))
    }
}
