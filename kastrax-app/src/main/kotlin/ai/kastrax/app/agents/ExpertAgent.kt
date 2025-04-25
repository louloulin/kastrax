package ai.kastrax.app.agents

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.app.constants.AgentIds
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.serialization.json.JsonElement

private val logger = KotlinLogging.logger {}

/**
 * 专家代理。
 * 这是一个专业领域的专家代理，可以提供深入的专业知识。
 */
val expertAgent = agent {
    // 设置名称
    name = "专家代理"
    
    // 设置指令（系统提示）
    instructions = """
        你是一个人工智能和机器学习领域的专家。你对深度学习、神经网络、自然语言处理和计算机视觉等领域有深入的了解。

        当用户询问这些领域的问题时，提供详细、准确和最新的信息。使用专业术语，但也要确保解释清楚，使非专业人士也能理解。

        如果你不确定某个问题的答案，坦诚地承认，而不是提供可能不准确的信息。
    """.trimIndent()

    // 设置模型
    model = openAi("gpt-4")
    
    // 配置默认生成选项
    defaultGenerateOptions {
        temperature(0.3) // 较低的温度，更加精确
        maxTokens(2000) // 更长的回复
    }
    
    // 注意：示例对话需要通过其他方式设置，当前Agent接口不直接支持examples DSL
    // 可以通过LlmMessage列表在generate方法中提供示例
}
