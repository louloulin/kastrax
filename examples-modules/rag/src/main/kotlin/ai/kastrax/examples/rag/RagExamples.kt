package ai.kastrax.examples.rag

/**
 * RAG示例入口类
 */
fun main(args: Array<String>) {
    println("=== RAG示例 ===")
    
    if (args.isEmpty()) {
        println("可用的RAG示例:")
        println("1. basic - 运行基础RAG示例")
        println("2. workflow - 运行RAG工作流示例")
        println("3. fastembed - 运行快速嵌入RAG示例")
        println("4. all - 运行所有RAG示例")
        return
    }
    
    when (args[0]) {
        "basic" -> runRAGExample()
        "workflow" -> runRAGWorkflowExample()
        "fastembed" -> runFastEmbedRAGExample()
        "all" -> runAllExamples()
        else -> {
            println("未知示例: ${args[0]}")
            println("请提供有效的示例名称")
        }
    }
}

/**
 * 运行基础RAG示例
 */
fun runRAGExample() {
    println("运行基础RAG示例...")
    try {
        // 这里将调用RAGExample.kt中的main函数
        println("基础RAG示例实现了基础RAG系统，可以从文档中检索信息并生成回答。")
    } catch (e: Exception) {
        println("运行基础RAG示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行RAG工作流示例
 */
fun runRAGWorkflowExample() {
    println("运行RAG工作流示例...")
    try {
        // 这里将调用RAGWorkflowExample.kt中的main函数
        println("RAG工作流示例实现了RAG工作流，包含研究、分析和报告生成步骤。")
    } catch (e: Exception) {
        println("运行RAG工作流示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行快速嵌入RAG示例
 */
fun runFastEmbedRAGExample() {
    println("运行快速嵌入RAG示例...")
    try {
        // 这里将调用FastEmbedRAGExample.kt中的main函数
        println("快速嵌入RAG示例实现了使用本地嵌入模型的RAG系统，无需依赖外部API。")
    } catch (e: Exception) {
        println("运行快速嵌入RAG示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行所有RAG示例
 */
fun runAllExamples() {
    println("运行所有RAG示例...")
    runRAGExample()
    runRAGWorkflowExample()
    runFastEmbedRAGExample()
}
