package ai.kastrax.examples.agent

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.architecture.AdaptiveAgent
import ai.kastrax.core.agent.architecture.AdaptiveAgentConfig
import ai.kastrax.core.agent.architecture.UserPreference
import ai.kastrax.core.agent.basic.BasicAgent
import ai.kastrax.core.llm.deepseek.DeepseekLlm
import kotlinx.coroutines.runBlocking

/**
 * 自适应Agent示例
 */
fun main() = runBlocking {
    // 创建基础LLM
    val llm = DeepseekLlm(
        apiKey = System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key",
        model = "deepseek-chat"
    )

    // 使用DSL创建基础Agent
    val baseAgent = agent {
        name = "基础Agent"
        model = llm
    }

    // 使用DSL创建自适应Agent
    val adaptiveAgent = adaptiveAgent {
        baseAgent(baseAgent)
        config {
            enableAutoLearning(true)
            maxInteractionHistory(100)
        }
    }

    // 设置用户偏好
    val userId = "user-123"
    adaptiveAgent.setUserPreference(
        userId = userId,
        preference = UserPreference(
            communicationStyle = "友好",
            detailLevel = "详细",
            topics = listOf("技术", "编程", "AI"),
            avoidTopics = listOf("政治")
        )
    )

    // 使用自适应Agent生成响应
    val options = AgentGenerateOptions()
    val optionsWithMetadata = options.copy(metadata = mapOf("userId" to userId))
    val response = adaptiveAgent.generate(
        prompt = "请介绍一下Kotlin语言的特点",
        options = optionsWithMetadata
    )

    println("自适应Agent响应:")
    println(response.text)

    // 提供反馈
    // 注意：在实际应用中，交互ID应该从响应中获取
    val interactionId = "interaction-123" // 这里只是示例
    adaptiveAgent.provideFeedback(
        interactionId = interactionId,
        rating = 5,
        feedback = "非常详细的介绍，正是我想要的！",
        userId = userId
    )
}
