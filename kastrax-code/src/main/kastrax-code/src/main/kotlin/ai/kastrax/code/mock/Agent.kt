package ai.kastrax.code.mock

/**
 * 智能体
 *
 * @property id 标识符
 * @property config 配置
 */
class Agent(
    val id: String,
    val config: AgentConfig
) {
    /**
     * 智能体名称
     */
    val name: String = config.name
    /**
     * 处理请求
     *
     * @param context 上下文
     * @return 响应
     */
    fun process(context: AgentContext): AgentResponse {
        // 模拟响应
        return AgentResponse(
            output = "这是一个模拟的智能体响应"
        )
    }
}

/**
 * 智能体配置
 *
 * @property name 名称
 * @property description 描述
 * @property model 模型
 * @property temperature 温度
 * @property maxTokens 最大令牌数
 */
data class AgentConfig(
    val name: String,
    val description: String,
    val model: String,
    val temperature: Double = 0.7,
    val maxTokens: Int = 1000
)

/**
 * 智能体上下文
 *
 * @property input 输入
 * @property metadata 元数据
 */
data class AgentContext(
    val input: String,
    val metadata: Map<String, Any> = emptyMap()
)

/**
 * 智能体响应
 *
 * @property output 输出
 */
data class AgentResponse(
    val output: String
)
