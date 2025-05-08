package ai.kastrax.codex.agent

import ai.kastrax.codex.adapter.LlmProviderAdapter
import ai.kastrax.codex.memory.CodexMemoryManager
import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.agent
import ai.kastrax.core.llm.LlmProvider
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import ee.carlrobert.codegpt.settings.GeneralSettings
import ee.carlrobert.codegpt.settings.service.ServiceType

/**
 * CodexAgentFactory 负责创建和管理 CodexAgent 实例
 */
class CodexAgentFactory {
    private val logger = Logger.getInstance(CodexAgentFactory::class.java)
    
    /**
     * 创建代码补全专家 Agent
     */
    fun createCodeCompletionAgent(project: Project): CodexAgent {
        val baseAgent = createBaseAgent(
            name = "代码补全专家",
            instructions = """
                你是一个代码补全专家，专注于提供高质量、符合上下文的代码建议。
                分析当前代码上下文，理解编程语言的语法和惯用法，并提供最相关的补全建议。
                确保生成的代码遵循项目的编码风格和最佳实践。
                
                当用户请求代码补全时：
                1. 分析提供的代码上下文和需求
                2. 考虑编程语言的特性和最佳实践
                3. 生成符合项目风格的代码
                4. 提供简洁的解释说明代码的功能和原理
            """.trimIndent(),
            project = project
        )
        
        return CodexAgent.createCodeCompletionAgent(baseAgent, project)
    }
    
    /**
     * 创建代码解释专家 Agent
     */
    fun createCodeExplanationAgent(project: Project): CodexAgent {
        val baseAgent = createBaseAgent(
            name = "代码解释专家",
            instructions = """
                你是一个代码解释专家，专注于清晰解释代码的功能和逻辑。
                分析给定代码，识别关键组件和算法，并提供易于理解的解释。
                根据用户的技术水平调整解释的详细程度。
                
                当用户请求解释代码时：
                1. 分析代码的结构和功能
                2. 识别关键算法和设计模式
                3. 解释代码的目的和工作原理
                4. 指出潜在的优化点或问题
            """.trimIndent(),
            project = project
        )
        
        return CodexAgent.createCodeExplanationAgent(baseAgent, project)
    }
    
    /**
     * 创建 Git 专家 Agent
     */
    fun createGitAgent(project: Project): CodexAgent {
        val baseAgent = createBaseAgent(
            name = "Git 专家",
            instructions = """
                你是一个 Git 操作专家，专注于帮助用户管理代码版本控制。
                分析代码变更，生成有意义的提交消息，并提供 Git 相关建议。
                
                当用户请求 Git 相关帮助时：
                1. 分析代码变更的内容和范围
                2. 生成符合约定式提交规范的提交消息
                3. 提供关于分支策略和合并的建议
                4. 帮助解决常见的 Git 问题
            """.trimIndent(),
            project = project
        )
        
        return CodexAgent.createGitAgent(baseAgent, project)
    }
    
    /**
     * 创建基础 Agent
     */
    private fun createBaseAgent(name: String, instructions: String, project: Project): Agent {
        // 获取当前选择的 LLM 服务
        val serviceType = GeneralSettings.getSelectedService()
        
        // 创建适配的 LlmProvider
        val llmProvider = createLlmProvider(serviceType)
        
        // 创建内存管理器
        val memoryManager = CodexMemoryManager(project.name)
        
        // 创建 IDE 工具
        val ideTools = CodexAgent.getIDETools(project)
        
        // 创建基础 Agent
        return agent {
            this.name = name
            this.instructions = instructions
            this.model = llmProvider
            
            // 添加 IDE 工具
            tools {
                ideTools.forEach { (id, tool) ->
                    tool(id, tool)
                }
            }
            
            // 配置内存
            memory(memoryManager.getConversationMemory())
            
            // 配置默认生成选项
            defaultGenerateOptions {
                temperature(0.7)
                maxTokens(2000)
                maxSteps(3) // 允许多步工具调用
                executeTools(true)
            }
        }
    }
    
    /**
     * 创建 LlmProvider 适配器
     */
    private fun createLlmProvider(serviceType: ServiceType): LlmProvider {
        val modelName = when (serviceType) {
            ServiceType.OPENAI -> "gpt-4"
            ServiceType.ANTHROPIC -> "claude-3-opus-20240229"
            ServiceType.GOOGLE -> "gemini-pro"
            ServiceType.AZURE -> "gpt-4"
            ServiceType.OLLAMA -> "llama3"
            ServiceType.LLAMA_CPP -> "llama3"
            ServiceType.CODEGPT -> "codegpt-api"
            ServiceType.CUSTOM_OPENAI -> "custom-model"
        }
        
        return LlmProviderAdapter(modelName)
    }
    
    companion object {
        // 单例实例
        private val INSTANCE = CodexAgentFactory()
        
        // 获取单例实例
        fun getInstance(): CodexAgentFactory = INSTANCE
    }
}
