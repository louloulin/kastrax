package ai.kastrax.code.agent

import ai.kastrax.code.agent.specialized.CodeCompletionAgent
import ai.kastrax.code.model.DetailLevel
import com.intellij.openapi.project.Project
import com.intellij.testFramework.LightPlatformTestCase
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.mockito.Mockito.mock

/**
 * 代码智能体测试
 */
class CodeAgentTest : LightPlatformTestCase() {

    private lateinit var codeAgent: CodeAgent
    private lateinit var project: Project

    override fun setUp() {
        super.setUp()
        project = mock(Project::class.java)
        codeAgent = CodeCompletionAgent(project)
    }

    /**
     * 测试生成代码
     */
    @Test
    fun testGenerateCode() = runBlocking {
        val prompt = "创建一个简单的Kotlin函数，计算两个数的和"
        val language = "kotlin"
        
        val result = codeAgent.generateCode(prompt, language)
        
        assertNotNull("生成的代码不应为空", result)
        assertTrue("生成的代码应包含函数定义", result.contains("fun"))
    }

    /**
     * 测试解释代码
     */
    @Test
    fun testExplainCode() = runBlocking {
        val code = """
            fun sum(a: Int, b: Int): Int {
                return a + b
            }
        """.trimIndent()
        
        val result = codeAgent.explainCode(code, DetailLevel.NORMAL)
        
        assertNotNull("解释不应为空", result)
        assertTrue("解释应包含函数的描述", result.contains("sum") || result.contains("计算"))
    }

    /**
     * 测试重构代码
     */
    @Test
    fun testRefactorCode() = runBlocking {
        val code = """
            fun sum(a: Int, b: Int): Int {
                return a + b
            }
        """.trimIndent()
        
        val instructions = "将函数改为使用表达式体语法"
        
        val result = codeAgent.refactorCode(code, instructions)
        
        assertNotNull("重构后的代码不应为空", result)
        assertTrue("重构后的代码应使用表达式体语法", result.contains("="))
    }

    /**
     * 测试生成测试
     */
    @Test
    fun testGenerateTest() = runBlocking {
        val code = """
            fun sum(a: Int, b: Int): Int {
                return a + b
            }
        """.trimIndent()
        
        val framework = "JUnit"
        
        val result = codeAgent.generateTest(code, framework)
        
        assertNotNull("生成的测试不应为空", result)
        assertTrue("生成的测试应包含测试框架相关内容", 
            result.contains("@Test") || result.contains("test") || result.contains("assert"))
    }

    /**
     * 测试代码补全
     */
    @Test
    fun testComplete() = runBlocking {
        val code = """
            fun sum(a: Int, b: Int): Int {
                return 
        """.trimIndent()
        
        val language = "kotlin"
        
        val result = codeAgent.complete(code, language)
        
        assertNotNull("补全的代码不应为空", result)
    }
}
