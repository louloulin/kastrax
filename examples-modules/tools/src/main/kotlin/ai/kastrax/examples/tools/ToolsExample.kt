package ai.kastrax.examples.tools

import ai.kastrax.core.agent.agent
import ai.kastrax.integrations.deepseek.DeepSeekModel
import ai.kastrax.integrations.deepseek.deepSeek
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

            model = deepSeek {
                model(DeepSeekModel.DEEPSEEK_CHAT)
                // 显式设置 API 密钥
                apiKey("sk-85e83081df28490b9ae63188f0cb4f79")
            }

            // 添加工具
            tools {
                tool(webSearchTool)
                tool(fileSystemTool)
            }
        }

        // 使用代理
        println("\n开始与代理交互...")

        // 定义示例问题列表，而不是依赖用户输入
        val exampleQuestions = listOf(
            "搜索关于 Kotlin 协程的信息",
            "将 Kotlin 协程的基本概念保存到 kotlin-coroutines.txt 文件中",
            "读取 kotlin-coroutines.txt 文件的内容",
            "列出当前目录中的所有文件"
        )

        println("\n正在使用示例问题进行演示...")

        // 使用示例问题而不是用户输入
        for (question in exampleQuestions) {
            println("\n示例问题: $question")
            println("思考中...")

            try {
                val response = researchAgent.generate(question)
                println("代理: ${response.text}")
            } catch (e: Exception) {
                println("错误: ${e.message}")
            }
        }

    } finally {
        // 清理临时目录
        Files.walk(tempDir)
            .sorted(Comparator.reverseOrder())
            .forEach { Files.delete(it) }
        println("\n清理临时目录: $tempDir")
    }
}
