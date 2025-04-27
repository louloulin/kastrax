package ai.kastrax.graal

/**
 * 一个不使用 Kotlin 反射和序列化的简单主程序
 */
object MainNoReflection {
    @JvmStatic
    fun main(args: Array<String>) {
        println("KastraX Native Image (No Reflection)")
        println("===================================")
        
        if (args.isEmpty()) {
            printHelp()
            return
        }
        
        when (args[0]) {
            "help" -> printHelp()
            "version" -> printVersion()
            "config" -> printConfig()
            else -> {
                println("未知命令: ${args[0]}")
                printHelp()
            }
        }
    }
    
    private fun printHelp() {
        println("可用命令:")
        println("  help    - 显示帮助信息")
        println("  version - 显示版本信息")
        println("  config  - 显示配置信息")
    }
    
    private fun printVersion() {
        println("KastraX 版本: 0.1.0")
        println("构建时间: ${java.time.LocalDateTime.now()}")
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
}
