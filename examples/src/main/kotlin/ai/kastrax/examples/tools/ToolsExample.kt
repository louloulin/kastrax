package ai.kastrax.examples.tools

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.openai.openAi
import ai.kastrax.core.tools.ToolFactory
import ai.kastrax.core.tools.web.WebSearchTool
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths

/**
 * 工具使用示例，展示如何使用 Web 搜索和文件系统工具。
 */
fun main() = runBlocking {
    println("KastraX 工具示例")
    println("---------------")

    // 创建临时目录
    val tempDir = Files.createTempDirectory("kastrax-tools-example")
    println("创建临时目录: $tempDir")

    try {
        // 创建工具
        val webSearchTool = ToolFactory.createWebSearchTool(
            searchEngine = WebSearchTool.SearchEngine.MOCK,
            maxResults = 3
        )

        val fileSystemTool = ToolFactory.createFileSystemTool(
            rootPath = tempDir.toString(),
            allowAbsolutePaths = false
        )

        // 创建代理
        val researchAgent = agent {
            name = "研究助手"
            instructions = """
                你是一个研究助手，可以帮助用户搜索信息并保存到文件中。

                你可以使用以下工具：
                1. web_search - 搜索互联网获取信息
                2. file_system - 读取和写入文件

                当用户要求你搜索信息时，使用 web_search 工具。
                当用户要求你保存信息时，使用 file_system 工具。

                始终以友好、专业的方式回应用户。
            """.trimIndent()

            model = openAi("gpt-4o")

            tools {
                add(webSearchTool)
                add(fileSystemTool)
            }
        }

        // 使用代理
        println("\n开始与代理交互...")

        // 搜索信息
        val searchResponse = researchAgent.generate("搜索关于 Kotlin 协程的信息")
        println("\n用户: 搜索关于 Kotlin 协程的信息")
        println("代理: ${searchResponse.text}")

        // 保存信息到文件
        val saveResponse = researchAgent.generate("将 Kotlin 协程的基本概念保存到 kotlin-coroutines.txt 文件中")
        println("\n用户: 将 Kotlin 协程的基本概念保存到 kotlin-coroutines.txt 文件中")
        println("代理: ${saveResponse.text}")

        // 读取文件
        val readResponse = researchAgent.generate("读取 kotlin-coroutines.txt 文件的内容")
        println("\n用户: 读取 kotlin-coroutines.txt 文件的内容")
        println("代理: ${readResponse.text}")

        // 列出目录
        val listResponse = researchAgent.generate("列出当前目录中的所有文件")
        println("\n用户: 列出当前目录中的所有文件")
        println("代理: ${listResponse.text}")

    } finally {
        // 清理临时目录
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.delete(it) }
        println("\n清理临时目录: $tempDir")
    }
}
