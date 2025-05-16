package ai.kastrax.code.agent.specialized

import ai.kastrax.code.agent.CodeAgent
import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.memory.ShortTermMemory
import ai.kastrax.code.model.DetailLevel
import ai.kastrax.code.model.ExplanationRequest
import ai.kastrax.code.model.ExplanationResult
import ai.kastrax.code.mock.Agent
import ai.kastrax.code.mock.AgentConfig
import ai.kastrax.code.mock.AgentContext
import ai.kastrax.code.mock.DeepSeekProvider
import ai.kastrax.code.mock.DeepSeekModel
import ai.kastrax.code.mock.deepSeek
import ai.kastrax.code.mock.LlmMessage
import ai.kastrax.code.mock.LlmMessageRole
import ai.kastrax.code.mock.LlmOptions
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 代码解释智能体
 *
 * 提供代码解释功能
 */
@Service(Service.Level.PROJECT)
class CodeExplanationAgent(
    private val project: Project,
    private val config: CodeExplanationAgentConfig = CodeExplanationAgentConfig()
) : KastraXCodeBase(component = "CODE_EXPLANATION_AGENT"), CodeAgent {

    // 使用父类的logger

    // 底层智能体
    private val agent: Agent by lazy {
        Agent(
            id = "code-explanation-agent",
            config = AgentConfig(
                name = "代码解释智能体",
                description = "提供代码解释功能",
                model = config.model,
                temperature = config.temperature,
                maxTokens = config.maxTokens
            )
        )
    }

    // DeepSeek提供者
    private val deepSeekProvider by lazy {
        deepSeek {
            model(DeepSeekModel.DEEPSEEK_CODER)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "mock-api-key")
            temperature(0.3)
            maxTokens(2000)
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
     * 解释代码
     *
     * @param code 代码
     * @param language 编程语言
     * @param detailLevel 详细程度
     * @return 解释结果
     */
    suspend fun explainCodeDetailed(
        code: String,
        language: String,
        detailLevel: DetailLevel = DetailLevel.NORMAL
    ): ExplanationResult = withContext(Dispatchers.IO) {
        try {
            logger.info { "解释代码, 语言: $language, 详细程度: $detailLevel" }

            // 创建解释请求
            val request = ExplanationRequest(
                code = code,
                language = language,
                detailLevel = detailLevel
            )

            // 创建提示
            val prompt = createExplanationPrompt(request)

            // 调用LLM
            val llmRequest = LLMRequest(
                model = config.model,
                prompt = prompt,
                temperature = config.temperature,
                maxTokens = config.maxTokens
            )

            val llmResponse = llmProvider.complete(llmRequest)

            // 存储到短期记忆
            shortTermMemory.storeMessage("user", "请解释以下${language}代码：\n$code")
            shortTermMemory.storeMessage("assistant", llmResponse.content)

            // 创建解释结果
            return@withContext ExplanationResult(
                id = UUID.randomUUID().toString(),
                explanation = llmResponse.content,
                detailLevel = detailLevel
            )
        } catch (e: Exception) {
            logger.error(e) { "解释代码时出错, 语言: $language, 详细程度: $detailLevel" }
            return@withContext ExplanationResult(
                id = UUID.randomUUID().toString(),
                explanation = "解释代码时出错: ${e.message}",
                detailLevel = detailLevel
            )
        }
    }

    /**
     * 创建解释提示
     *
     * @param request 解释请求
     * @return 提示
     */
    private fun createExplanationPrompt(request: ExplanationRequest): String {
        val sb = StringBuilder()

        // 系统提示
        sb.appendLine("你是一个专业的代码解释助手，擅长解释各种编程语言的代码。")

        // 详细程度
        when (request.detailLevel) {
            DetailLevel.BRIEF -> {
                sb.appendLine("请简要解释以下代码的主要功能和目的，不需要详细解释每一行。")
                sb.appendLine("解释应该简洁明了，只关注最重要的部分。")
            }
            DetailLevel.NORMAL -> {
                sb.appendLine("请解释以下代码的功能、结构和主要逻辑。")
                sb.appendLine("解释应该包括代码的目的、主要组件和工作原理。")
            }
            DetailLevel.DETAILED -> {
                sb.appendLine("请详细解释以下代码的每个部分，包括：")
                sb.appendLine("1. 代码的整体功能和目的")
                sb.appendLine("2. 代码的结构和组织")
                sb.appendLine("3. 每个主要部分的功能")
                sb.appendLine("4. 关键算法和逻辑")
                sb.appendLine("5. 潜在的优化点和改进建议")
                sb.appendLine("6. 可能的边界情况和错误处理")
            }
        }

        // 代码
        sb.appendLine("\n代码（${request.language}）：")
        sb.appendLine("```${request.language}")
        sb.appendLine(request.code)
        sb.appendLine("```")

        return sb.toString()
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
            val response = agent.process(agentContext)

            // 存储到短期记忆
            shortTermMemory.storeMessage("user", prompt)
            shortTermMemory.storeMessage("assistant", response.output)

            return@withContext response.output
        } catch (e: Exception) {
            logger.error(e) { "生成代码时出错: $prompt, 语言: $language" }
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

            // 检测语言
            val language = detectLanguage(code)

            // 获取详细解释
            val result = explainCodeDetailed(code, language, detailLevel)

            return@withContext result.explanation
        } catch (e: Exception) {
            logger.error(e) { "解释代码时出错, 详细程度: $detailLevel" }
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
            val response = agent.process(agentContext)

            // 存储到短期记忆
            shortTermMemory.storeMessage("user", "请根据以下指令重构代码：\n$instructions\n\n代码：\n$code")
            shortTermMemory.storeMessage("assistant", response.output)

            return@withContext response.output
        } catch (e: Exception) {
            logger.error(e) { "重构代码时出错: $instructions" }
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

            // 创建代理上下文
            val agentContext = AgentContext(
                input = code,
                metadata = mapOf(
                    "task" to "generateTest",
                    "framework" to framework
                )
            )

            // 调用代理
            val response = agent.process(agentContext)

            // 存储到短期记忆
            shortTermMemory.storeMessage("user", "请为以下代码生成$framework测试：\n$code")
            shortTermMemory.storeMessage("assistant", response.output)

            return@withContext response.output
        } catch (e: Exception) {
            logger.error(e) { "生成测试时出错: $framework" }
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
         * 获取项目的代码解释智能体实例
         *
         * @param project 项目
         * @return 代码解释智能体实例
         */
        fun getInstance(project: Project): CodeExplanationAgent {
            return project.service<CodeExplanationAgent>()
        }
    }
}

/**
 * 代码解释智能体配置
 *
 * @property model 模型
 * @property temperature 温度
 * @property maxTokens 最大令牌数
 */
data class CodeExplanationAgentConfig(
    val model: String = "deepseek-coder",
    val temperature: Double = 0.3,
    val maxTokens: Int = 2000
)
