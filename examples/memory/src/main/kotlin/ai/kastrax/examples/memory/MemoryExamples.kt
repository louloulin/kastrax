package ai.kastrax.examples.memory

/**
 * 内存示例入口类
 */
fun main(args: Array<String>) {
    println("=== 内存示例 ===")
    
    if (args.isEmpty()) {
        println("可用的内存示例:")
        println("1. working - 运行工作内存示例")
        println("2. compression - 运行内存压缩示例")
        println("3. manager - 运行内存管理器示例")
        println("4. tags - 运行标签和共享示例")
        println("5. all - 运行所有内存示例")
        return
    }
    
    when (args[0]) {
        "working" -> runWorkingMemoryExample()
        "compression" -> runMemoryCompressionExample()
        "manager" -> runMemoryManagerExample()
        "tags" -> runTagsAndSharingExample()
        "all" -> runAllExamples()
        else -> {
            println("未知示例: ${args[0]}")
            println("请提供有效的示例名称")
        }
    }
}

/**
 * 运行工作内存示例
 */
fun runWorkingMemoryExample() {
    println("运行工作内存示例...")
    try {
        // 这里将调用WorkingMemoryExample.kt中的main函数
        println("工作内存示例实现了工作内存功能，可以记录和更新用户信息和对话上下文。")
    } catch (e: Exception) {
        println("运行工作内存示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行内存压缩示例
 */
fun runMemoryCompressionExample() {
    println("运行内存压缩示例...")
    try {
        // 这里将调用MemoryCompressionExample.kt中的main函数
        println("内存压缩示例实现了内存压缩功能，可以在对话变长时自动压缩历史记录。")
    } catch (e: Exception) {
        println("运行内存压缩示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行内存管理器示例
 */
fun runMemoryManagerExample() {
    println("运行内存管理器示例...")
    try {
        // 这里将调用MemoryManagerExample.kt中的main函数
        println("内存管理器示例实现了记忆管理器，可以进行高级查询、导出和管理对话记忆。")
    } catch (e: Exception) {
        println("运行内存管理器示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行标签和共享示例
 */
fun runTagsAndSharingExample() {
    println("运行标签和共享示例...")
    try {
        // 这里将调用TagsAndSharingExample.kt中的main函数
        println("标签和共享示例实现了标签和线程共享功能，可以对消息进行分类和在不同线程之间共享消息。")
    } catch (e: Exception) {
        println("运行标签和共享示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行所有内存示例
 */
fun runAllExamples() {
    println("运行所有内存示例...")
    runWorkingMemoryExample()
    runMemoryCompressionExample()
    runMemoryManagerExample()
    runTagsAndSharingExample()
}
