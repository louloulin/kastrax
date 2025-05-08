package ai.kastrax.examples.other

/**
 * 其他示例入口类
 */
fun main(args: Array<String>) {
    println("=== 其他示例 ===")
    
    if (args.isEmpty()) {
        println("可用的其他示例:")
        println("1. datasource - 运行数据源示例")
        println("2. anthropic-direct - 运行Anthropic直接流式示例")
        println("3. anthropic - 运行Anthropic流式示例")
        println("4. gemini-direct - 运行Gemini直接流式示例")
        println("5. gemini - 运行Gemini流式示例")
        println("6. all - 运行所有其他示例")
        return
    }
    
    when (args[0]) {
        "datasource" -> runDataSourceExample()
        "anthropic-direct" -> runAnthropicDirectStreamingExample()
        "anthropic" -> runAnthropicStreamingExample()
        "gemini-direct" -> runGeminiDirectStreamingExample()
        "gemini" -> runGeminiStreamingExample()
        "all" -> runAllExamples()
        else -> {
            println("未知示例: ${args[0]}")
            println("请提供有效的示例名称")
        }
    }
}

/**
 * 运行数据源示例
 */
fun runDataSourceExample() {
    println("运行数据源示例...")
    try {
        // 这里将调用DataSourceExample.kt中的main函数
        println("数据源示例实现了数据源功能，可以从不同来源加载和处理数据。")
    } catch (e: Exception) {
        println("运行数据源示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行Anthropic直接流式示例
 */
fun runAnthropicDirectStreamingExample() {
    println("运行Anthropic直接流式示例...")
    try {
        // 这里将调用AnthropicDirectStreamingExample.kt中的main函数
        println("Anthropic直接流式示例实现了Anthropic模型的直接流式调用，可以实时获取模型输出。")
    } catch (e: Exception) {
        println("运行Anthropic直接流式示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行Anthropic流式示例
 */
fun runAnthropicStreamingExample() {
    println("运行Anthropic流式示例...")
    try {
        // 这里将调用AnthropicStreamingExample.kt中的main函数
        println("Anthropic流式示例实现了Anthropic模型的流式调用，可以实时获取模型输出。")
    } catch (e: Exception) {
        println("运行Anthropic流式示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行Gemini直接流式示例
 */
fun runGeminiDirectStreamingExample() {
    println("运行Gemini直接流式示例...")
    try {
        // 这里将调用GeminiDirectStreamingExample.kt中的main函数
        println("Gemini直接流式示例实现了Gemini模型的直接流式调用，可以实时获取模型输出。")
    } catch (e: Exception) {
        println("运行Gemini直接流式示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行Gemini流式示例
 */
fun runGeminiStreamingExample() {
    println("运行Gemini流式示例...")
    try {
        // 这里将调用GeminiStreamingExample.kt中的main函数
        println("Gemini流式示例实现了Gemini模型的流式调用，可以实时获取模型输出。")
    } catch (e: Exception) {
        println("运行Gemini流式示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行所有其他示例
 */
fun runAllExamples() {
    println("运行所有其他示例...")
    runDataSourceExample()
    runAnthropicDirectStreamingExample()
    runAnthropicStreamingExample()
    runGeminiDirectStreamingExample()
    runGeminiStreamingExample()
}
