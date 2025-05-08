package ai.kastrax.codex.service

import ai.kastrax.codex.agent.CodexAgent
import ai.kastrax.codex.agent.CodexAgentFactory
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.llm.LlmMessage
import ai.kastrax.core.llm.LlmMessageRole
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * CodexService 是 IntelliJ 插件的服务组件，用于管理 CodexAgent 的生命周期
 */
@Service
class CodexService(private val project: Project) {
    private val logger = Logger.getInstance(CodexService::class.java)
    private val coroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    
    // Agent 缓存
    private val agentCache = ConcurrentHashMap<AgentType, CodexAgent>()
    
    /**
     * 获取代码补全 Agent
     */
    fun getCodeCompletionAgent(): CodexAgent {
        return agentCache.computeIfAbsent(AgentType.CODE_COMPLETION) {
            CodexAgentFactory.getInstance().createCodeCompletionAgent(project)
        }
    }
    
    /**
     * 获取代码解释 Agent
     */
    fun getCodeExplanationAgent(): CodexAgent {
        return agentCache.computeIfAbsent(AgentType.CODE_EXPLANATION) {
            CodexAgentFactory.getInstance().createCodeExplanationAgent(project)
        }
    }
    
    /**
     * 获取 Git Agent
     */
    fun getGitAgent(): CodexAgent {
        return agentCache.computeIfAbsent(AgentType.GIT) {
            CodexAgentFactory.getInstance().createGitAgent(project)
        }
    }
    
    /**
     * 生成代码补全
     */
    suspend fun generateCodeCompletion(prompt: String): AgentResponse {
        return withContext(Dispatchers.IO) {
            try {
                val agent = getCodeCompletionAgent()
                agent.generate(prompt)
            } catch (e: Exception) {
                logger.error("Error generating code completion", e)
                AgentResponse(text = "Error: ${e.message}")
            }
        }
    }
    
    /**
     * 生成代码解释
     */
    suspend fun generateCodeExplanation(code: String, language: String): AgentResponse {
        return withContext(Dispatchers.IO) {
            try {
                val agent = getCodeExplanationAgent()
                val prompt = """
                    请解释以下 $language 代码的功能和工作原理：
                    
                    ```$language
                    $code
                    ```
                """.trimIndent()
                
                agent.generate(prompt)
            } catch (e: Exception) {
                logger.error("Error generating code explanation", e)
                AgentResponse(text = "Error: ${e.message}")
            }
        }
    }
    
    /**
     * 生成提交消息
     */
    suspend fun generateCommitMessage(diff: String): AgentResponse {
        return withContext(Dispatchers.IO) {
            try {
                val agent = getGitAgent()
                val prompt = """
                    请根据以下代码变更生成一个符合约定式提交规范的提交消息：
                    
                    ```diff
                    $diff
                    ```
                """.trimIndent()
                
                agent.generate(prompt)
            } catch (e: Exception) {
                logger.error("Error generating commit message", e)
                AgentResponse(text = "Error: ${e.message}")
            }
        }
    }
    
    /**
     * 流式生成响应
     */
    suspend fun streamGenerate(prompt: String, agentType: AgentType, callback: (String) -> Unit) {
        coroutineScope.launch {
            try {
                val agent = when (agentType) {
                    AgentType.CODE_COMPLETION -> getCodeCompletionAgent()
                    AgentType.CODE_EXPLANATION -> getCodeExplanationAgent()
                    AgentType.GIT -> getGitAgent()
                }
                
                val options = AgentStreamOptions(
                    onFinish = { fullText ->
                        callback(fullText)
                    }
                )
                
                val response = agent.stream(prompt, options)
                response.textStream?.collect { chunk ->
                    callback(chunk)
                }
            } catch (e: Exception) {
                logger.error("Error in stream generation", e)
                callback("Error: ${e.message}")
            }
        }
    }
    
    /**
     * 重置所有 Agent
     */
    fun resetAllAgents() {
        coroutineScope.launch {
            agentCache.values.forEach { agent ->
                try {
                    agent.reset()
                } catch (e: Exception) {
                    logger.error("Error resetting agent", e)
                }
            }
        }
    }
    
    companion object {
        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): CodexService {
            return project.getService(CodexService::class.java)
        }
    }
}

/**
 * Agent 类型枚举
 */
enum class AgentType {
    CODE_COMPLETION,
    CODE_EXPLANATION,
    GIT
}
