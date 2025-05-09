package ai.kastrax.store.examples

import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.VectorStoreFactory.createAnnIndex
import kotlinx.coroutines.runBlocking
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * LanceDB 向量存储示例。
 * 演示如何使用 LanceDB 向量存储进行向量操作。
 */
object LanceDBExample {

    /**
     * 运行示例。
     */
    @JvmStatic
    fun main(args: Array<String>) = runBlocking {
        // 创建临时目录
        val tempDir = Files.createTempDirectory("lancedb_example").toString()
        println("使用临时目录: $tempDir")

        try {
            // 创建 LanceDB 向量存储
            val vectorStore = VectorStoreFactory.createLanceDBVectorStore(tempDir)
            println("已创建 LanceDB 向量存储")

            // 创建索引
            val indexName = "example_index"
            val dimension = 3
            println("创建索引 $indexName，维度为 $dimension...")
            val created = vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)
            println("索引创建${if (created) "成功" else "失败"}")

            // 添加向量
            val vectors = listOf(
                floatArrayOf(1f, 0f, 0f),
                floatArrayOf(0f, 1f, 0f),
                floatArrayOf(0f, 0f, 1f),
                floatArrayOf(0.5f, 0.5f, 0f),
                floatArrayOf(0.3f, 0.3f, 0.3f)
            )
            val metadata = listOf(
                mapOf("name" to "向量1", "category" to "A"),
                mapOf("name" to "向量2", "category" to "B"),
                mapOf("name" to "向量3", "category" to "A"),
                mapOf("name" to "向量4", "category" to "B"),
                mapOf("name" to "向量5", "category" to "C")
            )
            println("添加 ${vectors.size} 个向量...")
            val ids = vectorStore.upsert(indexName, vectors, metadata)
            println("向量添加成功，ID: $ids")

            // 查询向量
            val queryVector = floatArrayOf(1f, 0f, 0f)
            println("查询与 [1, 0, 0] 最相似的 3 个向量...")
            val results = vectorStore.query(indexName, queryVector, 3)
            println("查询结果:")
            results.forEachIndexed { index, result ->
                println("  ${index + 1}. ID: ${result.id}, 分数: ${result.score}, 元数据: ${result.metadata}")
            }

            // 使用过滤器查询
            println("查询类别为 'A' 的向量...")
            val filteredResults = vectorStore.query(
                indexName = indexName,
                queryVector = queryVector,
                topK = 10,
                filter = mapOf("category" to "A")
            )
            println("过滤查询结果:")
            filteredResults.forEachIndexed { index, result ->
                println("  ${index + 1}. ID: ${result.id}, 分数: ${result.score}, 元数据: ${result.metadata}")
            }

            // 创建 ANN 索引
            println("创建 ANN 索引...")
            val annCreated = vectorStore.createAnnIndex(
                indexName = indexName,
                indexType = "ivf_pq",
                params = mapOf("num_partitions" to 2, "num_sub_vectors" to 1)
            )
            println("ANN 索引创建${if (annCreated) "成功" else "失败"}")

            // 批量添加向量
            val batchVectors = List(20) { i ->
                when (i % 3) {
                    0 -> floatArrayOf(1f, 0f, 0f)
                    1 -> floatArrayOf(0f, 1f, 0f)
                    else -> floatArrayOf(0f, 0f, 1f)
                }
            }
            val batchMetadata = List(20) { i ->
                mapOf("index" to i, "batch" to true)
            }
            println("批量添加 ${batchVectors.size} 个向量...")
            val batchIds = vectorStore.batchUpsert(indexName, batchVectors, batchMetadata, batchSize = 5)
            println("批量添加成功，ID 数量: ${batchIds.size}")

            // 获取索引信息
            println("获取索引信息...")
            val stats = vectorStore.describeIndex(indexName)
            println("索引信息: 维度=${stats.dimension}, 向量数量=${stats.count}, 度量方式=${stats.metric}")

            // 删除向量
            println("删除第一个向量...")
            val deleted = vectorStore.deleteVectors(indexName, listOf(ids[0]))
            println("向量删除${if (deleted) "成功" else "失败"}")

            // 更新向量
            println("更新第二个向量...")
            val updated = vectorStore.updateVector(
                indexName = indexName,
                id = ids[1],
                vector = floatArrayOf(0f, 0.5f, 0.5f),
                metadata = mapOf("name" to "更新的向量", "category" to "D")
            )
            println("向量更新${if (updated) "成功" else "失败"}")

            // 再次查询
            println("再次查询...")
            val updatedResults = vectorStore.query(indexName, queryVector, 3)
            println("更新后的查询结果:")
            updatedResults.forEachIndexed { index, result ->
                println("  ${index + 1}. ID: ${result.id}, 分数: ${result.score}, 元数据: ${result.metadata}")
            }

            // 列出所有索引
            println("列出所有索引...")
            val indexes = vectorStore.listIndexes()
            println("索引列表: $indexes")

            // 删除索引
            println("删除索引...")
            val indexDeleted = vectorStore.deleteIndex(indexName)
            println("索引删除${if (indexDeleted) "成功" else "失败"}")

        } catch (e: Exception) {
            println("发生错误: ${e.message}")
            e.printStackTrace()
        } finally {
            // 清理临时目录
            println("清理临时目录...")
            File(tempDir).deleteRecursively()
            println("示例运行完成")
        }
    }
}
