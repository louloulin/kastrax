package ai.kastrax.rag.examples

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.context.ContextBuilderConfig
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.rag.embedding.RandomEmbeddingService
import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.document.Document
import kotlinx.coroutines.runBlocking
import java.util.Scanner
import java.util.UUID

/**
 * 简单的 RAG 示例，展示如何使用 RAG 系统。
 */
fun main() = runBlocking {
    println("初始化 RAG 系统...")

    // 创建嵌入服务
    val ragEmbeddingService = RandomEmbeddingService(1536)
    val embeddingService = object : ai.kastrax.store.embedding.EmbeddingService() {
        override val dimension: Int = 1536
        override suspend fun embed(text: String): FloatArray = ragEmbeddingService.embed(text)
        override suspend fun embedBatch(texts: List<String>): List<FloatArray> = ragEmbeddingService.embedBatch(texts)
        override fun close() {}
    }

    // 创建向量存储
    val vectorStore = VectorStoreFactory.createInMemoryVectorStore()

    // 创建文档存储
    val documentStore = VectorStoreFactory.adaptToDocumentVectorStore(vectorStore)

    // 创建 RAG 实例
    val rag = RAG(
        documentStore = documentStore,
        embeddingService = embeddingService,
        reranker = IdentityReranker(),
        defaultOptions = RagProcessOptions(
            contextOptions = ContextBuilderConfig(
                separator = "\n\n"
            )
        )
    )

    // 创建示例文档
    val documents = listOf(
        Document(
            id = UUID.randomUUID().toString(),
            content = "人工智能（Artificial Intelligence，简称AI）是计算机科学的一个分支，它致力于创造能够模拟人类智能的机器。",
            metadata = mapOf("source" to "AI百科", "category" to "技术")
        ),
        Document(
            id = UUID.randomUUID().toString(),
            content = "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习，而无需明确编程。",
            metadata = mapOf("source" to "AI百科", "category" to "技术")
        ),
        Document(
            id = UUID.randomUUID().toString(),
            content = "深度学习是机器学习的一种特定方法，它使用多层神经网络来模拟人脑的工作方式，特别适合处理大规模数据。",
            metadata = mapOf("source" to "AI百科", "category" to "技术")
        ),
        Document(
            id = UUID.randomUUID().toString(),
            content = "自然语言处理（NLP）是人工智能的一个分支，专注于使计算机理解和生成人类语言，包括文本分析、机器翻译等。",
            metadata = mapOf("source" to "NLP百科", "category" to "技术")
        ),
        Document(
            id = UUID.randomUUID().toString(),
            content = "计算机视觉是人工智能的一个领域，专注于使计算机能够从图像或视频中获取信息，模拟人类视觉系统。",
            metadata = mapOf("source" to "CV百科", "category" to "技术")
        ),
        Document(
            id = UUID.randomUUID().toString(),
            content = "强化学习是机器学习的一种方法，通过与环境交互并从反馈中学习，使代理能够做出最优决策。",
            metadata = mapOf("source" to "AI百科", "category" to "技术")
        ),
        Document(
            id = UUID.randomUUID().toString(),
            content = "生成式AI是人工智能的一个分支，专注于创建新内容，如文本、图像、音乐等，而不仅仅是分析现有数据。",
            metadata = mapOf("source" to "AI百科", "category" to "技术")
        ),
        Document(
            id = UUID.randomUUID().toString(),
            content = "大型语言模型（LLM）是一种基于深度学习的模型，经过大量文本数据训练，能够理解和生成人类语言。",
            metadata = mapOf("source" to "NLP百科", "category" to "技术")
        )
    )

    // 加载文档
    println("加载文档...")
    documentStore.addDocuments(documents, embeddingService)
    println("已加载 ${documents.size} 个文档")

    // 创建交互式查询界面
    val scanner = Scanner(System.`in`)

    println("\nRAG 系统已准备就绪！")
    println("你可以输入查询，或输入 'exit' 退出")

    while (true) {
        print("\n请输入查询: ")
        val query = scanner.nextLine().trim()

        if (query.equals("exit", ignoreCase = true)) {
            break
        }

        if (query.isEmpty()) {
            continue
        }

        // 执行搜索
        val results = rag.search(query, limit = 3)

        println("\n找到 ${results.size} 个相关文档:")
        results.forEachIndexed { index, result ->
            println("${index + 1}. ${result.document.content}")
            println("   来源: ${result.document.metadata["source"]}")
            println("   相似度: ${result.score}")
        }

        // 生成上下文
        val context = rag.generateContext(query, limit = 3)

        println("\n生成的上下文:")
        println(context)

        // 检索上下文
        val retrieveResult = rag.retrieveContext(query, limit = 3)

        println("\n检索上下文结果:")
        println("文档数量: ${retrieveResult.documents.size}")
        println("上下文: ${retrieveResult.context}")
    }

    println("感谢使用 RAG 系统！")
}
