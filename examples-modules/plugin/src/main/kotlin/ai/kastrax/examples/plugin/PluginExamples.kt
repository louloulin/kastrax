package ai.kastrax.examples.plugin

/**
 * 插件示例入口类
 */
fun main(args: Array<String>) {
    println("=== 插件示例 ===")
    
    if (args.isEmpty()) {
        println("可用的插件示例:")
        println("1. http-connector - 运行HTTP连接器插件示例")
        println("2. http-step - 运行HTTP步骤插件示例")
        println("3. all - 运行所有插件示例")
        return
    }
    
    when (args[0]) {
        "http-connector" -> runHttpConnectorPluginExample()
        "http-step" -> runHttpStepPluginExample()
        "all" -> runAllExamples()
        else -> {
            println("未知示例: ${args[0]}")
            println("请提供有效的示例名称")
        }
    }
}

/**
 * 运行HTTP连接器插件示例
 */
fun runHttpConnectorPluginExample() {
    println("运行HTTP连接器插件示例...")
    try {
        // 这里将调用HttpConnectorPlugin.kt中的相关函数
        println("HTTP连接器插件示例实现了HTTP连接器插件，可以通过HTTP协议连接外部服务。")
    } catch (e: Exception) {
        println("运行HTTP连接器插件示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行HTTP步骤插件示例
 */
fun runHttpStepPluginExample() {
    println("运行HTTP步骤插件示例...")
    try {
        // 这里将调用HttpStepPlugin.kt中的相关函数
        println("HTTP步骤插件示例实现了HTTP步骤插件，可以在工作流中执行HTTP请求。")
    } catch (e: Exception) {
        println("运行HTTP步骤插件示例时出错: ${e.message}")
        e.printStackTrace()
    }
}

/**
 * 运行所有插件示例
 */
fun runAllExamples() {
    println("运行所有插件示例...")
    runHttpConnectorPluginExample()
    runHttpStepPluginExample()
}
