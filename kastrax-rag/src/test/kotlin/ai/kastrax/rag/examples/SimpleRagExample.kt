package ai.kastrax.rag.examples

import ai.kastrax.rag.RAG
import ai.kastrax.rag.RagProcessOptions
import ai.kastrax.rag.document.TextFileLoader
import ai.kastrax.rag.document.TextSplitter
import ai.kastrax.rag.reranker.IdentityReranker
import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.adapter.DocumentVectorStoreAdapter
import ai.kastrax.rag.examples.MockEmbeddingService
import kotlinx.coroutines.runBlocking

/**
 * 简单的 RAG 示例，展示如何使用 RAG 加载文档、搜索文档和生成上下文。
 */
fun main() = runBlocking {
    // 创建嵌入服务
    val embeddingService = MockEmbeddingService()

    // 创建向量存储
    val vectorStore = VectorStoreFactory.createInMemoryVectorStore()

    // 创建文档向量存储适配器
    val documentStore = DocumentVectorStoreAdapter(vectorStore)

    // 创建 RAG 实例
    val rag = RAG(
        documentStore = documentStore,
        embeddingService = embeddingService,
        reranker = IdentityReranker(),
        defaultOptions = RagProcessOptions(
            useHybridSearch = true,
            hybridOptions = ai.kastrax.rag.HybridOptions(
                vectorWeight = 0.7,
                keywordWeight = 0.3
            )
        )
    )

    // 加载文档
    val loader = TextFileLoader("path/to/your/documents")
    val splitter = TextSplitter(chunkSize = 500, chunkOverlap = 100)
    val count = rag.loadDocuments(loader, splitter)
    println("Loaded $count document chunks")

    // 搜索文档
    val query = "What is RAG?"
    val searchResults = rag.search(query, limit = 3)
    println("Search results:")
    searchResults.forEachIndexed { index, result ->
        println("${index + 1}. ${result.document.content.take(100)}... (score: ${result.score})")
    }

    // 生成上下文
    val context = rag.generateContext(query, limit = 3)
    println("\nGenerated context:")
    println(context)

    // 检索上下文
    val retrieveResult = rag.retrieveContext(query, limit = 3)
    println("\nRetrieved context:")
    println(retrieveResult.context)
    println("\nRetrieved documents:")
    retrieveResult.documents.forEachIndexed { index, document ->
        println("${index + 1}. ${document.id}: ${document.content.take(100)}...")
    }
}
