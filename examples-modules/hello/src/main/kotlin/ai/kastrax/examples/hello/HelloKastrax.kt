package ai.kastrax.examples.hello

import kotlinx.coroutines.runBlocking

/**
 * 一个简单的 Kastrax 示例
 */
fun main() = runBlocking {
    println("Hello Kastrax!")
    println("-------------------")

    // 模拟代理回答
    println("\n模拟代理回答:")
    val simulatedResponse = """
        你好！我是一个 AI 助手，可以回答你的问题、提供信息和帮助你完成各种任务。

        我可以帮助你：
        - 回答各种知识性问题
        - 提供信息和解释
        - 协助写作和创作
        - 帮助解决问题
        - 提供建议和意见

        有什么我可以帮助你的吗？
    """.trimIndent()

    // 打印回答
    println("\n代理回答:")
    println(simulatedResponse)

    println("\nHello Kastrax 示例完成")
}
