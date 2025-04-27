package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.architecture.adaptiveAgent
import ai.kastrax.core.agent.architecture.goalOrientedAgent
import ai.kastrax.core.agent.architecture.hierarchicalAgent
import ai.kastrax.core.agent.architecture.reflectiveAgent
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.DeepSeekModel
import kotlinx.coroutines.runBlocking

/**
 * Deepseek 架构示例
 *
 * 展示如何使用 Deepseek 作为 LLM 提供商创建不同架构的 Agent
 */
fun deepseekArchitectureExample() = runBlocking {
    // 使用 DSL 创建 Deepseek LLM 提供商
    val llm = deepSeek {
        // 设置 API 密钥（也可以从环境变量 DEEPSEEK_API_KEY 获取）
        apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "your-api-key")

        // 设置模型
        model(DeepSeekModel.DEEPSEEK_CHAT)

        // 设置生成参数
        temperature(0.7)
        maxTokens(2000)
        topP(0.95)

        // 设置超时时间（秒）
        timeout(60)
    }

    // 创建基础 Agent
    val baseAgent = agent {
        name = "基础Agent"
        model = llm
        instructions = "你是一个由 Deepseek 驱动的智能助手，专注于提供准确、有用的信息。"
    }

    // 创建分层 Agent
    val techAgent = agent {
        name = "技术Agent"
        model = llm
        instructions = "你是一个技术专家，专注于回答技术相关的问题。"
    }

    val creativeAgent = agent {
        name = "创意Agent"
        model = llm
        instructions = "你是一个创意专家，专注于提供创意和设计相关的建议。"
    }

    val hierarchicalAgent = hierarchicalAgent {
        coordinator(baseAgent)
        addSubAgent("tech", techAgent)
        addSubAgent("creative", creativeAgent)
    }

    // 创建自适应 Agent
    val adaptiveAgent = adaptiveAgent {
        baseAgent(baseAgent)
        config {
            enableAutoLearning(true)
            maxInteractionHistory(100)
        }
    }

    // 创建目标导向 Agent
    val goalOrientedAgent = goalOrientedAgent {
        baseAgent(baseAgent)
        config {
            enableAutoGoalExtraction(true)
            enableAutoTaskCreation(true)
        }
    }

    // 创建反思型 Agent
    val reflectiveAgent = reflectiveAgent {
        baseAgent(baseAgent)
        config {
            enablePreReflection(true)
            enableResponseReflection(true)
            enablePostReflection(true)
        }
    }

    println("=== Deepseek 架构示例 ===")

    // 准备提问
    val question = "请解释量子计算的基本原理，并提供一些潜在的应用场景。"
    println("\n问题: $question")

    // 创建生成选项
    val options = AgentGenerateOptions()
    val sessionId = "test-session"
    val userId = "test-user"
    val optionsWithMetadata = options.copy(
        metadata = mapOf(
            "sessionId" to sessionId,
            "userId" to userId
        )
    )

    // 使用不同架构的 Agent 生成回答
    println("\n=== 分层 Agent 回答 ===")
    val hierarchicalResponse = hierarchicalAgent.generate(question, optionsWithMetadata)
    println(hierarchicalResponse.text)

    println("\n=== 自适应 Agent 回答 ===")
    val adaptiveResponse = adaptiveAgent.generate(question, optionsWithMetadata)
    println(adaptiveResponse.text)

    println("\n=== 目标导向 Agent 回答 ===")
    val goalOrientedResponse = goalOrientedAgent.generate(question, optionsWithMetadata)
    println(goalOrientedResponse.text)

    println("\n=== 反思型 Agent 回答 ===")
    val reflectiveResponse = reflectiveAgent.generate(question, optionsWithMetadata)
    println(reflectiveResponse.text)

    // 查看反思型 Agent 的反思
    val reflections = reflectiveAgent.getSessionReflections(sessionId)
    println("\n=== 反思型 Agent 的反思 ===")
    println("共有 ${reflections.size} 条反思")

    if (reflections.isNotEmpty()) {
        val sample = reflections.take(2)
        sample.forEachIndexed { index, reflection ->
            println("\n反思 ${index + 1}:")
            println("类型: ${reflection.type}")
            println("内容: ${reflection.content}")
        }
    }
}
