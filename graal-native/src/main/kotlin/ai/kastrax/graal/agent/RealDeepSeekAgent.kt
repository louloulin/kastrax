package ai.kastrax.graal.agent

import ai.kastrax.core.agent.InMemorySessionManager
import ai.kastrax.core.agent.InMemoryStateManager
import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import java.util.*

/**
 * 真实的 DeepSeek Agent 类
 * 使用 agent DSL 构建
 */
object RealDeepSeekAgent {
    private val logger = LoggerFactory.getLogger(RealDeepSeekAgent::class.java)
    private const val DEFAULT_API_KEY = "sk-85e83081df28490b9ae63188f0cb4f79"

    /**
     * 运行 DeepSeek Agent
     */
    fun run(apiKey: String = DEFAULT_API_KEY, interactive: Boolean = true) = runBlocking {
        logger.info("启动真实的 DeepSeek Agent...")
        println("使用 API 密钥: $apiKey")

        // 创建内存状态和会话管理器
        val stateManager = InMemoryStateManager()
        val sessionManager = InMemorySessionManager()

        // 创建 DeepSeek 模型
        val deepSeekModel = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            apiKey(apiKey.ifEmpty { DEFAULT_API_KEY })
        }

        // 创建代理
        val agent = agent {
            name = "DeepSeekAgent"
            instructions = """
                你是一名专业的助手，名为DeepSeekAgent。
                你的主要职责是帮助用户回答问题，提供准确、有用的信息。
                你可以回答用户的问题，执行简单的计算，并提供当前的日期和时间。
                请保持回答简洁、准确和有帮助。
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
            title = "DeepSeek 会话",
            resourceId = "user-1",
            metadata = mapOf("type" to "interactive")
        )

        println("创建会话: ${session?.id}")

        if (interactive) {
            runInteractiveSession(agent, session?.id)
        } else {
            runSingleExample(agent, session?.id)
        }
    }

    /**
     * 运行交互式会话
     */
    private suspend fun runInteractiveSession(agent: ai.kastrax.core.agent.Agent, threadId: String?) {
        println("欢迎使用真实的 DeepSeek Agent！")
        println("输入 'exit' 或 'quit' 退出")
        println()

        var running = true
        val scanner = Scanner(System.`in`)

        while (running) {
            print("请输入问题: ")
            val input = scanner.nextLine() ?: ""

            if (input.equals("exit", ignoreCase = true) || input.equals("quit", ignoreCase = true)) {
                running = false
                continue
            }

            if (input.isBlank()) {
                continue
            }

            try {
                // 生成响应
                val response = agent.generate(
                    input,
                    options = ai.kastrax.core.agent.AgentGenerateOptions(
                        threadId = threadId
                    )
                )

                println("回答: ${response.text}")
                println()
            } catch (e: Exception) {
                logger.error("请求失败: ${e.message}")
                println("回答: 抱歉，请求失败: ${e.message}")
                println()
            }
        }

        println("感谢使用真实的 DeepSeek Agent！")
    }

    /**
     * 运行单个示例
     */
    private suspend fun runSingleExample(agent: ai.kastrax.core.agent.Agent, threadId: String?) {
        val examples = listOf(
            "你好，请介绍一下自己",
            "现在是什么时间？",
            "计算 123 + 456 是多少？",
            "你能记住我的名字吗？我叫张三",
            "我的名字是什么？"
        )

        println("真实的 DeepSeek Agent 示例演示：")
        println()

        examples.forEach { question ->
            println("问题: $question")

            try {
                // 生成响应
                val response = agent.generate(
                    question,
                    options = ai.kastrax.core.agent.AgentGenerateOptions(
                        threadId = threadId
                    )
                )

                println("回答: ${response.text}")
                println()
            } catch (e: Exception) {
                logger.error("请求失败: ${e.message}")
                println("回答: 抱歉，请求失败: ${e.message}")
                println()
            }
        }

        println("感谢使用真实的 DeepSeek Agent！")
    }


}
