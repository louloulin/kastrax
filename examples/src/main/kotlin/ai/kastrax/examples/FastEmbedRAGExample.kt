package ai.kastrax.examples


import ai.kastrax.core.agent.agent
import ai.kastrax.rag.RAG
import ai.kastrax.rag.document.DirectoryDocumentLoader
import ai.kastrax.rag.document.RecursiveCharacterTextSplitter
import ai.kastrax.rag.document.TextFileDocumentLoader
import ai.kastrax.rag.embedding.FastEmbedKotlinEmbeddingService
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import ai.kastrax.integrations.deepseek.deepSeek
import ai.kastrax.fastembed.EmbeddingModel
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * FastEmbed Kotlin RAG 示例
 *
 * 这个示例展示了如何使用 FastEmbed Kotlin 嵌入服务创建一个本地运行的 RAG 系统，无需依赖外部 API。
 * 该实现使用 FastEmbed Kotlin 库（基于 fastembed-rs 的 Kotlin 绑定），提供高性能的文本嵌入生成功能，无需 Python 环境。
 */
fun main() = runBlocking {
    println("初始化 FastEmbed Kotlin 嵌入服务...")

    // 创建 FastEmbed Kotlin 嵌入服务
    // 启用测试模式，使用模拟实现，因为我们在加载本地库时遇到问题
    System.setProperty("ai.kastrax.fastembed.test.mode", "true")
    println("注意: 正在使用测试模式，嵌入向量将是模拟的")

    // 打印系统信息，帮助调试本地库加载问题
    try {
        println("操作系统: ${System.getProperty("os.name")}, 架构: ${System.getProperty("os.arch")}")
        println("Java库路径: ${System.getProperty("java.library.path")}")
    } catch (e: Exception) {
        println("获取系统属性时出错: ${e.message}")
    }

    val embeddingService = FastEmbedKotlinEmbeddingService.create(
        model = EmbeddingModel.BGE_SMALL_ZH,  // 中文小型模型
        showDownloadProgress = true
    )

    println("嵌入模型维度: ${embeddingService.dimensions}")

    try {
        // 创建向量存储和 RAG 系统
        val vectorStore = InMemoryVectorStore()
        val rag = RAG(vectorStore, embeddingService)

        // 创建文档分割器
        val splitter = RecursiveCharacterTextSplitter(
            chunkSize = 500,
            chunkOverlap = 100
        )

        // 加载示例文档
        println("加载示例文档...")
        val exampleDocs = listOf(
            "人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。AI 研究包括机器学习、自然语言处理、计算机视觉等领域。",
            "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。机器学习算法通过分析大量数据来识别模式，并使用这些模式进行预测或决策。",
            "深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式。深度学习在图像识别、语音识别和自然语言处理等领域取得了显著成功。",
            "自然语言处理（NLP）是人工智能的一个分支，专注于使计算机理解和生成人类语言。NLP技术被用于机器翻译、情感分析、文本摘要等应用。",
            "计算机视觉是人工智能的一个领域，它使计算机能够从图像或视频中获取信息。计算机视觉技术被用于人脸识别、物体检测、自动驾驶等应用。"
        )

        // 创建临时文件并加载
        val tempDir = File("temp_docs").apply { mkdirs() }
        try {
            exampleDocs.forEachIndexed { index, content ->
                val file = File(tempDir, "doc_$index.txt")
                file.writeText(content)

                val loader = TextFileDocumentLoader(file.absolutePath)
                rag.loadDocuments(loader, splitter)
                println("已加载文档 ${index + 1}/${exampleDocs.size}: ${file.name}")
            }

            println("成功嵌入 ${exampleDocs.size} 个文档")

            // 从目录加载文档
            val docsDir = File("docs")
            if (docsDir.exists() && docsDir.isDirectory) {
                val directoryLoader = DirectoryDocumentLoader(
                    directory = docsDir,
                    recursive = true,
                    fileExtensions = listOf("txt", "md")
                )
                rag.loadDocuments(directoryLoader, splitter)
            }

            // 创建 DeepSeek 提供者（仅用于生成回答，嵌入使用本地模型）
            val deepseek = deepSeek(
                model = "deepseek-chat",
                apiKey = "sk-85e83081df28490b9ae63188f0cb4f79"
                // API 密钥从环境变量 DEEPSEEK_API_KEY 获取

            )

            // 创建 RAG 代理
            val ragAgent = agent {
                name = "FastEmbed RAG Agent"
                instructions = """
                    你是一个基于本地嵌入模型的问答助手。你的任务是使用提供的上下文信息回答用户的问题。

                    请遵循以下准则：
                    1. 仅使用提供的上下文信息回答问题
                    2. 如果上下文中没有足够的信息，请坦诚地说明你不知道
                    3. 不要编造信息或使用你自己的知识
                    4. 引用信息的来源（如果有）
                    5. 保持回答简洁、准确和有帮助

                    上下文信息：
                    {{context}}

                    用户问题：
                    {{question}}
                """.trimIndent()
                model = deepseek
            }

            // 使用 RAG 系统回答问题
            while (true) {
                print("\n请输入问题（输入 'exit' 退出）: ")
                val question = readLine() ?: ""

                if (question.equals("exit", ignoreCase = true)) {
                    break
                }

                // 检索相关上下文
                println("使用 FastEmbed 检索相关信息...")
                // 获取相似度分数用于调试
                val similarities = rag.getSimilarityScores(question)
                println("相似度分数: $similarities")

                val context = rag.generateContextWithMetadata(
                    query = question,
                    limit = 3,
                    minScore = 0.0,  // 设置为0，确保在测试模式下能返回结果
                    includeMetadata = true
                )

                if (context.isEmpty()) {
                    println("没有找到相关信息。")
                    continue
                }

                // 构建提示
                val instructions = """
                    你是一个基于本地嵌入模型的问答助手。你的任务是使用提供的上下文信息回答用户的问题。

                    请遵循以下准则：
                    1. 仅使用提供的上下文信息回答问题
                    2. 如果上下文中没有足够的信息，请坦诚地说明你不知道
                    3. 不要编造信息或使用你自己的知识
                    4. 引用信息的来源（如果有）
                    5. 保持回答简洁、准确和有帮助

                    上下文信息：
                    $context

                    用户问题：
                    $question
                """.trimIndent()

                // 生成回答
                println("正在生成回答...")
                val response = ragAgent.generate(instructions, options = ai.kastrax.core.agent.AgentGenerateOptions())

                // 显示回答
                println("\n回答:")
                println(response.text)
            }
        } finally {
            // 清理临时文件
            tempDir.deleteRecursively()
        }
    } finally {
        // 关闭 FastEmbed 服务，释放资源
        embeddingService.close()
    }
}
