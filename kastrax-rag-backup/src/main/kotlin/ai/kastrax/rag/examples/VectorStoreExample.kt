package ai.kastrax.rag.examples

import ai.kastrax.rag.embedding.EmbeddingService
import ai.kastrax.rag.retrieval.RetrieverFactory
import ai.kastrax.rag.retrieval.RetrieverType
import ai.kastrax.rag.vectorstore.RagVectorStoreFactory
import ai.kastrax.rag.vectorstore.StoreType
import kotlinx.coroutines.runBlocking

/**
 * 向量存储示例。
 */
object VectorStoreExample {

    /**
     * 运行示例。
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建模拟嵌入服务
        val embeddingService = object : EmbeddingService {
            override suspend fun embed(text: String): FloatArray {
                // 简单的模拟嵌入函数
                return when {
                    text.contains("apple") -> floatArrayOf(1f, 0f, 0f)
                    text.contains("banana") -> floatArrayOf(0f, 1f, 0f)
                    text.contains("orange") -> floatArrayOf(0f, 0f, 1f)
                    else -> floatArrayOf(0.33f, 0.33f, 0.33f)
                }
            }

            override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
                return texts.map { embed(it) }
            }
        }

        // 创建内存向量存储
        val vectorStore = RagVectorStoreFactory.createInMemoryVectorStore()

        // 添加文档
        val docs = listOf(
            "The apple is red and sweet.",
            "Bananas are yellow and nutritious.",
            "Oranges are rich in vitamin C.",
            "Apples and oranges are both fruits."
        )
        val metadata = listOf(
            mapOf("fruit" to "apple", "color" to "red"),
            mapOf("fruit" to "banana", "color" to "yellow"),
            mapOf("fruit" to "orange", "color" to "orange"),
            mapOf("fruit" to "mixed", "color" to "various")
        )
        val ids = vectorStore.addDocuments(docs, embeddingService, metadata)
        println("Added documents with IDs: $ids")

        // 创建 Top-K 检索器
        val topKRetriever = RetrieverFactory.createTopKRetriever(vectorStore, embeddingService)

        // 使用 Top-K 检索器检索文档
        val query = "I like sweet fruits"
        val topKResults = topKRetriever.retrieve(query, 2)
        println("Top-K retriever results for '$query':")
        topKResults.forEachIndexed { index, doc ->
            println("${index + 1}. ${doc.content}")
            println("   Metadata: ${doc.metadata}")
            println()
        }

        // 创建混合检索器
        val hybridRetriever = RetrieverFactory.createHybridRetriever(vectorStore, embeddingService)

        // 使用混合检索器检索文档
        val hybridResults = hybridRetriever.retrieve(query, 2)
        println("Hybrid retriever results for '$query':")
        hybridResults.forEachIndexed { index, doc ->
            println("${index + 1}. ${doc.content}")
            println("   Metadata: ${doc.metadata}")
            println()
        }

        // 创建基于 Chroma 向量存储的检索器
        println("Creating Chroma vector store (this requires a running Chroma server)...")
        try {
            val chromaRetriever = RetrieverFactory.createWithStoreType(
                StoreType.CHROMA,
                embeddingService,
                RetrieverType.HYBRID,
                mapOf("host" to "localhost", "port" to 8000)
            )

            // 添加文档到 Chroma 向量存储
            val chromaVectorStore = RagVectorStoreFactory.createChromaVectorStore()
            chromaVectorStore.addDocuments(docs, embeddingService, metadata)

            // 使用 Chroma 检索器检索文档
            val chromaResults = chromaRetriever.retrieve(query, 2)
            println("Chroma retriever results for '$query':")
            chromaResults.forEachIndexed { index, doc ->
                println("${index + 1}. ${doc.content}")
                println("   Metadata: ${doc.metadata}")
                println()
            }
        } catch (e: Exception) {
            println("Failed to connect to Chroma server: ${e.message}")
            println("Make sure Chroma server is running or use in-memory vector store instead.")
        }
    }
}
