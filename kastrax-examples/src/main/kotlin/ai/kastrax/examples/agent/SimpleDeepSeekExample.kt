package ai.kastrax.examples.agent

import ai.kastrax.agent.templates.AgentTemplates
import ai.kastrax.core.agent.InMemorySessionManager
import ai.kastrax.core.agent.InMemoryStateManager
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking

/**
 * 简单的DeepSeek代理示例
 *
 * 本示例演示如何使用DeepSeek模型创建代理，
 * 并使用内存状态和会话管理。
 */
fun main() = runBlocking {
    // 创建内存状态和会话管理器
    val stateManager = InMemoryStateManager()
    val sessionManager = InMemorySessionManager()

    // 创建DeepSeek模型
    val deepSeekModel = deepSeek {
        model(DeepSeekModel.DEEPSEEK_CHAT)
        apiKey(System.getenv("DEEPSEEK_API_KEY") ?: "sk-85e83081df28490b9ae63188f0cb4f79")
    }

    // 创建代理
    val agent = agent {
        name = "DeepSeekAgent"
        instructions = """
            你是一名专业的助手，名为DeepSeekAgent。
            你的主要职责是帮助用户回答问题，提供准确、有用的信息。
        """.trimIndent()
        model = deepSeekModel
        stateManager(stateManager)
        sessionManager(sessionManager)
        defaultGenerateOptions {
            maxSteps(3)
            temperature(0.3)
        }
    }

    // 创建会话
    val session = agent.createSession(
        title = "测试会话",
        resourceId = "user-123",
        metadata = mapOf("type" to "test")
    )

    println("创建会话: ${session?.id}")

    // 生成响应
    val response = agent.generate(
        "你好，请介绍一下自己",
        options = ai.kastrax.core.agent.AgentGenerateOptions(
            threadId = session?.id
        )
    )

    // 打印响应
    println("\n助手响应:")
    println(response.text)

    // 打印状态
    println("\n代理状态:")
    println(response.state)

    // 打印会话信息
    println("\n会话信息:")
    println(response.sessionInfo)

    // 获取会话消息
    val messages = session?.id?.let { agent.getSessionMessages(it) }
    println("\n会话消息数量: ${messages?.size}")
}
