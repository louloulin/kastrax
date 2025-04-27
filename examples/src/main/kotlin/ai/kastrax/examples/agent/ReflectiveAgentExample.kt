package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.core.agent.architecture.ReflectiveAgent
import ai.kastrax.core.agent.architecture.reflectiveAgent
import ai.kastrax.core.llm.deepseek.DeepseekLlm
import kotlinx.coroutines.runBlocking

/**
 * 反思型Agent示例
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

    // 使用DSL创建反思型Agent
    val reflectiveAgent = reflectiveAgent {
        baseAgent(baseAgent)
        config {
            enablePreReflection(true)
            enableResponseReflection(true)
            enablePostReflection(true)
            enableLearningFromReflection(true)
        }
    }

    // 注意：在新的实现中，没有addLesson方法
    // 反思型Agent会自动从交互中学习

    println("=== 反思型Agent示例 ===")

    // 创建会话
    val sessionId = "session-123"

    // 第一个问题
    val prompt1 = "什么是大语言模型？它们是如何工作的？"
    println("\n问题1: $prompt1")

    val options1 = AgentGenerateOptions()
    val optionsWithMetadata1 = options1.copy(metadata = mapOf("sessionId" to sessionId))
    val response1 = reflectiveAgent.generate(
        prompt = prompt1,
        options = optionsWithMetadata1
    )

    println("回答1:")
    println(response1.text)

    // 第二个问题
    val prompt2 = "大语言模型面临哪些挑战和局限性？"
    println("\n问题2: $prompt2")

    val options2 = AgentGenerateOptions()
    val optionsWithMetadata2 = options2.copy(metadata = mapOf("sessionId" to sessionId))
    val response2 = reflectiveAgent.generate(
        prompt = prompt2,
        options = optionsWithMetadata2
    )

    println("回答2:")
    println(response2.text)

    // 查看会话的反思记录
    val reflections = reflectiveAgent.getSessionReflections(sessionId)
    println("\n=== 会话反思记录 ===")
    println("共有 ${reflections.size} 条反思记录")

    // 显示部分反思记录
    if (reflections.isNotEmpty()) {
        val sample = reflections.take(2)
        sample.forEachIndexed { index, reflection ->
            println("\n反思记录 ${index + 1}:")
            println("类型: ${reflection.type}")
            println("内容: ${reflection.content}")
        }
    }

    // 查看全局反思
    val globalReflections = reflectiveAgent.getGlobalReflections()
    println("\n=== 全局反思 ===")
    println("共有 ${globalReflections.size} 条全局反思")
}
