package ai.kastrax.code.agent.specialized

import ai.kastrax.code.agent.AgentContext
import ai.kastrax.code.agent.CodeAgent
import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.memory.ShortTermMemory
import ai.kastrax.code.model.CompletionRequest
import ai.kastrax.code.model.CompletionResult
import ai.kastrax.code.model.Context
import ai.kastrax.code.model.DetailLevel
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
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * 代码补全智能体
 *
 * 提供智能代码补全功能
 */
@Service(Service.Level.PROJECT)
class CodeCompletionAgent(
    private val project: Project,
    private val config: CodeCompletionAgentConfig = CodeCompletionAgentConfig()
) : KastraXCodeBase(component = "CODE_COMPLETION_AGENT"), CodeAgent {

    // 使用父类的logger

    // DeepSeek提供者
    private val llmProvider: LlmProvider by lazy {
        deepSeek {
            model(DeepSeekModel.DEEPSEEK_CODER)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "")
            temperature(0.2)
            maxTokens(1000)
        }
    }

    // 底层智能体
    private val agent: Agent by lazy {
        agent {
            name = "代码补全智能体"
            instructions = "你是一个专业的代码补全助手，擅长根据上下文提供高质量的代码补全建议。"
            model = llmProvider
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
     * 获取代码补全
     *
     * @param code 当前代码
     * @param prefix 前缀
     * @param language 编程语言
     * @param maxCompletions 最大补全数量
     * @return 补全结果
     */
    suspend fun getCompletions(
        code: String,
        prefix: String,
        language: String,
        maxCompletions: Int = 5
    ): List<CompletionResult> = withContext(Dispatchers.IO) {
        try {
            logger.info("获取代码补全: $prefix, 语言: $language")

            // 创建补全请求
            val request = CompletionRequest(
                code = code,
                prefix = prefix,
                language = language,
                maxCompletions = maxCompletions
            )

            // 获取上下文
            val context = getCompletionContext(code, prefix, language)

            // 创建提示
            val prompt = createCompletionPrompt(request, context)

            // 调用LLM
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.SYSTEM,
                    content = "你是一个专业的代码补全助手，擅长根据上下文提供高质量的代码补全建议。"
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

            val response = llmProvider.generate(messages, options)

            // 解析补全结果
            return@withContext parseCompletionResults(response, maxCompletions)
        } catch (e: Exception) {
            logger.error("获取代码补全时出错: $prefix, 语言: $language", e)
            return@withContext emptyList()
        }
    }

    /**
     * 获取内联补全
     *
     * @param code 当前代码
     * @param cursorPosition 光标位置
     * @param language 编程语言
     * @return 补全结果
     */
    suspend fun getInlineCompletion(
        code: String,
        cursorPosition: Int,
        language: String
    ): CompletionResult? = withContext(Dispatchers.IO) {
        try {
            logger.info("获取内联补全, 语言: $language")

            // 获取前缀
            val prefix = code.substring(0, cursorPosition)

            // 获取补全
            val completions = getCompletions(code, prefix, language, 1)

            return@withContext completions.firstOrNull()
        } catch (e: Exception) {
            logger.error("获取内联补全时出错, 语言: $language", e)
            return@withContext null
        }
    }

    /**
     * 获取补全上下文
     *
     * @param code 当前代码
     * @param prefix 前缀
     * @param language 编程语言
     * @return 上下文
     */
    private suspend fun getCompletionContext(code: String, prefix: String, language: String): Context {
        // 创建查询
        val query = "code completion for $language: $prefix"

        // 获取上下文
        return contextEngine.getQueryContext(query, 10, 0.0, true)
    }

    /**
     * 创建补全提示
     *
     * @param request 补全请求
     * @param context 上下文
     * @return 提示
     */
    private fun createCompletionPrompt(request: CompletionRequest, context: Context): String {
        val sb = StringBuilder()

        // 系统提示
        sb.appendLine("你是一个专业的代码补全助手，擅长根据上下文提供高质量的代码补全建议。")
        sb.appendLine("请根据提供的代码和前缀，生成最多${request.maxCompletions}个可能的代码补全。")
        sb.appendLine("补全应该是高质量的、符合编程语言规范的、与上下文相关的。")
        sb.appendLine("请以JSON格式返回结果，格式为：")
        sb.appendLine("""
            {
              "completions": [
                {
                  "text": "补全文本1",
                  "displayText": "显示文本1",
                  "confidence": 0.95
                },
                {
                  "text": "补全文本2",
                  "displayText": "显示文本2",
                  "confidence": 0.85
                }
              ]
            }
        """.trimIndent())
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

        // 当前代码
        sb.appendLine("## 当前代码")
        sb.appendLine("```${request.language}")
        sb.appendLine(request.code)
        sb.appendLine("```")
        sb.appendLine()

        // 前缀
        sb.appendLine("## 需要补全的前缀")
        sb.appendLine("```")
        sb.appendLine(request.prefix)
        sb.appendLine("```")

        return sb.toString()
    }

    /**
     * 解析补全结果
     *
     * @param response LLM响应
     * @param maxCompletions 最大补全数量
     * @return 补全结果列表
     */
    private fun parseCompletionResults(response: LlmResponse, maxCompletions: Int): List<CompletionResult> {
        try {
            // 提取JSON部分
            val jsonPattern = "\\{[\\s\\S]*?\"completions\"[\\s\\S]*?\\}".toRegex()
            val jsonMatch = jsonPattern.find(response.content)

            if (jsonMatch != null) {
                val jsonString = jsonMatch.value

                // 解析JSON
                val json = kotlinx.serialization.json.Json.decodeFromString<CompletionResultsJson>(jsonString)

                // 转换为补全结果
                return json.completions.take(maxCompletions).map { completion ->
                    CompletionResult(
                        id = UUID.randomUUID().toString(),
                        text = completion.text,
                        displayText = completion.displayText,
                        confidence = completion.confidence
                    )
                }
            }

            // 如果无法解析JSON，尝试直接提取补全
            val completionPattern = "```[\\s\\S]*?([\\s\\S]*?)```".toRegex()
            val completionMatches = completionPattern.findAll(response.content)

            if (completionMatches.count() > 0) {
                return completionMatches.take(maxCompletions).mapIndexed { index, match ->
                    val text = match.groupValues[1].trim()
                    CompletionResult(
                        id = UUID.randomUUID().toString(),
                        text = text,
                        displayText = text.take(50) + if (text.length > 50) "..." else "",
                        confidence = 1.0 - (index * 0.1)
                    )
                }.toList()
            }

            // 如果都无法解析，返回空列表
            return emptyList()
        } catch (e: Exception) {
            logger.error("解析补全结果时出错", e)
            return emptyList()
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
            val response = agent.process(agentContext)

            // 存储到短期记忆
            shortTermMemory.storeMessage("user", prompt)
            shortTermMemory.storeMessage("assistant", response.output)

            return@withContext response.output
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
            val response = agent.process(agentContext)

            // 存储到短期记忆
            shortTermMemory.storeMessage("user", "请解释以下代码：\n$code")
            shortTermMemory.storeMessage("assistant", response.output)

            return@withContext response.output
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
            logger.info("重构代码: $instructions")

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
            logger.info("生成测试: $framework")

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
            logger.error("生成测试时出错: $framework", e)
            return@withContext "生成测试时出错: ${e.message}"
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
            logger.info("补全代码, 语言: $language")

            // 获取内联补全
            val completion = getInlineCompletion(code, code.length, language)

            return@withContext completion?.text ?: ""
        } catch (e: Exception) {
            logger.error("补全代码时出错, 语言: $language", e)
            return@withContext ""
        }
    }

    companion object {
        /**
         * 获取项目的代码补全智能体实例
         *
         * @param project 项目
         * @return 代码补全智能体实例
         */
        fun getInstance(project: Project): CodeCompletionAgent {
            return project.service<CodeCompletionAgent>()
        }
    }
}



/**
 * 补全结果JSON
 *
 * @property completions 补全列表
 */
@kotlinx.serialization.Serializable
data class CompletionResultsJson(
    val completions: List<CompletionJson>
)

/**
 * 补全JSON
 *
 * @property text 文本
 * @property displayText 显示文本
 * @property confidence 置信度
 */
@kotlinx.serialization.Serializable
data class CompletionJson(
    val text: String,
    val displayText: String,
    val confidence: Double
)
