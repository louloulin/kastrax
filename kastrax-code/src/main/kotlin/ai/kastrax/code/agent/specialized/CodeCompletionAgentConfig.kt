package ai.kastrax.code.agent.specialized

/**
 * 代码补全智能体配置
 */
data class CodeCompletionAgentConfig(
    /**
     * 模型名称
     */
    val model: String = "deepseek-coder",
    
    /**
     * 温度
     */
    val temperature: Double = 0.2,
    
    /**
     * 最大令牌数
     */
    val maxTokens: Int = 1000,
    
    /**
     * 是否启用调试模式
     */
    val debug: Boolean = false,
    
    /**
     * 是否启用上下文
     */
    val enableContext: Boolean = true,
    
    /**
     * 是否启用记忆
     */
    val enableMemory: Boolean = true,
    
    /**
     * 是否启用工具
     */
    val enableTools: Boolean = true,
    
    /**
     * 最大补全数量
     */
    val maxCompletions: Int = 5
)
