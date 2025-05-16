package ai.kastrax.code.service

import ai.kastrax.code.agent.CodeAgent
import ai.kastrax.code.agent.CodeAgentConfig
import ai.kastrax.code.agent.KastraxCodeAgent
import ai.kastrax.code.context.CodeContextEngine
import ai.kastrax.code.context.CodeContextEngineConfig
import ai.kastrax.code.context.KastraxCodeContextEngine
import ai.kastrax.code.tools.CodeToolRegistry
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentConfig
import ai.kastrax.core.agent.AgentFactory
import ai.kastrax.core.common.KastraXBase
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import io.github.oshai.kotlinlogging.KotlinLogging
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
class CodeAgentService(private val project: Project) : KastraXBase(component = "CODE_AGENT_SERVICE", name = "kastrax-code-agent-service") {
    
    private val logger = KotlinLogging.logger {}
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    private lateinit var agent: Agent
    private lateinit var contextEngine: CodeContextEngine
    private lateinit var toolRegistry: CodeToolRegistry
    private lateinit var codeAgent: CodeAgent
    
    private var initialized = false
    
    /**
     * 初始化服务
     */
    fun initialize() {
        if (initialized) {
            return
        }
        
        logger.info { "初始化代码智能体服务" }
        
        // 初始化工具注册表
        toolRegistry = project.service<CodeToolRegistry>()
        
        // 初始化上下文引擎
        contextEngine = KastraxCodeContextEngine(CodeContextEngineConfig())
        
        // 初始化智能体
        val agentConfig = AgentConfig(
            name = "kastrax-code-agent",
            model = "deepseek-coder",
            temperature = 0.3,
            maxTokens = 2000
        )
        agent = AgentFactory.createAgent(agentConfig)
        
        // 初始化代码智能体
        codeAgent = KastraxCodeAgent(
            agent = agent,
            contextEngine = contextEngine,
            toolRegistry = toolRegistry,
            config = CodeAgentConfig()
        )
        
        // 索引项目代码
        serviceScope.launch {
            indexProject()
        }
        
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
     * 索引项目代码
     */
    private suspend fun indexProject() {
        try {
            val projectPath = project.basePath
            if (projectPath != null) {
                val path = Paths.get(projectPath)
                logger.info { "索引项目代码: $path" }
                contextEngine.indexCodebase(path)
            }
        } catch (e: Exception) {
            logger.error(e) { "索引项目代码失败" }
        }
    }
    
    /**
     * 关闭服务
     */
    fun dispose() {
        if (initialized) {
            logger.info { "关闭代码智能体服务" }
            serviceScope.launch {
                contextEngine.close()
            }
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
