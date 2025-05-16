package ai.kastrax.code.agent

import ai.kastrax.code.agent.specialized.CodeCompletionAgent
import ai.kastrax.code.agent.specialized.CodeExplanationAgent
import ai.kastrax.code.agent.specialized.CodeRefactoringAgent
import ai.kastrax.code.agent.specialized.TestGenerationAgent
import ai.kastrax.code.common.KastraXCodeBase
import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.memory.ShortTermMemory
import ai.kastrax.code.model.DetailLevel
import ai.kastrax.code.model.TaskType
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentNetwork
import ai.kastrax.core.agent.AgentNetworkConfig
import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.agentNetwork
import ai.kastrax.core.agent.routing.RoutingStrategy
import ai.kastrax.core.agent.routing.ContextAwareRoutingStrategy
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import ai.kastrax.core.llm.LlmProvider
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 智能体协调器
 *
 * 协调多个智能体协作解决复杂问题
 */
@Service(Service.Level.PROJECT)
class AgentCoordinator(
    private val project: Project,
    private val config: AgentCoordinatorConfig = AgentCoordinatorConfig()
) : KastraXCodeBase(component = "AGENT_COORDINATOR") {

    // 使用父类的logger

    // DeepSeek提供者
    private val llmProvider: LlmProvider by lazy {
        deepSeek {
            model(DeepSeekModel.DEEPSEEK_CODER)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "")
            temperature(0.3)
            maxTokens(2000)
        }
    }

    // 代码补全智能体
    private val codeCompletionAgent: Agent by lazy {
        agent {
            name = "代码补全智能体"
            instructions = "你是一个专业的代码补全助手，擅长根据上下文提供高质量的代码补全建议。"
            model = llmProvider
        }
    }

    // 代码解释智能体
    private val codeExplanationAgent: Agent by lazy {
        agent {
            name = "代码解释智能体"
            instructions = "你是一个专业的代码解释助手，擅长解释复杂的代码并提供清晰的解释。"
            model = llmProvider
        }
    }

    // 代码重构智能体
    private val codeRefactoringAgent: Agent by lazy {
        agent {
            name = "代码重构智能体"
            instructions = "你是一个专业的代码重构助手，擅长优化和重构代码以提高其质量。"
            model = llmProvider
        }
    }

    // 测试生成智能体
    private val testGenerationAgent: Agent by lazy {
        agent {
            name = "测试生成智能体"
            instructions = "你是一个专业的测试生成助手，擅长为代码生成高质量的测试用例。"
            model = llmProvider
        }
    }

    // 智能体网络
    private val agentNetwork: AgentNetwork by lazy {
        agentNetwork {
            name = "代码智能体网络"
            instructions = "你是一个代码智能体网络，负责协调多个专业智能体解决复杂的编程问题。"
            model = llmProvider

            // 添加专业化智能体
            agent(codeCompletionAgent)
            agent(codeExplanationAgent)
            agent(codeRefactoringAgent)
            agent(testGenerationAgent)

            // 使用上下文感知路由策略
            useContextAwareRouting()
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

    // 专业化智能体已在前面定义

    /**
     * 处理用户请求
     *
     * @param request 用户请求
     * @return 响应
     */
    suspend fun processRequest(request: String): String = withContext(Dispatchers.IO) {
        try {
            logger.info("处理用户请求: $request")

            // 存储到短期记忆
            shortTermMemory.storeMessage("user", request)

            // 获取上下文
            val context = contextEngine.getQueryContext(request, 10, 0.0, true)
            val contextContent = context.toString()

            // 创建消息
            val messages = listOf(
                LlmMessage(
                    role = LlmMessageRole.SYSTEM,
                    content = "你是一个代码智能体网络，负责协调多个专业智能体解决复杂的编程问题。"
                ),
                LlmMessage(
                    role = LlmMessageRole.USER,
                    content = "$request\n\n上下文信息：\n$contextContent"
                )
            )

            // 生成响应
            val options = AgentGenerateOptions(
                temperature = config.temperature,
                maxTokens = config.maxTokens
            )

            val response = agentNetwork.generate(messages, options).text

            // 存储到短期记忆
            shortTermMemory.storeMessage("assistant", response)

            return@withContext response
        } catch (e: Exception) {
            logger.error("处理用户请求时出错: $request", e)
            return@withContext "处理请求时出错: ${e.message}"
        }
    }

    /**
     * 分析任务类型
     *
     * @param request 用户请求
     * @return 任务类型
     */
    private suspend fun analyzeTaskType(request: String): TaskType {
        // 创建消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = """
                    你是一个任务分类器，需要将用户请求分类为以下任务类型之一：
                    1. CODE_GENERATION - 生成代码
                    2. CODE_EXPLANATION - 解释代码
                    3. CODE_REFACTORING - 重构代码
                    4. TEST_GENERATION - 生成测试
                    5. UNKNOWN - 未知任务

                    请只返回任务类型，不要有其他内容。
                """.trimIndent()
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = request
            )
        )

        // 生成响应
        val options = AgentGenerateOptions(
            temperature = 0.0,
            maxTokens = 10
        )

        val response = llmProvider.generate(messages, options).text.trim()

        // 解析任务类型
        return when {
            response.contains("CODE_GENERATION", ignoreCase = true) -> TaskType.CODE_GENERATION
            response.contains("CODE_EXPLANATION", ignoreCase = true) -> TaskType.CODE_EXPLANATION
            response.contains("CODE_REFACTORING", ignoreCase = true) -> TaskType.CODE_REFACTORING
            response.contains("TEST_GENERATION", ignoreCase = true) -> TaskType.TEST_GENERATION
            else -> TaskType.UNKNOWN
        }
    }

    /**
     * 路由请求
     *
     * @param request 用户请求
     * @param context 上下文
     * @return 响应
     */
    private suspend fun routeRequest(request: String, context: String): String {
        // 创建消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = "你是一个代码智能助手，可以回答各种编程相关的问题。"
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = "$request\n\n上下文信息：\n$context"
            )
        )

        // 生成响应
        val options = AgentGenerateOptions(
            temperature = 0.7,
            maxTokens = 1000
        )

        return llmProvider.generate(messages, options).text
    }

    /**
     * 检测语言
     *
     * @param request 用户请求
     * @return 语言
     */
    private suspend fun detectLanguage(request: String): String {
        // 创建消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = """
                    你是一个编程语言检测器，需要从用户请求中检测出用户想要使用的编程语言。
                    请从以下语言中选择一个：
                    kotlin, java, python, javascript, typescript, html, css, cpp, csharp, go, rust, php, ruby, swift

                    如果无法确定，请返回 kotlin。
                    请只返回语言名称，不要有其他内容。
                """.trimIndent()
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = request
            )
        )

        // 生成响应
        val options = AgentGenerateOptions(
            temperature = 0.0,
            maxTokens = 10
        )

        val response = llmProvider.generate(messages, options).text.trim().lowercase()

        // 验证语言
        val validLanguages = setOf(
            "kotlin", "java", "python", "javascript", "typescript",
            "html", "css", "cpp", "csharp", "go", "rust", "php", "ruby", "swift"
        )

        return if (response in validLanguages) response else "kotlin"
    }

    /**
     * 提取代码
     *
     * @param request 用户请求
     * @return 代码
     */
    private fun extractCode(request: String): String {
        // 提取代码块
        val codePattern = "```(?:\\w*)?\\s*([\\s\\S]*?)```".toRegex()
        val match = codePattern.find(request)

        return if (match != null) {
            match.groupValues[1].trim()
        } else {
            // 如果没有找到代码块，返回空字符串
            ""
        }
    }

    /**
     * 检测详细程度
     *
     * @param request 用户请求
     * @return 详细程度
     */
    private suspend fun detectDetailLevel(request: String): DetailLevel {
        // 创建消息
        val messages = listOf(
            LlmMessage(
                role = LlmMessageRole.SYSTEM,
                content = """
                    你是一个详细程度检测器，需要从用户请求中检测出用户想要的详细程度。
                    请从以下选项中选择一个：
                    BRIEF - 简要的解释
                    NORMAL - 正常详细程度的解释
                    DETAILED - 非常详细的解释

                    如果用户请求中包含“详细”、“详尽”、“detailed”等词，请选择 DETAILED。
                    如果用户请求中包含“简要”、“简单”、“brief”等词，请选择 BRIEF。
                    如果无法确定，请选择 NORMAL。

                    请只返回选项名称，不要有其他内容。
                """.trimIndent()
            ),
            LlmMessage(
                role = LlmMessageRole.USER,
                content = request
            )
        )

        // 生成响应
        val options = AgentGenerateOptions(
            temperature = 0.0,
            maxTokens = 10
        )

        val response = llmProvider.generate(messages, options).text.trim().uppercase()

        return when {
            response.contains("DETAILED") -> DetailLevel.DETAILED
            response.contains("BRIEF") -> DetailLevel.BRIEF
            else -> DetailLevel.NORMAL
        }
    }

    /**
     * 提取代码和指令
     *
     * @param request 用户请求
     * @return 代码和指令
     */
    private fun extractCodeAndInstructions(request: String): Pair<String, String> {
        // 提取代码块
        val code = extractCode(request)

        // 提取指令（去除代码块后的内容）
        val instructions = request.replace("```(?:\\w*)?\\s*[\\s\\S]*?```".toRegex(), "").trim()

        return Pair(code, instructions)
    }

    /**
     * 提取代码和框架
     *
     * @param request 用户请求
     * @return 代码和框架
     */
    private fun extractCodeAndFramework(request: String): Pair<String, String> {
        // 提取代码块
        val code = extractCode(request)

        // 检测测试框架
        val framework = when {
            request.contains("junit", ignoreCase = true) -> "JUnit"
            request.contains("testng", ignoreCase = true) -> "TestNG"
            request.contains("pytest", ignoreCase = true) -> "pytest"
            request.contains("jest", ignoreCase = true) -> "Jest"
            request.contains("mocha", ignoreCase = true) -> "Mocha"
            else -> "JUnit" // 默认为JUnit
        }

        return Pair(code, framework)
    }

    companion object {
        /**
         * 获取项目的智能体协调器实例
         *
         * @param project 项目
         * @return 智能体协调器实例
         */
        fun getInstance(project: Project): AgentCoordinator {
            return project.service<AgentCoordinator>()
        }
    }
}


