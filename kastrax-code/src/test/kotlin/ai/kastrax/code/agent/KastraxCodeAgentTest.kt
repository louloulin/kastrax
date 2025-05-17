package ai.kastrax.code.agent

import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.context.CodeContextEngineConfig
import ai.kastrax.code.context.KastraxCodeContextEngine
import ai.kastrax.code.model.DetailLevel
import ai.kastrax.code.tools.CodeToolRegistry
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.agent
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.slf4j.LoggerFactory
import com.intellij.openapi.project.Project

class KastraxCodeAgentTest {

    private val logger = LoggerFactory.getLogger(KastraxCodeAgentTest::class.java)
    private lateinit var realAgent: Agent
    private lateinit var contextEngine: KastraxCodeContextEngine
    private lateinit var toolRegistry: CodeToolRegistry
    private lateinit var mockProject: Project
    private lateinit var codeAgent: KastraxCodeAgent

    @Before
    fun setup() {
        // 使用真实的 DeepSeek LLM
        realAgent = agent {
            name = "DeepSeek测试代理"
            instructions = "你是一个专业的编程助手，擅长代码生成、解释、重构和测试。"
            model = deepSeek {
                model(DeepSeekModel.DEEPSEEK_CODER)
                apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
                temperature(0.3)
                maxTokens(2000)
            }
        }

        // 创建模拟项目
        mockProject = mockk(relaxed = true)

        // 创建上下文引擎和工具注册表
        contextEngine = KastraxCodeContextEngine(CodeContextEngineConfig())
        toolRegistry = CodeToolRegistry(mockProject)

        // 使用真实的 Agent 创建代码智能体
        codeAgent = KastraxCodeAgent(
            agent = realAgent,
            contextEngine = contextEngine,
            toolRegistry = toolRegistry,
            config = CodeAgentConfig()
        )

        logger.info("使用真实的 DeepSeek Agent 设置测试环境")
    }

    @Test
    fun `test generateCode returns expected code`() = runBlocking {
        // Arrange
        val prompt = "Create a function to calculate factorial"
        val language = "kotlin"

        // Act
        logger.info("测试生成代码功能，提示：$prompt，语言：$language")
        val result = codeAgent.generateCode(prompt, language)
        logger.info("生成的代码：$result")

        // Assert
        // 使用真实的 DeepSeek LLM，输出可能不同，所以我们只检查输出不为空且包含关键词
        assertTrue(result.isNotEmpty())
        assertTrue("factorial" in result)
        assertTrue("Int" in result)
    }

    @Test
    fun `test explainCode returns explanation`() = runBlocking {
        // Arrange
        val code = "fun factorial(n: Int): Int {\n    return if (n <= 1) 1 else n * factorial(n - 1)\n}"
        val detailLevel = DetailLevel.NORMAL

        // Act
        logger.info("测试解释代码功能，详细程度：$detailLevel")
        val result = codeAgent.explainCode(code, detailLevel)
        logger.info("代码解释：$result")

        // Assert
        // 使用真实的 DeepSeek LLM，输出可能不同，所以我们只检查输出不为空且包含关键词
        assertTrue(result.isNotEmpty())
        assertTrue(result.contains("factorial") || result.contains("阶乘"))
    }

    @Test
    fun `test refactorCode returns refactored code`() = runBlocking {
        // Arrange
        val code = "fun factorial(n: Int): Int {\n    return if (n <= 1) 1 else n * factorial(n - 1)\n}"
        val instructions = "Convert to iterative approach"

        // Act
        logger.info("测试重构代码功能，指令：$instructions")
        val result = codeAgent.refactorCode(code, instructions)
        logger.info("重构后的代码：$result")

        // Assert
        // 使用真实的 DeepSeek LLM，输出可能不同，所以我们只检查输出不为空且包含关键词
        assertTrue(result.isNotEmpty())
        assertTrue("factorial" in result)
        assertTrue("for" in result || "while" in result) // 迭代方法应该包含循环
    }

    @Test
    fun `test generateTest returns test code`() = runBlocking {
        // Arrange
        val code = "fun factorial(n: Int): Int {\n    return if (n <= 1) 1 else n * factorial(n - 1)\n}"
        val framework = "JUnit"

        // Act
        logger.info("测试生成测试代码功能，框架：$framework")
        val result = codeAgent.generateTest(code, framework)
        logger.info("生成的测试代码：$result")

        // Assert
        // 使用真实的 DeepSeek LLM，输出可能不同，所以我们只检查输出不为空且包含关键词
        assertTrue(result.isNotEmpty())
        assertTrue("@Test" in result)
        assertTrue("factorial" in result)
        assertTrue("assertEquals" in result)
    }

    @Test
    fun `test complete returns completion`() = runBlocking {
        // Arrange
        val code = "fun factorial(n: Int): Int {\n    return if (n <= 1) 1 else"
        val language = "kotlin"

        // Act
        logger.info("测试代码补全功能，语言：$language")
        val result = codeAgent.complete(code, language)
        logger.info("补全的代码：$result")

        // Assert
        // 使用真实的 DeepSeek LLM，输出可能不同，所以我们只检查输出不为空且包含关键词
        assertTrue(result.isNotEmpty())
        assertTrue("factorial" in result || "*" in result) // 应该包含递归调用或乘法运算符
    }
}
