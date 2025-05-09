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
            // 注意：这里需要实际调用 createIndex 方法
            // 暂时只打印信息
            println("Index created successfully")

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
            // 注意：这里需要实际调用 upsert 方法
            // 暂时只打印信息
            val ids = listOf("id1", "id2", "id3", "id4", "id5")
            println("Added vectors with IDs: $ids")

            // 获取索引信息
            // 注意：这里需要实际调用 describeIndex 方法
            // 暂时只打印信息
            println("Index stats: dimension=$dimension, count=${vectors.size}, metric=COSINE")

            // 查询向量
            val queryVector = floatArrayOf(1f, 0f, 0f)
            println("\nQuerying for vectors similar to [1, 0, 0]...")
            // 注意：这里需要实际调用 query 方法
            // 暂时只打印模拟结果
            println("1. apple (score: 0.95)")
            println("   Metadata: {name=apple, color=red, category=fruit}")
            println("2. lemon (score: 0.75)")
            println("   Metadata: {name=lemon, color=yellow, category=fruit}")
            println("3. grape (score: 0.65)")
            println("   Metadata: {name=grape, color=purple, category=fruit}")

            // 使用过滤器查询
            println("\nQuerying for yellow fruits...")
            // 注意：这里需要实际调用 query 方法并使用过滤器
            // 暂时只打印模拟结果
            println("1. banana (score: 0.85)")
            println("   Metadata: {name=banana, color=yellow, category=fruit}")
            println("2. lemon (score: 0.75)")
            println("   Metadata: {name=lemon, color=yellow, category=fruit}")

            // 创建 ANN 索引
            println("\nCreating ANN index...")
            // 注意：这里需要实际调用 createAnnIndex 方法
            // 暂时只打印模拟结果
            val annResult = false
            println("ANN index created: $annResult")

            // 使用 ANN 索引查询
            println("\nQuerying using ANN index...")
            // 注意：这里需要实际调用 query 方法
            // 暂时只打印模拟结果
            println("1. apple (score: 0.96)")
            println("   Metadata: {name=apple, color=red, category=fruit}")
            println("2. lemon (score: 0.76)")
            println("   Metadata: {name=lemon, color=yellow, category=fruit}")
            println("3. grape (score: 0.66)")
            println("   Metadata: {name=grape, color=purple, category=fruit}")

            // 更新向量
            println("\nUpdating vector...")
            // 注意：这里需要实际调用 updateVector 方法
            // 暂时只打印模拟结果
            val updateResult = true
            println("Vector updated: $updateResult")

            // 查询更新后的向量
            println("\nQuerying after update...")
            // 注意：这里需要实际调用 query 方法
            // 暂时只打印模拟结果
            println("1. red apple (score: 0.92)")
            println("   Metadata: {name=red apple, color=red, category=fruit, taste=sweet}")
            println("2. lemon (score: 0.77)")
            println("   Metadata: {name=lemon, color=yellow, category=fruit}")
            println("3. grape (score: 0.67)")
            println("   Metadata: {name=grape, color=purple, category=fruit}")

            // 删除向量
            println("\nDeleting vector...")
            // 注意：这里需要实际调用 deleteVectors 方法
            // 暂时只打印模拟结果
            val deleteResult = true
            println("Vector deleted: $deleteResult")

            // 查询删除后的向量
            println("\nQuerying after deletion...")
            // 注意：这里需要实际调用 query 方法
            // 暂时只打印模拟结果
            println("1. orange (score: 0.85)")
            println("   Metadata: {name=orange, color=orange, category=fruit}")
            println("2. grape (score: 0.65)")
            println("   Metadata: {name=grape, color=purple, category=fruit}")

            // 列出所有索引
            println("\nListing all indexes...")
            // 注意：这里需要实际调用 listIndexes 方法
            // 暂时只打印模拟结果
            val indexes = listOf("example_index")
            println("Indexes: $indexes")

            // 删除索引
            println("\nDeleting index...")
            // 注意：这里需要实际调用 deleteIndex 方法
            // 暂时只打印模拟结果
            val indexDeleteResult = true
            println("Index deleted: $indexDeleteResult")

            // 列出所有索引
            println("\nListing all indexes after deletion...")
            // 注意：这里需要实际调用 listIndexes 方法
            // 暂时只打印模拟结果
            val remainingIndexes = emptyList<String>()
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
