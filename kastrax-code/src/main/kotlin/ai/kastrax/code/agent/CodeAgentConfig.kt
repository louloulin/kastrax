package ai.kastrax.code.agent

/**
 * 代码智能体配置
 */
data class CodeAgentConfig(
    // 代码生成相关配置
    val codeGenerationTemperature: Double = 0.2,
    val codeGenerationMaxTokens: Int = 2000,
    
    // 代码解释相关配置
    val codeExplanationTemperature: Double = 0.7,
    val codeExplanationMaxTokens: Int = 2000,
    
    // 代码重构相关配置
    val codeRefactoringTemperature: Double = 0.3,
    val codeRefactoringMaxTokens: Int = 2000,
    
    // 测试生成相关配置
    val testGenerationTemperature: Double = 0.3,
    val testGenerationMaxTokens: Int = 2000,
    
    // 代码补全相关配置
    val codeCompletionTemperature: Double = 0.2,
    val codeCompletionMaxTokens: Int = 1000
)
