package ai.kastrax.examples

import ai.kastrax.core.agent.agent
import ai.kastrax.rag.RAG
import ai.kastrax.rag.document.DirectoryDocumentLoader
import ai.kastrax.rag.document.RecursiveCharacterTextSplitter
import ai.kastrax.rag.document.WebPageDocumentLoader
import ai.kastrax.rag.embedding.OpenAIEmbeddingService
import ai.kastrax.rag.embedding.RandomEmbeddingService
import ai.kastrax.rag.vectorstore.RagInMemoryVectorStore
import ai.kastrax.integrations.openai.openAi
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * RAG 系统示例
 *
 * 这个示例展示了如何使用 RAG 系统从文档中检索信息并生成回答。
 */
fun main() = runBlocking {
    // 创建向量存储和嵌入服务
    val vectorStore = RagInMemoryVectorStore()

    // 使用 OpenAI 嵌入服务（需要 API 密钥）
    // val embeddingService = OpenAIEmbeddingService(apiKey = System.getenv("OPENAI_API_KEY"))

    // 使用随机嵌入服务（用于测试，不需要 API 密钥）
    val embeddingService = RandomEmbeddingService(dimensions = 1536)

    // 创建 RAG 系统
    val rag = RAG(vectorStore, embeddingService)

    // 创建文档分割器
    val splitter = RecursiveCharacterTextSplitter(
        chunkSize = 500,
        chunkOverlap = 100
    )

    // 从目录加载文档
    println("从目录加载文档...")
    val docsDir = File("docs")
    if (docsDir.exists() && docsDir.isDirectory) {
        val directoryLoader = DirectoryDocumentLoader(
            directory = docsDir,
            recursive = true,
            fileExtensions = listOf("txt", "md", "html")
        )
        rag.loadDocuments(directoryLoader, splitter)
    }

    // 从网页加载文档
    println("从网页加载文档...")
    val urls = listOf(
        "https://en.wikipedia.org/wiki/Artificial_intelligence",
        "https://en.wikipedia.org/wiki/Machine_learning",
        "https://en.wikipedia.org/wiki/Natural_language_processing"
    )

    for (url in urls) {
        try {
            val webLoader = WebPageDocumentLoader(url)
            rag.loadDocuments(webLoader, splitter)
        } catch (e: Exception) {
            println("无法加载网页: $url - ${e.message}")
        }
    }

    // 创建 OpenAI 提供者
    val openai = openAi(
        model = "gpt-3.5-turbo"
        // API 密钥从环境变量 OPENAI_API_KEY 获取
    )

    // 创建 RAG 代理
    val ragAgent = agent {
        name = "RAG Agent"
        instructions = """
            你是一个基于检索增强生成 (RAG) 的问答助手。你的任务是使用提供的上下文信息回答用户的问题。

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
        model = openai
    }

    // 使用 RAG 系统回答问题
    while (true) {
        print("\n请输入问题（输入 'exit' 退出）: ")
        val question = readLine() ?: ""

        if (question.equals("exit", ignoreCase = true)) {
            break
        }

        // 检索相关上下文
        val context = rag.generateContextWithMetadata(
            query = question,
            limit = 5,
            minScore = 0.5,
            includeMetadata = true
        )

        if (context.isEmpty()) {
            println("没有找到相关信息。")
            continue
        }

        // 构建提示
        val instructions = """
            你是一个基于检索增强生成 (RAG) 的问答助手。你的任务是使用提供的上下文信息回答用户的问题。

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

        val prompt = instructions

        // 生成回答
        println("\n正在生成回答...")
        val response = ragAgent.generate(prompt, options = ai.kastrax.core.agent.AgentGenerateOptions())

        // 显示回答
        println("\n回答:")
        println(response.text)
    }
}
