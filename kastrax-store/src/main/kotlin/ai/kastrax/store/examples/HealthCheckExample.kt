package ai.kastrax.store.examples

import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.health.HealthStatus
import ai.kastrax.store.health.IndexHealthCheckResult
import ai.kastrax.store.health.VectorStoreHealthCheck
import kotlinx.coroutines.runBlocking

/**
 * 健康检查示例。
 */
object HealthCheckExample {

    /**
     * 运行示例。
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建内存向量存储
        val vectorStore = VectorStoreFactory.createInMemoryVectorStore()

        // 创建索引
        val indexName1 = "example_index_1"
        val indexName2 = "example_index_2"
        val dimension = 3
        vectorStore.createIndex(indexName1, dimension)
        vectorStore.createIndex(indexName2, dimension)

        // 添加向量到第一个索引
        val vectors1 = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
        )
        val metadata1 = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2"),
            mapOf("name" to "vector3")
        )
        vectorStore.upsert(indexName1, vectors1, metadata1)

        // 添加向量到第二个索引
        val vectors2 = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val metadata2 = listOf(
            mapOf("name" to "vector4"),
            mapOf("name" to "vector5")
        )
        vectorStore.upsert(indexName2, vectors2, metadata2)

        // 执行健康检查
        val healthResult = VectorStoreHealthCheck.checkHealth(vectorStore)
        println("Vector store health status: ${healthResult.status}")
        println("Message: ${healthResult.message}")
        println("Duration: ${healthResult.duration}")
        println("Index count: ${healthResult.details["indexCount"]}")

        // 打印索引健康检查结果
        val indexResults = healthResult.details["indexResults"] as List<IndexHealthCheckResult>
        println("\nIndex health check results:")
        indexResults.forEach { result ->
            println("  ${result.indexName}:")
            println("    Status: ${result.status}")
            println("    Message: ${result.message}")
            println("    Duration: ${result.duration}")
            println("    Details:")
            result.details.forEach { (key, value) ->
                println("      $key: $value")
            }
            println()
        }

        // 模拟不健康的索引
        try {
            // 检查不存在的索引
            val nonExistentResult = VectorStoreHealthCheck.checkIndexHealth(vectorStore, "non_existent_index")
            println("\nNon-existent index health check result:")
            println("  Status: ${nonExistentResult.status}")
            println("  Message: ${nonExistentResult.message}")
            println("  Details: ${nonExistentResult.details["error"]}")
        } catch (e: Exception) {
            println("\nError checking non-existent index: ${e.message}")
        }

        // 检查所有索引的健康状态
        val allIndexesResult = VectorStoreHealthCheck.checkIndexesHealth(vectorStore, vectorStore.listIndexes())
        println("\nAll indexes health check results:")
        allIndexesResult.forEach { result ->
            println("  ${result.indexName}: ${result.status}")
        }

        // 统计健康状态
        val statusCounts = allIndexesResult.groupBy { it.status }
            .mapValues { it.value.size }
        println("\nHealth status counts:")
        HealthStatus.values().forEach { status ->
            println("  $status: ${statusCounts[status] ?: 0}")
        }
    }
}
