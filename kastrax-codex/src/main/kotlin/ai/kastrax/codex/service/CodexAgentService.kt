package ai.kastrax.codex.service

import ai.kastrax.codex.model.CodeContext
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.AgentResponse
import ai.kastrax.core.agent.AgentStreamOptions
import ai.kastrax.core.agent.architecture.AdaptiveAgent
import ai.kastrax.core.agent.architecture.CreativeAgent
import ai.kastrax.core.agent.architecture.HierarchicalAgent
import ai.kastrax.core.agent.architecture.ReflectiveAgent
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

/**
 * CodexAgentService接口，作为Codex与Kastrax Agent的桥梁
 */
interface CodexAgentService {
    /**
     * 创建并初始化编程智能体
     * 
     * @param config 智能体配置
     * @return 创建的智能体
     */
    suspend fun createProgrammingAgent(config: AgentConfig): Agent
    
    /**
     * 向智能体发送代码上下文
     * 
     * @param agent 智能体
     * @param context 代码上下文
     * @return 智能体响应
     */
    suspend fun sendCodeContext(agent: Agent, context: CodeContext): AgentResponse
    
    /**
     * 获取智能体响应
     * 
     * @param agent 智能体
     * @param prompt 提示
     * @return 智能体响应流
     */
    suspend fun getResponse(agent: Agent, prompt: String): Flow<AgentResponse>
    
    /**
     * 获取智能体状态
     * 
     * @param agent 智能体
     * @return 智能体状态
     */
    fun getAgentStatus(agent: Agent): AgentStatus
    
    /**
     * 终止智能体
     * 
     * @param agent 智能体
     */
    fun terminateAgent(agent: Agent)
}

/**
 * 智能体配置
 * 
 * @property name 智能体名称
 * @property type 智能体类型
 * @property instructions 智能体指令
 * @property apiKey API密钥
 * @property model 模型名称
 * @property temperature 温度
 * @property maxTokens 最大令牌数
 */
data class AgentConfig(
    val name: String,
    val type: AgentType = AgentType.ADAPTIVE,
    val instructions: String = "",
    val apiKey: String = "",
    val model: String = DeepSeekModel.DEEPSEEK_CHAT,
    val temperature: Double = 0.7,
    val maxTokens: Int = 2000
)

/**
 * 智能体类型
 */
enum class AgentType {
    BASIC,      // 基本智能体
    ADAPTIVE,   // 自适应智能体
    CREATIVE,   // 创造性智能体
    REFLECTIVE, // 反思型智能体
    HIERARCHICAL // 层次化智能体
}

/**
 * 智能体状态
 */
enum class AgentStatus {
    IDLE,       // 空闲
    BUSY,       // 忙碌
    ERROR,      // 错误
    STOPPED     // 已停止
}

/**
 * CodexAgentService实现类
 */
@Service(Service.Level.PROJECT)
class CodexAgentServiceImpl(private val project: Project) : CodexAgentService {
    
    private val logger = Logger.getInstance(CodexAgentServiceImpl::class.java)
    
    override suspend fun createProgrammingAgent(config: AgentConfig): Agent {
        logger.info("创建编程智能体: ${config.name}, 类型: ${config.type}")
        
        // 创建基础智能体
        val baseAgent = agent {
            name = config.name
            instructions = config.instructions
            
            // 配置DeepSeek模型
            model = deepSeek {
                apiKey(config.apiKey)
                model(config.model)
                temperature(config.temperature)
                maxTokens(config.maxTokens)
                timeout(60000) // 60秒超时
            }
            
            // 添加IDE相关工具
            tools {
                // TODO: 添加IDE特定工具
            }
            
            // 配置记忆系统
            memory(ai.kastrax.memory.impl.memory {
                storage(ai.kastrax.memory.impl.inMemoryStorage())
                lastMessages(10)
                semanticRecall(true)
            })
        }
        
        // 根据配置类型创建不同类型的智能体
        return when (config.type) {
            AgentType.BASIC -> baseAgent
            AgentType.ADAPTIVE -> AdaptiveAgent(baseAgent)
            AgentType.CREATIVE -> CreativeAgent(baseAgent)
            AgentType.REFLECTIVE -> ReflectiveAgent(baseAgent)
            AgentType.HIERARCHICAL -> {
                // 创建代码分析智能体
                val codeAnalysisAgent = agent {
                    name = "代码分析专家"
                    instructions = "你是一个代码分析专家，专注于理解代码结构和语义。"
                    model = baseAgent.model
                }
                
                // 创建代码生成智能体
                val codeGenerationAgent = agent {
                    name = "代码生成专家"
                    instructions = "你是一个代码生成专家，专注于生成高质量代码。"
                    model = baseAgent.model
                }
                
                // 创建测试生成智能体
                val testGenerationAgent = agent {
                    name = "测试生成专家"
                    instructions = "你是一个测试生成专家，专注于生成单元测试。"
                    model = baseAgent.model
                }
                
                // 创建层次化智能体
                HierarchicalAgent.create(
                    coordinator = baseAgent,
                    subAgents = mapOf(
                        "codeAnalysis" to codeAnalysisAgent,
                        "codeGeneration" to codeGenerationAgent,
                        "testGeneration" to testGenerationAgent
                    )
                )
            }
        }
    }
    
    override suspend fun sendCodeContext(agent: Agent, context: CodeContext): AgentResponse {
        logger.info("向智能体发送代码上下文: ${context.fileName}")
        
        // 构建提示
        val prompt = buildString {
            appendLine("请分析以下代码:")
            appendLine("文件名: ${context.fileName}")
            appendLine("语言: ${context.language}")
            appendLine("代码:")
            appendLine("```${context.language}")
            appendLine(context.code)
            appendLine("```")
            
            if (context.selection.isNotEmpty()) {
                appendLine("选中的代码:")
                appendLine("```${context.language}")
                appendLine(context.selection)
                appendLine("```")
            }
            
            if (context.task.isNotEmpty()) {
                appendLine("任务: ${context.task}")
            }
        }
        
        // 生成响应
        return agent.generate(prompt, AgentGenerateOptions(maxSteps = 3))
    }
    
    override suspend fun getResponse(agent: Agent, prompt: String): Flow<AgentResponse> = flow {
        logger.info("获取智能体响应: $prompt")
        
        try {
            // 生成响应
            val response = agent.generate(prompt, AgentGenerateOptions(maxSteps = 3))
            emit(response)
        } catch (e: Exception) {
            logger.error("获取智能体响应失败", e)
            throw e
        }
    }
    
    override fun getAgentStatus(agent: Agent): AgentStatus {
        // 简单实现，实际应用中可能需要更复杂的状态管理
        return AgentStatus.IDLE
    }
    
    override fun terminateAgent(agent: Agent) {
        logger.info("终止智能体: ${agent.name}")
        // 实际应用中可能需要清理资源
    }
}
