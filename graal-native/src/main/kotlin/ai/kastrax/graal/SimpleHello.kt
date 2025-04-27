package ai.kastrax.graal

/**
 * 一个简单的 Hello World 程序，不使用 Kotlin 反射
 */
object SimpleHello {
    @JvmStatic
    fun main(args: Array<String>) {
        println("Hello, Native World from SimpleHello!")
        
        // 显示命令行参数
        if (args.isNotEmpty()) {
            println("Arguments:")
            args.forEachIndexed { index, arg ->
                println("  $index: $arg")
            }
        }
        
        // 显示系统信息
        println("System information:")
        println("  OS: ${System.getProperty("os.name")}")
        println("  Java version: ${System.getProperty("java.version")}")
        println("  User: ${System.getProperty("user.name")}")
    }
}
