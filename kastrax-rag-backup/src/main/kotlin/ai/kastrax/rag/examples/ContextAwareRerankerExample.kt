package ai.kastrax.rag.examples

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.document.Document
import ai.kastrax.rag.embedding.RandomEmbeddingService
import ai.kastrax.rag.reranker.ContextAwareReranker
import ai.kastrax.rag.reranker.ContextAwareRerankerConfig
import ai.kastrax.rag.vectorstore.InMemoryVectorStore
import kotlinx.coroutines.runBlocking

/**
 * 上下文感知重排序器示例。
 */
fun main() = runBlocking {
    // 创建向量存储和嵌入服务
    val vectorStore = InMemoryVectorStore()
    val embeddingService = RandomEmbeddingService()

    // 创建上下文感知重排序器
    val reranker = ContextAwareReranker(
        embeddingService = embeddingService,
        config = ContextAwareRerankerConfig(
            contextWeight = 0.6,
            queryWeight = 0.4,
            originalScoreWeight = 0.3
        )
    )

    // 创建 RAG 实例
    val rag = RAG(
        vectorStore = vectorStore,
        embeddingService = embeddingService,
        reranker = reranker,
        defaultOptions = RagProcessOptions(
            useContextAwareReranking = true
        )
    )

    // 加载示例文档
    val documents = listOf(
        Document(
            content = "人工智能是计算机科学的一个分支，它关注于开发能够执行通常需要人类智能的任务的系统。",
            metadata = mapOf("source" to "wiki", "id" to "1")
        ),
        Document(
            content = "机器学习是人工智能的一个子领域，它使用统计技术使计算机系统能够从数据中学习。",
            metadata = mapOf("source" to "textbook", "id" to "2")
        ),
        Document(
            content = "深度学习是机器学习的一种方法，它使用神经网络来模拟人类大脑的学习过程。",
            metadata = mapOf("source" to "article", "id" to "3")
        ),
        Document(
            content = "自然语言处理是人工智能的一个分支，它关注于使计算机能够理解和生成人类语言。",
            metadata = mapOf("source" to "blog", "id" to "4")
        ),
        Document(
            content = "计算机视觉是人工智能的一个领域，它使计算机能够从图像或视频中获取信息。",
            metadata = mapOf("source" to "lecture", "id" to "5")
        )
    )

    // 将文档添加到向量存储
    for (doc in documents) {
        // 将 Map<String, Any> 转换为 Map<String, String>
        val stringMetadata = doc.metadata.mapValues { it.value.toString() }
        val id = vectorStore.addDocument(doc.content, embeddingService, stringMetadata)
        println("Added document with ID: $id")
    }

    // 使用上下文感知重排序进行搜索
    val query = "人工智能和机器学习"
    val context = "我正在研究深度学习和神经网络"

    // 创建自定义选项，包括上下文感知重排序
    val options = RagProcessOptions(
        useContextAwareReranking = true,
        contextAwareRerankingOptions = ContextAwareRerankerConfig(
            contextWeight = 0.7,
            queryWeight = 0.3,
            originalScoreWeight = 0.2
        )
    )

    // 执行搜索
    val results = rag.search(query, 3, 0.0, options)

    // 打印结果
    println("查询: $query")
    println("上下文: $context")
    println("搜索结果:")
    results.forEachIndexed { index, result ->
        println("${index + 1}. ${result.document.content} (分数: ${result.score})")
    }

    // 生成上下文
    val generatedContext = rag.generateContext(query, 3, 0.0, options)
    println("\n生成的上下文:")
    println(generatedContext)
}
