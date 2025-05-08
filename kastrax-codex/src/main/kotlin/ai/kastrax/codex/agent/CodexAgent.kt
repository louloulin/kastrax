package ai.kastrax.codex.agent

import ai.kastrax.codex.tools.CodeAnalysisTool
import ai.kastrax.codex.tools.CodeGenerationTool
import ai.kastrax.codex.tools.GitTool
import ai.kastrax.codex.tools.SymbolLookupTool
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentState
import ai.kastrax.core.agent.AgentStatus
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.AgentStreamResponse
import ai.kastrax.core.agent.AgentToolCallResult
import ai.kastrax.core.agent.SessionInfo
import ai.kastrax.core.agent.SessionMessage
import ai.kastrax.core.agent.version.AgentVersion
import ai.kastrax.core.agent.version.AgentVersionManager
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.tools.Tool
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow

/**
 * CodexAgent 是 kastrax-codex 的核心 Agent 实现，它集成了 IDE 特定功能
 */
class CodexAgent(
    private val baseAgent: Agent,
    private val project: Project
) : Agent {
    private val logger = Logger.getInstance(CodexAgent::class.java)
    
    // 委托基础 Agent 的属性
    override val name: String = baseAgent.name
    override val versionManager: AgentVersionManager? = baseAgent.versionManager
    
    /**
     * 使用 IDE 上下文增强提示
     */
    private fun enhanceWithIDEContext(prompt: String): String {
        // 获取当前文件、项目结构等上下文
        val currentFile = getCurrentFile()
        val projectStructure = getProjectStructure()
        
        return """
            $prompt
            
            Current context:
            ${if (currentFile != null) "Current file: $currentFile" else ""}
            ${if (projectStructure.isNotEmpty()) "Project structure: $projectStructure" else ""}
        """.trimIndent()
    }
    
    /**
     * 获取当前文件内容
     */
    private fun getCurrentFile(): String? {
        // 使用 IntelliJ API 获取当前文件
        return try {
            // 实现获取当前文件的逻辑
            null // 暂时返回 null，后续实现
        } catch (e: Exception) {
            logger.warn("Failed to get current file", e)
            null
        }
    }
    
    /**
     * 获取项目结构
     */
    private fun getProjectStructure(): String {
        // 使用 IntelliJ API 获取项目结构
        return try {
            // 实现获取项目结构的逻辑
            "" // 暂时返回空字符串，后续实现
        } catch (e: Exception) {
            logger.warn("Failed to get project structure", e)
            ""
        }
    }
    
    /**
     * 生成响应，使用 IDE 上下文增强提示
     */
    override suspend fun generate(
        prompt: String,
        options: AgentGenerateOptions
    ): AgentResponse {
        val enhancedPrompt = enhanceWithIDEContext(prompt)
        return baseAgent.generate(enhancedPrompt, options)
    }
    
    /**
     * 生成响应，使用多个消息
     */
    override suspend fun generate(
        messages: List<LlmMessage>,
        options: AgentGenerateOptions
    ): AgentResponse {
        // 对于多消息生成，我们不增强上下文，直接委托给基础 Agent
        return baseAgent.generate(messages, options)
    }
    
    /**
     * 流式生成响应
     */
    override suspend fun stream(
        prompt: String,
        options: AgentStreamOptions
    ): AgentResponse {
        val enhancedPrompt = enhanceWithIDEContext(prompt)
        return baseAgent.stream(enhancedPrompt, options)
    }
    
    /**
     * 流式生成响应，返回 Flow
     */
    override suspend fun generateStream(
        prompt: String,
        options: AgentStreamOptions
    ): Flow<AgentStreamResponse> {
        val enhancedPrompt = enhanceWithIDEContext(prompt)
        return baseAgent.generateStream(enhancedPrompt, options)
    }
    
    /**
     * 处理工具调用结果
     */
    override suspend fun processToolCallResult(
        prompt: String,
        toolCallResult: AgentToolCallResult,
        options: AgentGenerateOptions
    ): AgentResponse {
        return baseAgent.processToolCallResult(prompt, toolCallResult, options)
    }
    
    /**
     * 重置 Agent 状态
     */
    override suspend fun reset() {
        baseAgent.reset()
    }
    
    /**
     * 获取 Agent 状态
     */
    override suspend fun getState(): AgentState? {
        return baseAgent.getState()
    }
    
    /**
     * 更新 Agent 状态
     */
    override suspend fun updateState(status: AgentStatus): AgentState? {
        return baseAgent.updateState(status)
    }
    
    /**
     * 创建会话
     */
    override suspend fun createSession(
        title: String?,
        resourceId: String?,
        metadata: Map<String, String>
    ): SessionInfo? {
        return baseAgent.createSession(title, resourceId, metadata)
    }
    
    /**
     * 获取会话信息
     */
    override suspend fun getSession(sessionId: String): SessionInfo? {
        return baseAgent.getSession(sessionId)
    }
    
    /**
     * 获取会话消息
     */
    override suspend fun getSessionMessages(sessionId: String, limit: Int): List<SessionMessage>? {
        return baseAgent.getSessionMessages(sessionId, limit)
    }
    
    /**
     * 创建 Agent 版本
     */
    override suspend fun createVersion(
        instructions: String,
        name: String?,
        description: String?,
        metadata: Map<String, String>,
        activateImmediately: Boolean
    ): AgentVersion? {
        return baseAgent.createVersion(instructions, name, description, metadata, activateImmediately)
    }
    
    /**
     * 获取 Agent 版本列表
     */
    override suspend fun getVersions(limit: Int, offset: Int): List<AgentVersion>? {
        return baseAgent.getVersions(limit, offset)
    }
    
    /**
     * 获取当前激活的 Agent 版本
     */
    override suspend fun getActiveVersion(): AgentVersion? {
        return baseAgent.getActiveVersion()
    }
    
    /**
     * 激活 Agent 版本
     */
    override suspend fun activateVersion(versionId: String): AgentVersion? {
        return baseAgent.activateVersion(versionId)
    }
    
    /**
     * 回滚到指定版本
     */
    override suspend fun rollbackToVersion(versionId: String): AgentVersion? {
        return baseAgent.rollbackToVersion(versionId)
    }
    
    companion object {
        /**
         * 创建代码补全专家 Agent
         */
        fun createCodeCompletionAgent(baseAgent: Agent, project: Project): CodexAgent {
            val agent = CodexAgent(baseAgent, project)
            // 可以在这里添加特定于代码补全的配置
            return agent
        }
        
        /**
         * 创建代码解释专家 Agent
         */
        fun createCodeExplanationAgent(baseAgent: Agent, project: Project): CodexAgent {
            val agent = CodexAgent(baseAgent, project)
            // 可以在这里添加特定于代码解释的配置
            return agent
        }
        
        /**
         * 创建 Git 专家 Agent
         */
        fun createGitAgent(baseAgent: Agent, project: Project): CodexAgent {
            val agent = CodexAgent(baseAgent, project)
            // 可以在这里添加特定于 Git 操作的配置
            return agent
        }
        
        /**
         * 获取 IDE 特定工具
         */
        fun getIDETools(project: Project): Map<String, Tool> {
            return mapOf(
                "code_analysis" to CodeAnalysisTool(project),
                "code_generation" to CodeGenerationTool(project),
                "git" to GitTool(project),
                "symbol_lookup" to SymbolLookupTool(project)
            )
        }
    }
}
