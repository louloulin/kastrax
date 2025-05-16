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
import ai.kastrax.code.mock.Agent
import ai.kastrax.code.mock.AgentConfig
import ai.kastrax.code.mock.AgentContext
import ai.kastrax.code.mock.DeepSeekProvider
import ai.kastrax.code.mock.DeepSeekModel
import ai.kastrax.code.mock.deepSeek
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

    // 底层智能体
    private val routerAgent: Agent by lazy {
        Agent(
            id = "agent-router",
            config = AgentConfig(
                name = "智能体路由器",
                description = "根据用户请求路由到合适的智能体",
                model = config.model,
                temperature = config.temperature,
                maxTokens = config.maxTokens
            )
        )
    }

    // DeepSeek提供者
    private val llmProvider by lazy {
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

    // 专业化智能体
    private val codeCompletionAgent: CodeCompletionAgent by lazy {
        CodeCompletionAgent.getInstance(project)
    }

    private val codeExplanationAgent: CodeExplanationAgent by lazy {
        CodeExplanationAgent.getInstance(project)
    }

    private val codeRefactoringAgent: CodeRefactoringAgent by lazy {
        CodeRefactoringAgent.getInstance(project)
    }

    private val testGenerationAgent: TestGenerationAgent by lazy {
        TestGenerationAgent.getInstance(project)
    }

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

            // 分析任务类型
            val taskType = analyzeTaskType(request)

            // 根据任务类型路由到合适的智能体
            val response = when (taskType) {
                TaskType.CODE_GENERATION -> {
                    // 检测语言
                    val language = detectLanguage(request)
                    codeCompletionAgent.generateCode(request, language)
                }
                TaskType.CODE_EXPLANATION -> {
                    // 提取代码
                    val code = extractCode(request)
                    val detailLevel = detectDetailLevel(request)
                    codeExplanationAgent.explainCode(code, detailLevel)
                }
                TaskType.CODE_REFACTORING -> {
                    // 提取代码和指令
                    val (code, instructions) = extractCodeAndInstructions(request)
                    codeRefactoringAgent.refactorCode(code, instructions)
                }
                TaskType.TEST_GENERATION -> {
                    // 提取代码和框架
                    val (code, framework) = extractCodeAndFramework(request)
                    testGenerationAgent.generateTest(code, framework)
                }
                TaskType.UNKNOWN -> {
                    // 使用路由智能体处理未知任务
                    routeRequest(request, context.getContent())
                }
            }

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
        // 创建提示
        val prompt = """
            你是一个任务分类器，需要将用户请求分类为以下任务类型之一：
            1. CODE_GENERATION - 生成代码
            2. CODE_EXPLANATION - 解释代码
            3. CODE_REFACTORING - 重构代码
            4. TEST_GENERATION - 生成测试
            5. UNKNOWN - 未知任务

            请分析以下用户请求，并返回对应的任务类型（仅返回任务类型，不要有其他内容）：

            $request
        """.trimIndent()

        // 调用LLM
        val llmRequest = LLMRequest(
            model = config.model,
            prompt = prompt,
            temperature = 0.0,
            maxTokens = 10
        )

        val llmResponse = llmProvider.complete(llmRequest)

        // 解析任务类型
        return when {
            llmResponse.content.contains("CODE_GENERATION") -> TaskType.CODE_GENERATION
            llmResponse.content.contains("CODE_EXPLANATION") -> TaskType.CODE_EXPLANATION
            llmResponse.content.contains("CODE_REFACTORING") -> TaskType.CODE_REFACTORING
            llmResponse.content.contains("TEST_GENERATION") -> TaskType.TEST_GENERATION
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
        // 创建代理上下文
        val agentContext = AgentContext(
            input = request,
            metadata = mapOf(
                "context" to context
            )
        )

        // 调用代理
        val response = routerAgent.process(agentContext)

        return response.output
    }

    /**
     * 检测语言
     *
     * @param request 用户请求
     * @return 语言
     */
    private fun detectLanguage(request: String): String {
        // 检测请求中提到的语言
        return when {
            request.contains("kotlin", ignoreCase = true) -> "kotlin"
            request.contains("java", ignoreCase = true) -> "java"
            request.contains("python", ignoreCase = true) -> "python"
            request.contains("javascript", ignoreCase = true) || request.contains("js", ignoreCase = true) -> "javascript"
            request.contains("typescript", ignoreCase = true) || request.contains("ts", ignoreCase = true) -> "typescript"
            request.contains("html", ignoreCase = true) -> "html"
            request.contains("css", ignoreCase = true) -> "css"
            else -> "kotlin" // 默认为Kotlin
        }
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
    private fun detectDetailLevel(request: String): DetailLevel {
        return when {
            request.contains("详细", ignoreCase = true) ||
            request.contains("详尽", ignoreCase = true) ||
            request.contains("详细解释", ignoreCase = true) ||
            request.contains("detailed", ignoreCase = true) -> DetailLevel.DETAILED

            request.contains("简要", ignoreCase = true) ||
            request.contains("简单", ignoreCase = true) ||
            request.contains("简短", ignoreCase = true) ||
            request.contains("brief", ignoreCase = true) -> DetailLevel.BRIEF

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

/**
 * 智能体协调器配置
 *
 * @property model 模型
 * @property temperature 温度
 * @property maxTokens 最大令牌数
 */
data class AgentCoordinatorConfig(
    val model: String = "deepseek-coder",
    val temperature: Double = 0.2,
    val maxTokens: Int = 1000
)
