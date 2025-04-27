package ai.kastrax.graal

import java.time.LocalDateTime

/**
 * 一个不使用 Kotlin 反射的简化版本的 KastraX 主程序
 */
object SimpleMain {
    @JvmStatic
    fun main(args: Array<String>) {
        println("KastraX Native Image (Simple)")
        println("============================")
        
        if (args.isEmpty()) {
            printHelp()
            return
        }
        
        when (args[0]) {
            "help" -> printHelp()
            "version" -> printVersion()
            "config" -> printConfig()
            "deepseek" -> testDeepSeek()
            else -> {
                println("未知命令: ${args[0]}")
                printHelp()
            }
        }
    }
    
    private fun printHelp() {
        println("可用命令:")
        println("  help     - 显示帮助信息")
        println("  version  - 显示版本信息")
        println("  config   - 显示配置信息")
        println("  deepseek - 测试 DeepSeek 集成")
    }
    
    private fun printVersion() {
        println("KastraX 版本: 0.1.0")
        println("构建时间: ${LocalDateTime.now()}")
        println("Java 版本: ${System.getProperty("java.version")}")
        println("OS: ${System.getProperty("os.name")}")
    }
    
    private fun printConfig() {
        println("配置信息:")
        println("  工作目录: ${System.getProperty("user.dir")}")
        println("  用户主目录: ${System.getProperty("user.home")}")
        println("  临时目录: ${System.getProperty("java.io.tmpdir")}")
        println("  文件分隔符: ${System.getProperty("file.separator")}")
        println("  路径分隔符: ${System.getProperty("path.separator")}")
        println("  行分隔符: ${System.getProperty("line.separator").replace("\n", "\\n").replace("\r", "\\r")}")
    }
    
    private fun testDeepSeek() {
        println("测试 DeepSeek 集成")
        println("这是一个简化的实现，不使用 Kotlin 反射和序列化")
        
        // 模拟 DeepSeek 请求和响应
        val request = mapOf(
            "model" to "deepseek-coder",
            "messages" to listOf(
                mapOf("role" to "system", "content" to "You are a helpful assistant."),
                mapOf("role" to "user", "content" to "Hello, how are you?")
            ),
            "temperature" to 0.7,
            "max_tokens" to 100
        )
        
        println("请求: $request")
        println("响应: 我很好，谢谢询问！有什么我可以帮助你的吗？")
    }
}
