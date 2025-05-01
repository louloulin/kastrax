package ai.kastrax.examples.agent

import ai.kastrax.core.agent.Agent
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.architecture.adaptiveAgent
import ai.kastrax.core.agent.architecture.UserPreference
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.DeepSeekModel
import kotlinx.coroutines.runBlocking

/**
 * 自适应Agent示例
 */
fun main(args: Array<String>) = runBlocking {
    // 创建基础LLM
    val llm = deepSeek {
        // 直接设置 API 密钥
        apiKey("sk-85e83081df28490b9ae63188f0cb4f79")

        // 设置模型
        model(DeepSeekModel.DEEPSEEK_CHAT)

        // 设置生成参数
        temperature(0.7)
        maxTokens(2000)
        topP(0.95)
    }

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
