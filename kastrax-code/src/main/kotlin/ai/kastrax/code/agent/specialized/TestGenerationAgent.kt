package ai.kastrax.code.agent.specialized

import ai.kastrax.code.agent.AgentContext
import ai.kastrax.code.agent.CodeAgent
import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.memory.ShortTermMemory
import ai.kastrax.code.model.DetailLevel
import ai.kastrax.code.model.TestGenerationRequest
import ai.kastrax.code.model.TestGenerationResult
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmOptions
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.core.llm.LlmResponse
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 测试生成智能体
 *
 * 提供测试生成功能
 */
@Service(Service.Level.PROJECT)
class TestGenerationAgent(
    private val project: Project,
    private val config: TestGenerationAgentConfig = TestGenerationAgentConfig()
) : KastraXCodeBase(component = "TEST_GENERATION_AGENT"), CodeAgent {

    // 使用父类的logger

    // 底层智能体
    private val agent: Agent by lazy {
        agent {
            name = "测试生成智能体"
            instructions = "你是一个专业的测试生成助手，擅长为代码生成高质量的测试用例。"
            model = llmProvider
        }
    }

    // DeepSeek提供者
    private val llmProvider: LlmProvider by lazy {
        deepSeek {
            model(DeepSeekModel.DEEPSEEK_CODER)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "")
            temperature(0.2)
            maxTokens(3000)
        }
    }

    // 代码上下文引擎
    private val contextEngine: CodeContextEngine by lazy {
        CodeContextEngine.getInstance(project)
    }

    // 短期记忆
    private val shortTermMemory: ShortTermMemory by lazy {
        ShortTermMemory.getInstance(project)
    }

    /**
     * 生成测试
     *
     * @param code 代码
     * @param language 编程语言
     * @param framework 测试框架
     * @param coverage 覆盖率要求
     * @return 测试生成结果
     */
    suspend fun generateTestDetailed(
        code: String,
        language: String,
        framework: String,
        coverage: Double = 0.8
    ): TestGenerationResult = withContext(Dispatchers.IO) {
        try {
            logger.info { "生成测试, 语言: $language, 框架: $framework, 覆盖率: $coverage" }

            // 创建测试生成请求
            val request = TestGenerationRequest(
                code = code,
                language = language,
                framework = framework,
                coverage = coverage
            )

            // 获取上下文
            val context = contextEngine.getQueryContext("generate test for $language code", 10, 0.0, true)

            // 创建提示
            val prompt = createTestGenerationPrompt(request, context)

            // 调用LLM
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.SYSTEM,
                    content = "你是一个专业的测试生成助手，擅长为各种编程语言的代码生成高质量的测试。"
                ),
                LlmMessage(
                    role = LlmMessageRole.USER,
                    content = prompt
                )
            )

            val options = LlmOptions(
                temperature = config.temperature,
                maxTokens = config.maxTokens
            )

            val llmResponse = llmProvider.generate(messages, options)

            // 存储到短期记忆
            shortTermMemory.storeMessage("user", "请为以下${language}代码生成${framework}测试：\n$code")
            shortTermMemory.storeMessage("assistant", llmResponse.content)

            // 提取测试代码
            val testCode = extractCode(llmResponse.content, language)

            // 创建测试生成结果
            return@withContext TestGenerationResult(
                id = UUID.randomUUID().toString(),
                testCode = testCode,
                explanation = llmResponse.content.replace(testCode, "").trim(),
                framework = framework,
                coverage = coverage
            )
        } catch (e: Exception) {
            logger.error(e) { "生成测试时出错, 语言: $language, 框架: $framework" }
            return@withContext TestGenerationResult(
                id = UUID.randomUUID().toString(),
                testCode = "",
                explanation = "生成测试时出错: ${e.message}",
                framework = framework,
                coverage = coverage
            )
        }
    }

    /**
     * 创建测试生成提示
     *
     * @param request 测试生成请求
     * @param context 上下文
     * @return 提示
     */
    private fun createTestGenerationPrompt(request: TestGenerationRequest, context: ai.kastrax.code.model.Context): String {
        val sb = StringBuilder()

        // 系统提示
        sb.appendLine("你是一个专业的测试生成助手，擅长为各种编程语言的代码生成高质量的测试。")
        sb.appendLine("请为提供的代码生成测试，使用指定的测试框架。")
        sb.appendLine("测试应该覆盖代码的主要功能和边界情况，达到${request.coverage * 100}%的代码覆盖率。")
        sb.appendLine("请在回复中包含完整的测试代码，使用 ```${request.language} 和 ``` 包围代码。")
        sb.appendLine("同时提供测试说明，解释测试的目的和覆盖的场景。")
        sb.appendLine()

        // 测试框架信息
        sb.appendLine("## 测试框架")
        sb.appendLine("请使用 ${request.framework} 框架生成测试。")
        sb.appendLine()

        // 上下文
        if (context.elements.isNotEmpty()) {
            sb.appendLine("## 相关上下文")
            context.elements.forEach { element ->
                sb.appendLine("### ${element.type}: ${element.name}")
                sb.appendLine("```${request.language}")
                sb.appendLine(element.content)
                sb.appendLine("```")
                sb.appendLine()
            }
        }

        // 代码
        sb.appendLine("## 待测试代码")
        sb.appendLine("```${request.language}")
        sb.appendLine(request.code)
        sb.appendLine("```")

        return sb.toString()
    }

    /**
     * 提取代码
     *
     * @param content 内容
     * @param language 编程语言
     * @return 提取的代码
     */
    private fun extractCode(content: String, language: String): String {
        // 提取代码块
        val codePattern = "```(?:$language)?\\s*([\\s\\S]*?)```".toRegex()
        val match = codePattern.find(content)

        return if (match != null) {
            match.groupValues[1].trim()
        } else {
            // 如果没有找到代码块，尝试提取非Markdown部分
            val lines = content.lines()
            val nonMarkdownLines = lines.filter { !it.startsWith("#") && !it.startsWith(">") }
            nonMarkdownLines.joinToString("\n")
        }
    }

    /**
     * 生成代码
     *
     * @param prompt 提示
     * @param language 编程语言
     * @return 生成的代码
     */
    override suspend fun generateCode(prompt: String, language: String): String = withContext(Dispatchers.IO) {
        try {
            logger.info { "生成代码: $prompt, 语言: $language" }

            // 获取上下文
            val context = contextEngine.getQueryContext(prompt, 10, 0.0, true)

            // 创建代理上下文
            val agentContext = AgentContext(
                input = prompt,
                metadata = mapOf(
                    "language" to language,
                    "context" to context.getContent()
                )
            )

            // 调用代理
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.SYSTEM,
                    content = "你是一个专业的代码生成助手，擅长编写高质量的代码。"
                ),
                LlmMessage(
                    role = LlmMessageRole.USER,
                    content = agentContext.toString()
                )
            )

            val options = AgentGenerateOptions(
                temperature = config.temperature,
                maxTokens = config.maxTokens
            )

            val response = agent.generate(messages, options)

            // 存储到短期记忆
            shortTermMemory.storeMessage("user", prompt)
            shortTermMemory.storeMessage("assistant", response.text)

            return@withContext response.text
        } catch (e: Exception) {
            logger.error("生成代码时出错: $prompt, 语言: $language", e)
            return@withContext "生成代码时出错: ${e.message}"
        }
    }

    /**
     * 解释代码
     *
     * @param code 代码
     * @param detailLevel 详细程度
     * @return 解释
     */
    override suspend fun explainCode(code: String, detailLevel: DetailLevel): String = withContext(Dispatchers.IO) {
        try {
            logger.info { "解释代码, 详细程度: $detailLevel" }

            // 创建代理上下文
            val agentContext = AgentContext(
                input = code,
                metadata = mapOf(
                    "task" to "explain",
                    "detailLevel" to detailLevel.name
                )
            )

            // 调用代理
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.SYSTEM,
                    content = "你是一个专业的代码解释助手，擅长解释复杂的代码并提供清晰的解释。"
                ),
                LlmMessage(
                    role = LlmMessageRole.USER,
                    content = agentContext.toString()
                )
            )

            val options = AgentGenerateOptions(
                temperature = config.temperature,
                maxTokens = config.maxTokens
            )

            val response = agent.generate(messages, options)

            // 存储到短期记忆
            shortTermMemory.storeMessage("user", "请解释以下代码：\n$code")
            shortTermMemory.storeMessage("assistant", response.text)

            return@withContext response.text
        } catch (e: Exception) {
            logger.error("解释代码时出错, 详细程度: $detailLevel", e)
            return@withContext "解释代码时出错: ${e.message}"
        }
    }

    /**
     * 重构代码
     *
     * @param code 代码
     * @param instructions 指令
     * @return 重构后的代码
     */
    override suspend fun refactorCode(code: String, instructions: String): String = withContext(Dispatchers.IO) {
        try {
            logger.info { "重构代码: $instructions" }

            // 获取上下文
            val context = contextEngine.getQueryContext(instructions, 10, 0.0, true)

            // 创建代理上下文
            val agentContext = AgentContext(
                input = "$instructions\n\n$code",
                metadata = mapOf(
                    "task" to "refactor",
                    "context" to context.getContent()
                )
            )

            // 调用代理
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.SYSTEM,
                    content = "你是一个专业的代码重构助手，擅长优化和重构代码以提高其质量。"
                ),
                LlmMessage(
                    role = LlmMessageRole.USER,
                    content = agentContext.toString()
                )
            )

            val options = AgentGenerateOptions(
                temperature = config.temperature,
                maxTokens = config.maxTokens
            )

            val response = agent.generate(messages, options)

            // 存储到短期记忆
            shortTermMemory.storeMessage("user", "请根据以下指令重构代码：\n$instructions\n\n代码：\n$code")
            shortTermMemory.storeMessage("assistant", response.text)

            return@withContext response.text
        } catch (e: Exception) {
            logger.error("重构代码时出错: $instructions", e)
            return@withContext "重构代码时出错: ${e.message}"
        }
    }

    /**
     * 生成测试
     *
     * @param code 代码
     * @param framework 测试框架
     * @return 生成的测试代码
     */
    override suspend fun generateTest(code: String, framework: String): String = withContext(Dispatchers.IO) {
        try {
            logger.info { "生成测试: $framework" }

            // 检测语言
            val language = detectLanguage(code)

            // 获取详细测试生成结果
            val result = generateTestDetailed(code, language, framework)

            // 返回测试代码和解释
            return@withContext """
                # 测试生成结果

                ## 测试代码

                ```$language
                ${result.testCode}
                ```

                ## 测试说明

                ${result.explanation}
            """.trimIndent()
        } catch (e: Exception) {
            logger.error("生成测试时出错: $framework", e)
            return@withContext "生成测试时出错: ${e.message}"
        }
    }

    /**
     * 检测语言
     *
     * @param code 代码
     * @return 语言
     */
    private fun detectLanguage(code: String): String {
        // 简单的语言检测逻辑
        return when {
            code.contains("fun ") && code.contains("val ") -> "kotlin"
            code.contains("public class ") || code.contains("private class ") -> "java"
            code.contains("def ") && code.contains(":") -> "python"
            code.contains("function ") && code.contains("var ") -> "javascript"
            code.contains("import React") || code.contains("const [") -> "typescript"
            code.contains("<html>") || code.contains("<!DOCTYPE html>") -> "html"
            code.contains("@media") || code.contains("margin:") -> "css"
            else -> "text"
        }
    }

    /**
     * 补全代码
     *
     * @param code 当前代码
     * @param language 编程语言
     * @param maxTokens 最大生成令牌数
     * @return 补全的代码
     */
    override suspend fun complete(code: String, language: String, maxTokens: Int): String = withContext(Dispatchers.IO) {
        try {
            logger.info { "补全代码, 语言: $language" }

            // 生成代码
            return@withContext generateCode("补全以下代码: $code", language)
        } catch (e: Exception) {
            logger.error(e) { "补全代码时出错, 语言: $language" }
            return@withContext ""
        }
    }

    companion object {
        /**
         * 获取项目的测试生成智能体实例
         *
         * @param project 项目
         * @return 测试生成智能体实例
         */
        fun getInstance(project: Project): TestGenerationAgent {
            return project.service<TestGenerationAgent>()
        }
    }
}

/**
 * 测试生成智能体配置
 *
 * @property model 模型
 * @property temperature 温度
 * @property maxTokens 最大令牌数
 */
data class TestGenerationAgentConfig(
    val model: String = "deepseek-coder",
    val temperature: Double = 0.2,
    val maxTokens: Int = 3000
)
