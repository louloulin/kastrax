package com.dataflare.native

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

/**
 * Dataflare Native 应用程序入口点
 */
object NativeApp {
    private val startTime = LocalDateTime.now()
    private const val VERSION = "0.1.0"

    @JvmStatic
    fun main(args: Array<String>) {
        printBanner()

        // 处理命令行参数
        if (args.isEmpty()) {
            printUsage()
            return
        }

        when (args[0]) {
            "version" -> printVersion()
            "info" -> printSystemInfo()
            "help" -> printUsage()
            else -> {
                println("未知命令: ${args[0]}")
                printUsage()
            }
        }
    }

    private fun printBanner() {
        println("""
            ╔═══════════════════════════════════════════════╗
            ║                 DATAFLARE                     ║
            ║        Native Data Processing Engine          ║
            ╚═══════════════════════════════════════════════╝
        """.trimIndent())
    }

    private fun printVersion() {
        println("Dataflare 版本: $VERSION")
        println("构建类型: GraalVM Native Image")
        println("构建时间: ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
    }

    private fun printSystemInfo() {
        println("系统信息:")
        println("操作系统: ${System.getProperty("os.name")} ${System.getProperty("os.version")}")
        println("架构: ${System.getProperty("os.arch")}")
        println("Java 版本: ${System.getProperty("java.version")}")
        println("可用处理器: ${Runtime.getRuntime().availableProcessors()}")
        println("最大内存: ${Runtime.getRuntime().maxMemory() / 1024 / 1024} MB")
        println("已分配内存: ${Runtime.getRuntime().totalMemory() / 1024 / 1024} MB")
        println("空闲内存: ${Runtime.getRuntime().freeMemory() / 1024 / 1024} MB")
        println("启动时间: ${startTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
        println("当前时间: ${LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)}")
        println("运行时长: ${LocalDateTime.now().second - startTime.second} 秒")
    }

    private fun printUsage() {
        println("用法: dataflare <命令> [参数]")
        println("可用命令:")
        println("  version    显示版本信息")
        println("  info       显示系统信息")
        println("  help       显示帮助信息")
    }
}
