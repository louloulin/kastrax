package ai.kastrax.store.examples

import ai.kastrax.store.embedding.EmbeddingService
import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.fusion.FusionOptions
import ai.kastrax.store.fusion.FusionStrategy
import ai.kastrax.store.fusion.QueryFusion
import kotlinx.coroutines.runBlocking

/**
 * 查询融合示例。
 */
object QueryFusionExample {

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
        vectorStore.createIndex(indexName, dimension)

        // 创建模拟嵌入服务
        val embeddingService = object : EmbeddingService() {
            override suspend fun embed(text: String): FloatArray {
                // 简单的模拟嵌入函数
                return when {
                    text.contains("apple") -> floatArrayOf(1f, 0f, 0f)
                    text.contains("banana") -> floatArrayOf(0f, 1f, 0f)
                    text.contains("orange") -> floatArrayOf(0f, 0f, 1f)
                    text.contains("fruit") -> floatArrayOf(0.33f, 0.33f, 0.33f)
                    text.contains("sweet") -> floatArrayOf(0.7f, 0.2f, 0.1f)
                    text.contains("yellow") -> floatArrayOf(0.1f, 0.8f, 0.1f)
                    else -> floatArrayOf(0.33f, 0.33f, 0.33f)
                }
            }

            override suspend fun embedBatch(texts: List<String>): List<FloatArray> {
                return texts.map { embed(it) }
            }

            override val dimension: Int = dimension

            override fun close() {
                // 无需关闭任何资源
            }
        }

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f),
            floatArrayOf(0.7f, 0.7f, 0f),
            floatArrayOf(0.33f, 0.33f, 0.33f)
        )
        val metadata = listOf(
            mapOf("content" to "The apple is red and sweet.", "category" to "fruit"),
            mapOf("content" to "Bananas are yellow and nutritious.", "category" to "fruit"),
            mapOf("content" to "Oranges are rich in vitamin C.", "category" to "fruit"),
            mapOf("content" to "Apples and bananas are both sweet fruits.", "category" to "mixed"),
            mapOf("content" to "Fruits are an important part of a healthy diet.", "category" to "general")
        )
        val ids = vectorStore.upsert(indexName, vectors, metadata)
        println("Added vectors with IDs: $ids")

        // 定义多个查询
        val queries = listOf(
            "I like sweet fruits",
            "Yellow fruits with potassium"
        )
        println("Queries: $queries")

        // 使用加权融合策略
        val weightedOptions = FusionOptions(
            strategy = FusionStrategy.WEIGHTED,
            weights = listOf(0.7, 0.3)
        )
        val weightedResults = QueryFusion.fuseQueries(
            vectorStore = vectorStore,
            indexName = indexName,
            queries = queries,
            embeddingService = embeddingService,
            topK = 3,
            options = weightedOptions
        )
        println("\nWeighted fusion results:")
        weightedResults.forEachIndexed { index, result ->
            println("${index + 1}. ID: ${result.id}, Score: ${result.score}")
            println("   Content: ${result.metadata?.get("content")}")
            println()
        }

        // 使用最大分数融合策略
        val maxScoreOptions = FusionOptions(strategy = FusionStrategy.MAX_SCORE)
        val maxScoreResults = QueryFusion.fuseQueries(
            vectorStore = vectorStore,
            indexName = indexName,
            queries = queries,
            embeddingService = embeddingService,
            topK = 3,
            options = maxScoreOptions
        )
        println("\nMax score fusion results:")
        maxScoreResults.forEachIndexed { index, result ->
            println("${index + 1}. ID: ${result.id}, Score: ${result.score}")
            println("   Content: ${result.metadata?.get("content")}")
            println()
        }

        // 使用平均分数融合策略
        val averageOptions = FusionOptions(strategy = FusionStrategy.AVERAGE)
        val averageResults = QueryFusion.fuseQueries(
            vectorStore = vectorStore,
            indexName = indexName,
            queries = queries,
            embeddingService = embeddingService,
            topK = 3,
            options = averageOptions
        )
        println("\nAverage fusion results:")
        averageResults.forEachIndexed { index, result ->
            println("${index + 1}. ID: ${result.id}, Score: ${result.score}")
            println("   Content: ${result.metadata?.get("content")}")
            println()
        }

        // 使用递归融合策略
        val recursiveOptions = FusionOptions(
            strategy = FusionStrategy.RECURSIVE,
            recursiveDepth = 2
        )
        val recursiveResults = QueryFusion.fuseQueries(
            vectorStore = vectorStore,
            indexName = indexName,
            queries = queries,
            embeddingService = embeddingService,
            topK = 3,
            options = recursiveOptions
        )
        println("\nRecursive fusion results:")
        recursiveResults.forEachIndexed { index, result ->
            println("${index + 1}. ID: ${result.id}, Score: ${result.score}")
            println("   Content: ${result.metadata?.get("content")}")
            println()
        }
    }
}
