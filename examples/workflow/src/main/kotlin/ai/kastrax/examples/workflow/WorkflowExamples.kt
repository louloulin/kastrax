package ai.kastrax.examples.workflow

import kotlinx.coroutines.runBlocking

/**
 * Workflow示例入口
 */
fun main() {
    println("KastraX Workflow示例")
    println("===================")
    println("请选择要运行的示例：")
    println("1. 简单工作流示例")
    println("2. 条件工作流示例")
    println("3. 并行工作流示例")
    println("4. 动态工作流示例")
    println("5. 工作流模板示例")
    println("0. 退出")

    print("\n请输入选项 [0-5]: ")
    val choice = readLine()?.toIntOrNull() ?: 0

    when (choice) {
        1 -> {
            println("\n运行简单工作流示例...")
            runBlocking {
            }
        }
        2 -> {
            println("\n运行条件工作流示例...")
            runBlocking {
                ai.kastrax.examples.workflow.ConditionalWorkflowExample().main()
            }
        }
        3 -> {
            println("\n运行并行工作流示例...")
            runBlocking {
                ai.kastrax.examples.workflow.ParallelWorkflowExample().main()
            }
        }
        4 -> {
            println("\n运行动态工作流示例...")
            runBlocking {
                ai.kastrax.examples.workflow.DynamicWorkflowExample().main()
            }
        }
        5 -> {
            println("\n运行工作流模板示例...")
            runBlocking {
                ai.kastrax.examples.workflow.WorkflowTemplateExample().main()
            }
        }
        else -> {
            println("退出程序")
        }
    }
}
