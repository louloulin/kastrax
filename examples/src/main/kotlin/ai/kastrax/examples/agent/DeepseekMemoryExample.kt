package ai.kastrax.examples.agent

import ai.kastrax.core.agent.AgentGenerateOptions
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.memory.api.MemoryPriority
import ai.kastrax.memory.impl.MemoryFactory
import ai.kastrax.memory.impl.memory
import ai.kastrax.memory.impl.inMemoryStorage
import kotlinx.coroutines.runBlocking

/**
 * Deepseek Memory Agent 示例
 *
 * 展示如何使用 Deepseek 作为 LLM 提供商创建带内存功能的 Agent
 */
fun deepseekMemoryExample() = runBlocking {
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

    // 创建内存
    val memoryStore = memory {
        storage(inMemoryStorage())
        lastMessages(10)
        semanticRecall(true)
    }

    // 使用 DSL 创建 Agent
    val agent = agent {
        // 设置 Agent 名称
        name = "DeepseekMemoryAgent"

        // 设置 Agent 指令
        instructions = """
            你是一个由 Deepseek 驱动的智能助手，具有记忆功能。
            你应该：
            1. 记住用户提供的信息
            2. 在回答问题时利用之前的对话历史
            3. 保持一致性，避免重复询问已经提供过的信息
            4. 保持礼貌和专业
        """.trimIndent()

        // 设置 LLM 模型
        model = llm

        // 设置内存
        memory(memoryStore)

        // 配置默认生成选项
        defaultGenerateOptions {
            temperature(0.7)
            maxTokens(2000)
        }
    }

    println("=== Deepseek Memory Agent 示例 ===")

    // 创建会话
    val sessionId = "memory-test-session"
    val session = agent.createSession(
        title = "记忆测试会话",
        metadata = mapOf("userId" to "test-user")
    )

    println("创建会话: ${session?.id}")

    // 准备对话
    val conversation = listOf(
        "你好，我叫张三。",
        "我今年 30 岁，是一名软件工程师。",
        "我喜欢编程、阅读和旅行。",
        "你还记得我的名字吗？",
        "我的职业是什么？",
        "我有哪些兴趣爱好？",
        "我今年多大了？"
    )

    // 进行对话
    conversation.forEachIndexed { index, message ->
        println("\n用户: $message")

        // 创建生成选项
        val options = AgentGenerateOptions()
        val optionsWithMetadata = options.copy(
            metadata = mapOf("sessionId" to sessionId)
        )

        // 生成回答
        val response = agent.generate(message, optionsWithMetadata)

        println("助手: ${response.text}")

        // 添加记忆（如果需要）
        if (index < 3) {
            // 为前三条消息添加高优先级记忆
            // 注意：在实际应用中，您需要使用正确的 Memory API 来添加记忆
            // 这里简化了实现
            println("[模拟] 添加记忆: $message")
        }
    }

    // 获取会话消息
    val messages = agent.getSessionMessages(sessionId)
    println("\n=== 会话消息 ===")
    println("共有 ${messages?.size ?: 0} 条消息")

    messages?.take(5)?.forEachIndexed { index, sessionMessage ->
        println("${index + 1}. ${sessionMessage.message.role.name}: ${sessionMessage.message.content}")
    }
}
