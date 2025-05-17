package ai.kastrax.code.service

import ai.kastrax.code.agent.AgentCoordinator
import ai.kastrax.code.agent.CodeAgent
import ai.kastrax.code.agent.CodeAgentConfig
import ai.kastrax.code.agent.KastraxCodeAgent
import ai.kastrax.code.agent.specialized.CodeCompletionAgent
import ai.kastrax.code.agent.specialized.CodeExplanationAgent
import ai.kastrax.code.agent.specialized.CodeRefactoringAgent
import ai.kastrax.code.agent.specialized.TestGenerationAgent
import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.context.CodeContextEngineImpl
import ai.kastrax.code.indexing.CodeIndexManager
import ai.kastrax.code.tools.CodeToolRegistry
import ai.kastrax.code.mock.Agent
import ai.kastrax.code.mock.AgentConfig
import ai.kastrax.code.common.KastraXCodeBase
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.diagnostic.Logger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import java.nio.file.Path
import java.nio.file.Paths

/**
 * 代码智能体服务
 *
 * 管理代码智能体和上下文引擎的生命周期
 */
@Service(Service.Level.PROJECT)
class CodeAgentService(private val project: Project) : KastraXCodeBase(component = "CODE_AGENT_SERVICE") {

    // 使用父类的logger
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var agent: Agent
    private lateinit var contextEngine: CodeContextEngine
    private lateinit var toolRegistry: CodeToolRegistry
    private lateinit var codeAgent: CodeAgent
    private lateinit var agentCoordinator: AgentCoordinator
    private lateinit var codeCompletionAgent: CodeCompletionAgent
    private lateinit var codeExplanationAgent: CodeExplanationAgent
    private lateinit var codeRefactoringAgent: CodeRefactoringAgent
    private lateinit var testGenerationAgent: TestGenerationAgent

    private var initialized = false

    /**
     * 初始化服务
     */
    fun initialize() {
        if (initialized) {
            return
        }

        logger.info("初始化代码智能体服务")

        // 初始化工具注册表
        toolRegistry = project.service<CodeToolRegistry>()
        toolRegistry.initializeDefaultTools()

        // 初始化上下文引擎
        contextEngine = CodeContextEngineImpl.getInstance(project)

        // 初始化智能体
        val agentConfig = AgentConfig(
            name = "kastrax-code-agent",
            description = "基于DeepSeek的代码智能体",
            model = "deepseek-coder",
            temperature = 0.3,
            maxTokens = 2000
        )
        agent = Agent("code-agent-1", agentConfig)

        // 初始化专业化智能体
        codeCompletionAgent = CodeCompletionAgent.getInstance(project)
        codeExplanationAgent = CodeExplanationAgent.getInstance(project)
        codeRefactoringAgent = CodeRefactoringAgent.getInstance(project)
        testGenerationAgent = TestGenerationAgent.getInstance(project)

        // 初始化智能体协调器
        agentCoordinator = AgentCoordinator.getInstance(project)

        // 初始化代码智能体（使用协调器作为默认实现）
        codeAgent = codeCompletionAgent

        // 启动代码索引管理器
        val indexManager = CodeIndexManager.getInstance(project)
        indexManager.start()

        initialized = true
    }

    /**
     * 获取代码智能体
     *
     * @return 代码智能体
     */
    fun getCodeAgent(): CodeAgent {
        if (!initialized) {
            initialize()
        }
        return codeAgent
    }

    /**
     * 获取上下文引擎
     *
     * @return 上下文引擎
     */
    fun getContextEngine(): CodeContextEngine {
        if (!initialized) {
            initialize()
        }
        return contextEngine
    }

    /**
     * 获取工具注册表
     *
     * @return 工具注册表
     */
    fun getToolRegistry(): CodeToolRegistry {
        if (!initialized) {
            initialize()
        }
        return toolRegistry
    }

    /**
     * 处理用户请求
     *
     * @param request 用户请求
     * @return 响应
     */
    suspend fun processRequest(request: String): String {
        return agentCoordinator.processRequest(request)
    }

    /**
     * 关闭服务
     */
    fun dispose() {
        if (initialized) {
            logger.info("关闭代码智能体服务")
            // 停止代码索引管理器
            val indexManager = CodeIndexManager.getInstance(project)
            indexManager.stop()
            serviceScope.cancel()
            initialized = false
        }
    }

    companion object {
        /**
         * 获取服务实例
         *
         * @param project 项目
         * @return 服务实例
         */
        fun getInstance(project: Project): CodeAgentService {
            return project.service<CodeAgentService>()
        }
    }
}
