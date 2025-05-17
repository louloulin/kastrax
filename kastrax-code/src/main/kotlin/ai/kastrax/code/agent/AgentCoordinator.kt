package ai.kastrax.code.agent

import ai.kastrax.code.agent.specialized.CodeCompletionAgent
import ai.kastrax.code.agent.specialized.CodeExplanationAgent
import ai.kastrax.code.agent.specialized.CodeRefactoringAgent
import ai.kastrax.code.agent.specialized.DetailLevelDetectorAgent
import ai.kastrax.code.agent.specialized.LanguageDetectorAgent
import ai.kastrax.code.agent.specialized.QueryResponseAgent
import ai.kastrax.code.agent.specialized.TaskClassifierAgent
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
    private val project: Project
) : KastraXCodeBase(component = "AGENT_COORDINATOR") {
    // 使用默认配置
    private val config: AgentCoordinatorConfig = AgentCoordinatorConfig()

    // 使用父类的logger

    // DeepSeek提供者 - 仅用于智能体网络
    private val llmProvider: LlmProvider by lazy {
        deepSeek {
            model(DeepSeekModel.DEEPSEEK_CODER)
            apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "")
            temperature(0.3)
            maxTokens(2000)
        }
    }

    // 任务分类智能体
    private val taskClassifierAgent: TaskClassifierAgent by lazy {
        TaskClassifierAgent.getInstance(project)
    }

    // 语言检测智能体
    private val languageDetectorAgent: LanguageDetectorAgent by lazy {
        LanguageDetectorAgent.getInstance(project)
    }

    // 详细程度检测智能体
    private val detailLevelDetectorAgent: DetailLevelDetectorAgent by lazy {
        DetailLevelDetectorAgent.getInstance(project)
    }

    // 查询响应智能体
    private val queryResponseAgent: QueryResponseAgent by lazy {
        QueryResponseAgent.getInstance(project)
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
        return taskClassifierAgent.analyzeTaskType(request)
    }

    /**
     * 路由请求
     *
     * @param request 用户请求
     * @param context 上下文
     * @return 响应
     */
    private suspend fun routeRequest(request: String, context: String): String {
        return queryResponseAgent.answerQuery(request, context)
    }

    /**
     * 检测语言
     *
     * @param request 用户请求
     * @return 语言
     */
    private suspend fun detectLanguage(request: String): String {
        return languageDetectorAgent.detectLanguage(request)
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
        return detailLevelDetectorAgent.detectDetailLevel(request)
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
