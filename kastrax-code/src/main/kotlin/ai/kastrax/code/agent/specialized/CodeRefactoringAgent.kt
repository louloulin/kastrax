package ai.kastrax.code.agent.specialized

import ai.kastrax.code.agent.AgentContext
import ai.kastrax.code.agent.CodeAgent
import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.memory.ShortTermMemory
import ai.kastrax.code.model.DetailLevel
import ai.kastrax.code.model.RefactoringRequest
import ai.kastrax.code.model.RefactoringResult
import ai.kastrax.code.workflow.CheckpointManager
import ai.kastrax.code.workflow.CheckpointManagerImpl
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
 * 代码重构智能体
 *
 * 提供代码重构功能
 */
@Service(Service.Level.PROJECT)
class CodeRefactoringAgent(
    private val project: Project,
    private val config: CodeRefactoringAgentConfig = CodeRefactoringAgentConfig()
) : KastraXCodeBase(component = "CODE_REFACTORING_AGENT"), CodeAgent {

    // 使用父类的logger

    // 底层智能体
    private val agent: Agent by lazy {
        agent {
            name = "代码重构智能体"
            instructions = "你是一个专业的代码重构助手，擅长优化和重构代码以提高其质量。"
            model = llmProvider
        }
    }

    // DeepSeek提供者
    private val llmProvider: LlmProvider by lazy {
        deepSeek {
            model(DeepSeekModel.DEEPSEEK_CODER)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "")
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

    // 检查点管理器
    private val checkpointManager: CheckpointManager by lazy {
        CheckpointManagerImpl.getInstance(project)
    }

    /**
     * 重构代码
     *
     * @param code 代码
     * @param instructions 指令
     * @param language 编程语言
     * @param createCheckpoint 是否创建检查点
     * @return 重构结果
     */
    suspend fun refactorCodeDetailed(
        code: String,
        instructions: String,
        language: String,
        createCheckpoint: Boolean = true
    ): RefactoringResult = withContext(Dispatchers.IO) {
        try {
            logger.info("重构代码: $instructions, 语言: $language")

            // 创建检查点
            if (createCheckpoint) {
                val checkpoint = checkpointManager.createCheckpoint(
                    name = "重构前检查点",
                    description = "重构前自动创建的检查点"
                )
                logger.info("创建重构前检查点: ${checkpoint.id}")
            }

            // 创建重构请求
            val request = RefactoringRequest(
                code = code,
                instructions = instructions,
                language = language
            )

            // 获取上下文
            val context = contextEngine.getQueryContext(instructions, 10, 0.0, true)

            // 创建提示
            val prompt = createRefactoringPrompt(request, context)

            // 调用LLM
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.SYSTEM,
                    content = "你是一个专业的代码重构助手，擅长优化和重构代码以提高其质量。"
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
            shortTermMemory.storeMessage("user", "请根据以下指令重构代码：\n$instructions\n\n代码：\n$code")
            shortTermMemory.storeMessage("assistant", llmResponse.content)

            // 提取重构后的代码
            val refactoredCode = extractCode(llmResponse.content, language)

            // 创建重构结果
            return@withContext RefactoringResult(
                id = UUID.randomUUID().toString(),
                originalCode = code,
                refactoredCode = refactoredCode,
                explanation = llmResponse.content.replace(refactoredCode, "").trim()
            )
        } catch (e: Exception) {
            logger.error("重构代码时出错: $instructions, 语言: $language", e)
            return@withContext RefactoringResult(
                id = UUID.randomUUID().toString(),
                originalCode = code,
                refactoredCode = code,
                explanation = "重构代码时出错: ${e.message}"
            )
        }
    }

    /**
     * 创建重构提示
     *
     * @param request 重构请求
     * @param context 上下文
     * @return 提示
     */
    private fun createRefactoringPrompt(request: RefactoringRequest, context: ai.kastrax.code.model.Context): String {
        val sb = StringBuilder()

        // 系统提示
        sb.appendLine("你是一个专业的代码重构助手，擅长根据指令重构代码，提高代码质量、可读性和性能。")
        sb.appendLine("请根据提供的指令重构代码，并解释你所做的更改。")
        sb.appendLine("重构后的代码应该保持原有功能，同时改进代码质量。")
        sb.appendLine("请在回复中包含完整的重构后代码，使用 ```${request.language} 和 ``` 包围代码。")
        sb.appendLine("同时提供详细的重构说明，解释你所做的更改和改进。")
        sb.appendLine()

        // 上下文
        if (context.elements.isNotEmpty()) {
            sb.appendLine("## 相关上下文")
            context.elements.forEach { element ->
                sb.appendLine("### ${element.element.type}: ${element.element.name}")
                sb.appendLine("```${request.language}")
                sb.appendLine(element.content)
                sb.appendLine("```")
                sb.appendLine()
            }
        }

        // 重构指令
        sb.appendLine("## 重构指令")
        sb.appendLine(request.instructions)
        sb.appendLine()

        // 原始代码
        sb.appendLine("## 原始代码")
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
            logger.info("生成代码: $prompt, 语言: $language")

            // 获取上下文
            val context = contextEngine.getQueryContext(prompt, 10, 0.0, true)

            // 创建代理上下文
            val agentContext = AgentContext(
                input = prompt,
                metadata = mapOf(
                    "language" to language,
                    "context" to context.toString()
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
            logger.info("解释代码, 详细程度: $detailLevel")

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

            // 检测语言
            val language = detectLanguage(code)

            // 获取详细重构结果
            val result = refactorCodeDetailed(code, instructions, language)

            // 返回重构后的代码和解释
            return@withContext """
                # 重构结果

                ## 重构后的代码

                ```$language
                ${result.refactoredCode}
                ```

                ## 重构说明

                ${result.explanation}
            """.trimIndent()
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
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.SYSTEM,
                    content = "你是一个专业的测试生成助手，擅长为代码生成高质量的测试用例。"
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
            shortTermMemory.storeMessage("user", "请为以下代码生成${framework}测试：\n$code")
            shortTermMemory.storeMessage("assistant", response.text)

            return@withContext response.text
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
         * 获取项目的代码重构智能体实例
         *
         * @param project 项目
         * @return 代码重构智能体实例
         */
        fun getInstance(project: Project): CodeRefactoringAgent {
            return project.service<CodeRefactoringAgent>()
        }
    }
}

/**
 * 代码重构智能体配置
 *
 * @property model 模型
 * @property temperature 温度
 * @property maxTokens 最大令牌数
 */
data class CodeRefactoringAgentConfig(
    val model: String = "deepseek-coder",
    val temperature: Double = 0.2,
    val maxTokens: Int = 3000
)
