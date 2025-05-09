package ai.kastrax.store.examples

import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.metrics.MetricType
import ai.kastrax.store.metrics.VectorStoreMetrics
import kotlinx.coroutines.runBlocking
import kotlin.time.Duration.Companion.milliseconds

/**
 * 指标收集示例。
 */
object MetricsExample {

    /**
     * 运行示例。
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建内存向量存储
        val baseVectorStore = VectorStoreFactory.createInMemoryVectorStore()
        
        // 添加指标收集功能
        val vectorStore = VectorStoreFactory.withMetrics(baseVectorStore)

        // 创建索引
        val indexName = "example_index"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2"),
            mapOf("name" to "vector3")
        )
        vectorStore.upsert(indexName, vectors, metadata)

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        vectorStore.query(indexName, queryVector, 2)

        // 批量添加向量
        val batchVectors = List(100) { i ->
            when (i % 3) {
                0 -> floatArrayOf(1f, 0f, 0f)
                1 -> floatArrayOf(0f, 1f, 0f)
                else -> floatArrayOf(0f, 0f, 1f)
            }
        }
        val batchMetadata = List(100) { i ->
            mapOf("index" to i)
        }
        vectorStore.batchUpsert(indexName, batchVectors, batchMetadata, batchSize = 10)

        // 获取索引信息
        vectorStore.describeIndex(indexName)

        // 列出索引
        vectorStore.listIndexes()

        // 删除向量
        vectorStore.deleteVectors(indexName, listOf("1"))

        // 获取全局指标
        val globalMetrics = VectorStoreMetrics.getGlobalMetrics()
        println("Global metrics:")
        globalMetrics.forEach { (type, metric) ->
            println("  $type:")
            println("    Count: ${metric.count}")
            println("    Avg duration: ${metric.avgDuration}")
            println("    Min duration: ${metric.minDuration}")
            println("    Max duration: ${metric.maxDuration}")
            println("    Error count: ${metric.errorCount}")
        }

        // 获取索引指标
        val indexMetrics = VectorStoreMetrics.getIndexMetrics(indexName)
        println("\nIndex metrics for $indexName:")
        indexMetrics.forEach { (type, metric) ->
            println("  $type:")
            println("    Count: ${metric.count}")
            println("    Avg duration: ${metric.avgDuration}")
            println("    Min duration: ${metric.minDuration}")
            println("    Max duration: ${metric.maxDuration}")
            println("    Error count: ${metric.errorCount}")
        }

        // 模拟错误
        try {
            vectorStore.query("non_existent_index", queryVector, 2)
        } catch (e: Exception) {
            println("\nCaught expected exception: ${e.message}")
        }

        // 查看错误指标
        val updatedGlobalMetrics = VectorStoreMetrics.getGlobalMetrics()
        val queryMetric = updatedGlobalMetrics[MetricType.QUERY]
        println("\nQuery metrics after error:")
        println("  Count: ${queryMetric?.count}")
        println("  Error count: ${queryMetric?.errorCount}")

        // 重置指标
        VectorStoreMetrics.reset()
        println("\nMetrics after reset:")
        println("  Global metrics count: ${VectorStoreMetrics.getGlobalMetrics().size}")
        println("  Index metrics count: ${VectorStoreMetrics.getIndexMetrics(indexName).size}")
    }
}
