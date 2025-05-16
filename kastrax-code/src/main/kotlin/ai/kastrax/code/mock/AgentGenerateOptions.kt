package ai.kastrax.code.mock

/**
 * 智能体生成选项
 *
 * @property temperature 温度
 * @property maxTokens 最大令牌数
 */
data class AgentGenerateOptions(
    val temperature: Double = 0.7,
    val maxTokens: Int = 1000
)

/**
 * 智能体生成响应
 *
 * @property text 文本
 */
data class AgentGenerateResponse(
    val text: String
)

/**
 * 智能体扩展函数
 *
 * @param prompt 提示文本
 * @param options 选项
 * @return 响应
 */
fun Agent.generate(prompt: String, options: AgentGenerateOptions): AgentGenerateResponse {
    // 模拟生成响应
    return AgentGenerateResponse(
        text = "这是一个模拟的智能体生成响应"
    )
}
