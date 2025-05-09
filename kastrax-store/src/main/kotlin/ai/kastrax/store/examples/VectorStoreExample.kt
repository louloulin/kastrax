package ai.kastrax.store.examples

import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.VectorStoreFactory
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
        // 创建内存向量存储
        val vectorStore = VectorStoreFactory.createInMemoryVectorStore()

        // 创建索引
        val indexName = "example_index"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f),
            floatArrayOf(0.7f, 0.7f, 0f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1", "category" to "A"),
            mapOf("name" to "vector2", "category" to "B"),
            mapOf("name" to "vector3", "category" to "A"),
            mapOf("name" to "vector4", "category" to "B")
        )
        val ids = vectorStore.upsert(indexName, vectors, metadata)
        println("Added vectors with IDs: $ids")

        // 查询向量
        val queryVector = floatArrayOf(0.9f, 0.1f, 0f)
        val results = vectorStore.query(indexName, queryVector, 2)
        println("Query results:")
        results.forEach { result ->
            println("  ID: ${result.id}, Score: ${result.score}, Name: ${result.metadata?.get("name")}")
        }

        // 使用过滤器查询
        val filteredResults = vectorStore.query(
            indexName = indexName,
            queryVector = queryVector,
            topK = 10,
            filter = mapOf("category" to "A")
        )
        println("Filtered query results:")
        filteredResults.forEach { result ->
            println("  ID: ${result.id}, Score: ${result.score}, Name: ${result.metadata?.get("name")}, Category: ${result.metadata?.get("category")}")
        }

        // 删除向量
        val deleteResult = vectorStore.deleteVectors(indexName, listOf(ids[0]))
        println("Deleted vector: $deleteResult")

        // 再次查询
        val resultsAfterDelete = vectorStore.query(indexName, queryVector, 2)
        println("Query results after delete:")
        resultsAfterDelete.forEach { result ->
            println("  ID: ${result.id}, Score: ${result.score}, Name: ${result.metadata?.get("name")}")
        }

        // 获取索引信息
        val stats = vectorStore.describeIndex(indexName)
        println("Index stats: dimension=${stats.dimension}, count=${stats.count}, metric=${stats.metric}")

        // 删除索引
        val deleteIndexResult = vectorStore.deleteIndex(indexName)
        println("Deleted index: $deleteIndexResult")
    }
}

/**
 * 与 RAG 集成的示例。
 */
object RagIntegrationExample {

    /**
     * 运行示例。
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建内存向量存储
        val vectorStore = VectorStoreFactory.createInMemoryVectorStore()

        // 创建 RAG 向量存储适配器
        val ragVectorStore = VectorStoreFactory.adaptToRagVectorStore(vectorStore)

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

            override fun dimension(): Int {
                return 3
            }

            override fun close() {
                // 无需关闭任何资源
            }
        }

        // 添加文档
        val docs = listOf(
            ai.kastrax.store.document.Document(
                id = "doc1",
                content = "The apple is red and sweet.",
                metadata = mapOf("fruit" to "apple", "color" to "red")
            ),
            ai.kastrax.store.document.Document(
                id = "doc2",
                content = "Bananas are yellow and nutritious.",
                metadata = mapOf("fruit" to "banana", "color" to "yellow")
            ),
            ai.kastrax.store.document.Document(
                id = "doc3",
                content = "Oranges are rich in vitamin C.",
                metadata = mapOf("fruit" to "orange", "color" to "orange")
            ),
            ai.kastrax.store.document.Document(
                id = "doc4",
                content = "Apples and oranges are both fruits.",
                metadata = mapOf("fruit" to "mixed", "color" to "various")
            )
        )
        val added = ragVectorStore.addDocuments(docs, embeddingService)
        println("Added documents: $added")

        // 相似度搜索
        val query = "I like apples"
        val searchResults = ragVectorStore.similaritySearch(query, embeddingService, 2)
        println("Similarity search results for '$query':")
        searchResults.forEach { result ->
            println("  Content: ${result.document.content}")
            println("  Score: ${result.score}")
            println("  Metadata: ${result.document.metadata}")
            println()
        }

        // 关键词搜索
        val keywordResults = ragVectorStore.keywordSearch(listOf("vitamin", "rich"), 2)
        println("Keyword search results:")
        keywordResults.forEach { result ->
            println("  Content: ${result.document.content}")
            println("  Score: ${result.score}")
            println("  Metadata: ${result.document.metadata}")
            println()
        }

        // 元数据搜索
        val metadataResults = ragVectorStore.metadataSearch(mapOf("color" to "red"), 2)
        println("Metadata search results:")
        metadataResults.forEach { result ->
            println("  Content: ${result.document.content}")
            println("  Score: ${result.score}")
            println("  Metadata: ${result.document.metadata}")
            println()
        }
    }
}
