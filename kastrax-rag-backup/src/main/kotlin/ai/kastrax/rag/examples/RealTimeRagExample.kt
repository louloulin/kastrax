package ai.kastrax.rag.examples

import ai.kastrax.rag.document.Document
import ai.kastrax.rag.document.DirectoryDocumentLoader
import ai.kastrax.rag.document.RecursiveCharacterTextSplitter
import ai.kastrax.rag.embedding.OpenAIEmbeddingService
import ai.kastrax.rag.realtime.RealTimeRag
import ai.kastrax.rag.realtime.RealTimeRagConfig
import ai.kastrax.rag.realtime.realTimeRag
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Paths
import java.util.Scanner
import java.util.UUID

/**
 * 实时 RAG 示例
 */
fun main() = runBlocking {
    println("正在初始化实时 RAG 系统...")

    // 创建向量存储
    val vectorStore = InMemoryVectorStore()

    // 创建嵌入服务（需要 OpenAI API 密钥）
    val openaiApiKey = System.getenv("OPENAI_API_KEY") ?: ""
    if (openaiApiKey.isEmpty()) {
        println("请设置 OPENAI_API_KEY 环境变量")
        return@runBlocking
    }

    val embeddingService = OpenAIEmbeddingService(openaiApiKey)

    // 创建实时 RAG 系统
    val realTimeRag = realTimeRag {
        vectorStore(vectorStore)
        embeddingService(embeddingService)
        config(RealTimeRagConfig(
            streamingEnabled = true,
            useAsyncEmbedding = true,
            useIncrementalIndexing = true
        ))
    }

    // 启动实时 RAG 系统
    realTimeRag.start()

    // 准备实时 RAG 系统

    // 使用示例数据
    println("实时 RAG 系统已初始化！")
    println("你可以使用以下工具：")
    println("1. 实时检索工具 - 用于从文档中检索信息")
    println("2. 文档管理工具 - 用于添加、更新和删除文档")

    // 加载示例文档
    println("是否加载示例文档？(y/n)")
    val loadExamples = readLine()?.trim()?.lowercase() == "y"

    if (loadExamples) {
        loadExampleDocuments(realTimeRag)
    }

    // 交互式测试
    println("\n实时 RAG 系统已准备就绪！")
    println("你可以：")
    println("1. 测试检索功能")
    println("2. 测试添加文档")
    println("3. 测试更新文档")
    println("4. 测试删除文档")
    println("5. 输入 'exit' 退出")
    println()

    val scanner = Scanner(System.`in`)

    while (true) {
        print("\n选择操作 (1-5): ")
        val input = scanner.nextLine().trim()

        if (input.equals("exit", ignoreCase = true) || input == "5") {
            break
        }

        when (input) {
            "1" -> {
                print("\n输入查询内容: ")
                val query = scanner.nextLine().trim()
                val results = realTimeRag.search(query, 3)
                println("\n检索结果:")
                if (results.isEmpty()) {
                    println("  未找到相关文档")
                } else {
                    results.forEachIndexed { index, result ->
                        println("${index + 1}. 相关度: ${String.format("%.2f", result.score)}")
                        println("   内容: ${result.document.content.take(100)}...")
                        println()
                    }
                }
            }
            "2" -> {
                print("\n输入要添加的文档内容: ")
                val content = scanner.nextLine().trim()
                print("\n输入文档来源 (可选): ")
                val source = scanner.nextLine().trim()

                val metadata = if (source.isNotEmpty()) {
                    mapOf("source" to source)
                } else {
                    emptyMap()
                }

                val document = Document(content, metadata)
                val result = realTimeRag.addDocument(document)

                if (result) {
                    println("\n文档添加成功!")
                } else {
                    println("\n文档添加失败!")
                }
            }
            "3" -> {
                print("\n输入要更新的文档内容: ")
                val content = scanner.nextLine().trim()
                print("\n输入文档来源 (可选): ")
                val source = scanner.nextLine().trim()

                val metadata = if (source.isNotEmpty()) {
                    mapOf("source" to source, "updated" to "true")
                } else {
                    mapOf("updated" to "true")
                }

                val document = Document(content, metadata)
                val result = realTimeRag.updateDocument(document)

                if (result) {
                    println("\n文档更新成功!")
                } else {
                    println("\n文档更新失败!")
                }
            }
            "4" -> {
                print("\n输入要删除的文档内容: ")
                val content = scanner.nextLine().trim()

                val document = Document(content)
                val result = realTimeRag.deleteDocument(document)

                if (result) {
                    println("\n文档删除成功!")
                } else {
                    println("\n文档删除失败!")
                }
            }
            else -> println("\n无效的选择")
        }
    }

    println("\n实时 RAG 系统已退出")
}

/**
 * 加载示例文档
 *
 * @param realTimeRag 实时 RAG 系统
 */
private suspend fun loadExampleDocuments(realTimeRag: RealTimeRag) {
    println("正在加载示例文档...")

    // 示例文档
    val documents = listOf(
        Document(
            content = """
                # 人工智能简介

                人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，致力于创造能够模拟人类智能的机器。

                ## 主要领域

                1. 机器学习：使计算机能够从数据中学习，而无需明确编程。
                2. 自然语言处理：使计算机能够理解和生成人类语言。
                3. 计算机视觉：使计算机能够从图像或视频中获取信息。
                4. 机器人学：研究如何设计、制造和操作机器人。
            """.trimIndent(),
            metadata = mapOf(
                "source" to "AI简介.md",
                "category" to "人工智能",
                "author" to "AI研究员"
            )
        ),
        Document(
            content = """
                # 机器学习基础

                机器学习是人工智能的一个子领域，专注于开发能够从数据中学习的算法和模型。

                ## 主要类型

                1. 监督学习：使用标记的数据训练模型。
                2. 无监督学习：使用未标记的数据发现模式。
                3. 强化学习：通过与环境交互学习最优策略。

                ## 常见算法

                - 线性回归
                - 决策树
                - 神经网络
                - 支持向量机
                - K均值聚类
            """.trimIndent(),
            metadata = mapOf(
                "source" to "机器学习基础.md",
                "category" to "机器学习",
                "author" to "ML专家"
            )
        ),
        Document(
            content = """
                # 深度学习入门

                深度学习是机器学习的一个分支，使用多层神经网络从数据中学习表示。

                ## 核心概念

                1. 神经网络：由多层神经元组成的计算模型。
                2. 反向传播：用于训练神经网络的算法。
                3. 激活函数：引入非线性，使网络能够学习复杂模式。

                ## 常见架构

                - 卷积神经网络（CNN）：适用于图像处理。
                - 循环神经网络（RNN）：适用于序列数据。
                - 变换器（Transformer）：适用于自然语言处理。
            """.trimIndent(),
            metadata = mapOf(
                "source" to "深度学习入门.md",
                "category" to "深度学习",
                "author" to "DL研究员"
            )
        )
    )

    // 添加文档
    for (document in documents) {
        realTimeRag.addDocument(document)
        println("已添加文档：${document.metadata["source"]}")
    }

    println("示例文档加载完成！")

    // 等待处理完成
    delay(1000)
}
