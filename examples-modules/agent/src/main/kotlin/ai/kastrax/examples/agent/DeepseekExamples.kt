package ai.kastrax.examples.agent

import kotlinx.coroutines.runBlocking

/**
 * Deepseek 示例入口
 *
 * 提供了多个 Deepseek 示例的入口点
 */
fun deepseekExamples() = runBlocking {
    println("=== Deepseek 示例 ===")
    println("请选择要运行的示例：")
    println("1. 基本 Deepseek Agent 示例")
    println("2. Deepseek Tool Agent 示例")
    println("3. Deepseek 架构示例")
    println("4. Deepseek Memory Agent 示例")
    println("0. 退出")

    print("请输入选项 (0-4): ")
    val option = readLine()?.trim()?.toIntOrNull() ?: 0

    when (option) {
        1 -> {
            println("\n运行基本 Deepseek Agent 示例...")
            deepseekAgentExample()
        }
        2 -> {
            println("\n运行 Deepseek Tool Agent 示例...")
            deepseekToolAgentExample()
        }
        3 -> {
            println("\n运行 Deepseek 架构示例...")
            deepseekArchitectureExample()
        }
        4 -> {
            println("\n运行 Deepseek Memory Agent 示例...")
            deepseekMemoryExample()
        }
        else -> {
            println("退出程序")
        }
    }
}
