package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.memory.impl.MemoryFactory
import kotlinx.coroutines.runBlocking

fun main() = runBlocking {
    // 创建一个带有内存系统的代理
    val myAgent = agent {
        name = "记忆助手"
        instructions = """
            你是一个有记忆能力的助手，能够记住之前的对话内容。
            当用户提到之前讨论过的话题时，你应该能够回忆起相关信息。
            保持友好和专业的态度。
        """.trimIndent()
        model = deepSeek {
            model(DeepSeekModel.DEEPSEEK_CHAT)
            // 显式设置 API 密钥
            apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
        }

        // 配置内存系统
        memory = ai.kastrax.memory.impl.MemoryFactory.createMemory {
            storage(ai.kastrax.memory.impl.MemoryFactory.createInMemoryStorage())
            lastMessages(10)
            semanticRecall(true)
        }
    }

    println("记忆助手示例")
    println("-------------")
    println("输入 'exit' 退出")

    // 创建一个新的对话线程
    var threadId: String? = null

    // 定义示例问题列表，而不是依赖用户输入
    val exampleQuestions = listOf(
        "你好，我是小明。",
        "我最喜欢的颜色是蓝色。",
        "你还记得我的名字吗？",
        "我之前说过我喜欢什么颜色吗？"
    )

    println("\n正在使用示例问题进行演示...")

    // 使用示例问题而不是用户输入
    for (input in exampleQuestions) {
        println("\n示例问题: $input")
        println("\n思考中...")

        try {
            // 生成回复，使用相同的线程ID保持对话上下文
            val response = myAgent.generate(input, options = ai.kastrax.core.agent.AgentGenerateOptions(
                threadId = threadId,
                threadTitle = "对话示例"
            ))

            // 保存线程ID以便下次使用
            threadId = response.threadId

            println("\n回复:")
            println(response.text)
        } catch (e: Exception) {
            println("\n错误: ${e.message}")
        }
    }

    println("\n示例演示完成。")
}
