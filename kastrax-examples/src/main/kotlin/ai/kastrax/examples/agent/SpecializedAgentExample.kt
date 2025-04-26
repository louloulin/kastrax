package ai.kastrax.examples.agent

import ai.kastrax.agent.templates.AgentTemplates
import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.SQLiteSessionManager
import ai.kastrax.core.agent.SQLiteStateManager
import ai.kastrax.core.tools.web.WebSearchTool
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.runBlocking
import java.nio.file.Paths

/**
 * 专用代理示例
 *
 * 本示例演示如何使用专用代理模板创建不同类型的代理，
 * 并使用持久化的状态和会话管理。
 */
fun main() = runBlocking {
    // 创建持久化的状态和会话管理器
    val dataDir = Paths.get(System.getProperty("user.home"), ".kastrax", "data").toString()
    val stateManager = SQLiteStateManager("$dataDir/agent_state.db")
    val sessionManager = SQLiteSessionManager("$dataDir/agent_session.db")

    // 创建工具
    val webSearchTool = WebSearchTool(
        apiKey = System.getenv("GOOGLE_API_KEY") ?: "",
        searchEngineId = System.getenv("GOOGLE_SEARCH_ENGINE_ID") ?: ""
    )

    // 创建OpenAI模型
    val model = openAi(
        apiKey = System.getenv("OPENAI_API_KEY") ?: "",
        model = "gpt-4o"
    )

    // 创建研究助手代理
    val researchAgent = AgentTemplates.createResearchAssistantAgent(
        name = "ResearchAssistant",
        model = model,
        additionalTools = mapOf("web_search" to webSearchTool),
        customInstructions = """
            你是一名专业的研究助手，名为ResearchAssistant。
            你的主要职责是帮助用户进行深入的研究和分析，特别是关于人工智能和机器学习领域的最新发展。
            你必须使用提供的web_search工具来获取最新信息，并确保引用信息来源。
        """.trimIndent()
    )

    // 设置状态和会话管理
    researchAgent.stateManager = stateManager
    researchAgent.sessionManager = sessionManager

    // 创建会话
    val session = researchAgent.createSession(
        title = "AI研究会话",
        resourceId = "user-123",
        metadata = mapOf("topic" to "AI")
    )

    println("创建会话: ${session?.id}")

    // 生成响应
    val response = researchAgent.generate(
        "请研究并总结大型语言模型的最新进展，特别是在2023年发布的模型",
        options = AgentGenerateOptions(
            threadId = session?.id,
            maxSteps = 3
        )
    )

    // 打印响应
    println("\n研究助手响应:")
    println(response.text)

    // 打印状态
    println("\n代理状态:")
    println(response.state)

    // 打印会话信息
    println("\n会话信息:")
    println(response.sessionInfo)

    // 获取会话消息
    val messages = session?.id?.let { researchAgent.getSessionMessages(it) }
    println("\n会话消息数量: ${messages?.size}")
}
