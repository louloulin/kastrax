package ai.kastrax.examples.agent

/**
 * Agent示例入口
 */
fun main() {
    println("KastraX Agent示例")
    println("=================")
    println("请选择要运行的示例：")
    println("1. 简单Agent示例")
    println("2. 高级Agent示例")
    println("3. Agent网络示例")
    println("4. Agent状态管理示例")
    println("5. Agent版本控制示例")
    println("6. 协作型Agent网络示例")
    println("7. 创造性Agent示例")
    println("8. Deepseek Agent示例")
    println("9. Deepseek架构示例")
    println("10. Zod工具Agent示例")
    println("0. 退出")

    print("\n请输入选项 [0-10]: ")
    val choice = readLine()?.toIntOrNull() ?: 0

    when (choice) {
        1 -> {
            println("\n运行简单Agent示例...")
            // 调用HelloAgent.kt中的main函数
        }
        2 -> {
            println("\n运行高级Agent示例...")
            println("此功能尚未实现")
            // advancedAgentExample()
        }
        3 -> {
            println("\n运行Agent网络示例...")
            agentNetworkExample()
        }
        4 -> {
            println("\n运行Agent状态管理示例...")
            agentStateExample()
        }
        5 -> {
            println("\n运行Agent版本控制示例...")
            agentVersioningExample()
        }
        6 -> {
            println("\n运行协作型Agent网络示例...")
            collaborativeAgentNetworkExample()
        }
        7 -> {
            println("\n运行创造性Agent示例...")
            creativeAgentExample()
        }
        8 -> {
            println("\n运行Deepseek Agent示例...")
            deepseekAgentExample()
        }
        9 -> {
            println("\n运行Deepseek架构示例...")
            deepseekArchitectureExample()
        }
        10 -> {
            println("\n运行Zod工具Agent示例...")
            println("Zod工具Agent示例暂未实现")
            // zodAgentExample()
        }
        else -> {
            println("退出程序")
        }
    }
}
