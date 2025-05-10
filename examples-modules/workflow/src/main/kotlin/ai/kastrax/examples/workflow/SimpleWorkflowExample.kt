package ai.kastrax.examples.workflow

import kotlinx.coroutines.runBlocking

/**
 * 简单的工作流示例
 */
fun main() = runBlocking {
    println("Hello, Kastrax Workflow!")
    println("这是一个简单的工作流示例，用于测试编译和运行。")
    
    // 运行 RAG 工作流示例
    println("\n运行 RAG 工作流示例...")
    RAGWorkflowExample.main()
}

/**
 * RAG 工作流示例对象
 */
object RAGWorkflowExample {
    /**
     * RAG 工作流示例主函数
     */
    @JvmStatic
    fun main() = runBlocking {
        println("开始 RAG 工作流示例...")
        println("这是一个简单的 RAG 工作流示例，用于测试编译和运行。")
    }
}
