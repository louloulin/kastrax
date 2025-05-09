package ai.kastrax.store.examples

import ai.kastrax.store.VectorStoreFactory
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths

/**
 * LanceDB 示例。
 */
object LanceDBExample {

    /**
     * 运行示例。
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建临时目录
        val tempDir = Files.createTempDirectory("lancedb_example").toString()
        println("Using temporary directory: $tempDir")

        try {
            // 创建 LanceDB 向量存储
            val vectorStore = VectorStoreFactory.createLanceDBVectorStore(tempDir)

            // 创建索引
            val indexName = "example_index"
            val dimension = 3
            println("Creating index $indexName with dimension $dimension...")
            vectorStore.createIndex(indexName, dimension)

            // 添加向量
            val vectors = listOf(
                floatArrayOf(1f, 0f, 0f),
                floatArrayOf(0f, 1f, 0f),
                floatArrayOf(0f, 0f, 1f),
                floatArrayOf(0.5f, 0.5f, 0f),
                floatArrayOf(0.3f, 0.3f, 0.3f)
            )
            val metadata = listOf(
                mapOf("name" to "apple", "color" to "red", "category" to "fruit"),
                mapOf("name" to "banana", "color" to "yellow", "category" to "fruit"),
                mapOf("name" to "orange", "color" to "orange", "category" to "fruit"),
                mapOf("name" to "lemon", "color" to "yellow", "category" to "fruit"),
                mapOf("name" to "grape", "color" to "purple", "category" to "fruit")
            )
            println("Adding vectors...")
            val ids = vectorStore.upsert(indexName, vectors, metadata)
            println("Added vectors with IDs: $ids")

            // 获取索引信息
            val stats = vectorStore.describeIndex(indexName)
            println("Index stats: dimension=${stats.dimension}, count=${stats.count}, metric=${stats.metric}")

            // 查询向量
            val queryVector = floatArrayOf(1f, 0f, 0f)
            println("\nQuerying for vectors similar to [1, 0, 0]...")
            val results = vectorStore.query(indexName, queryVector, 3)
            results.forEachIndexed { index, result ->
                println("${index + 1}. ${result.metadata?.get("name")} (score: ${result.score})")
                println("   Metadata: ${result.metadata}")
            }

            // 使用过滤器查询
            println("\nQuerying for yellow fruits...")
            val filteredResults = vectorStore.query(
                indexName = indexName,
                queryVector = queryVector,
                topK = 5,
                filter = mapOf("color" to "yellow")
            )
            filteredResults.forEachIndexed { index, result ->
                println("${index + 1}. ${result.metadata?.get("name")} (score: ${result.score})")
                println("   Metadata: ${result.metadata}")
            }

            // 创建 ANN 索引
            println("\nCreating ANN index...")
            val annResult = vectorStore.createAnnIndex(
                indexName = indexName,
                indexType = "ivf_pq",
                params = mapOf("num_partitions" to 2, "num_sub_vectors" to 1)
            )
            println("ANN index created: $annResult")

            // 使用 ANN 索引查询
            println("\nQuerying using ANN index...")
            val annResults = vectorStore.query(indexName, queryVector, 3)
            annResults.forEachIndexed { index, result ->
                println("${index + 1}. ${result.metadata?.get("name")} (score: ${result.score})")
                println("   Metadata: ${result.metadata}")
            }

            // 更新向量
            println("\nUpdating vector...")
            val updateResult = vectorStore.updateVector(
                indexName = indexName,
                id = ids[0],
                vector = floatArrayOf(0.9f, 0.1f, 0f),
                metadata = mapOf("name" to "red apple", "color" to "red", "category" to "fruit", "taste" to "sweet")
            )
            println("Vector updated: $updateResult")

            // 查询更新后的向量
            println("\nQuerying after update...")
            val updatedResults = vectorStore.query(indexName, queryVector, 3)
            updatedResults.forEachIndexed { index, result ->
                println("${index + 1}. ${result.metadata?.get("name")} (score: ${result.score})")
                println("   Metadata: ${result.metadata}")
            }

            // 删除向量
            println("\nDeleting vector...")
            val deleteResult = vectorStore.deleteVectors(indexName, listOf(ids[1]))
            println("Vector deleted: $deleteResult")

            // 查询删除后的向量
            println("\nQuerying after deletion...")
            val afterDeleteResults = vectorStore.query(indexName, floatArrayOf(0f, 1f, 0f), 3)
            afterDeleteResults.forEachIndexed { index, result ->
                println("${index + 1}. ${result.metadata?.get("name")} (score: ${result.score})")
                println("   Metadata: ${result.metadata}")
            }

            // 列出所有索引
            println("\nListing all indexes...")
            val indexes = vectorStore.listIndexes()
            println("Indexes: $indexes")

            // 删除索引
            println("\nDeleting index...")
            val indexDeleteResult = vectorStore.deleteIndex(indexName)
            println("Index deleted: $indexDeleteResult")

            // 列出所有索引
            println("\nListing all indexes after deletion...")
            val remainingIndexes = vectorStore.listIndexes()
            println("Remaining indexes: $remainingIndexes")
        } finally {
            // 清理临时目录
            println("\nCleaning up temporary directory...")
            Files.walk(Paths.get(tempDir))
                .sorted(Comparator.reverseOrder())
                .forEach { Files.deleteIfExists(it) }
            println("Cleanup completed")
        }
    }
}
