package ai.kastrax.store.examples

import ai.kastrax.store.VectorStoreFactory
import ai.kastrax.store.backup.VectorStoreBackup
import kotlinx.coroutines.runBlocking
import java.nio.file.Files
import java.nio.file.Paths
import java.time.format.DateTimeFormatter

/**
 * 备份示例。
 */
object BackupExample {

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
            mapOf("name" to "vector1", "category" to "A"),
            mapOf("name" to "vector2", "category" to "B"),
            mapOf("name" to "vector3", "category" to "A")
        )
        vectorStore.upsert(indexName1, vectors1, metadata1)

        // 添加向量到第二个索引
        val vectors2 = listOf(
            floatArrayOf(0.5f, 0.5f, 0f),
            floatArrayOf(0.3f, 0.3f, 0.3f)
        )
        val metadata2 = listOf(
            mapOf("name" to "vector4", "category" to "C"),
            mapOf("name" to "vector5", "category" to "D")
        )
        vectorStore.upsert(indexName2, vectors2, metadata2)

        // 创建备份目录
        val backupDir = "backup"
        Files.createDirectories(Paths.get(backupDir))

        // 备份向量存储
        println("Backing up vector store...")
        val backupFile = VectorStoreBackup.backup(vectorStore, backupDir)
        println("Backup created: $backupFile")

        // 列出备份
        println("\nListing backups:")
        val backups = VectorStoreBackup.listBackups(backupDir)
        backups.forEach { backup ->
            val timestamp = backup.timestamp.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
            val sizeKB = backup.size / 1024.0
            println("  ${backup.file} (${timestamp}, ${String.format("%.2f", sizeKB)} KB, ${backup.indexCount} indexes)")
        }

        // 创建新的向量存储
        println("\nCreating new vector store...")
        val newVectorStore = VectorStoreFactory.createInMemoryVectorStore()

        // 恢复向量存储
        println("Restoring from backup...")
        val restoredCount = VectorStoreBackup.restore(newVectorStore, backupFile)
        println("Restored $restoredCount indexes")

        // 验证恢复的索引
        println("\nVerifying restored indexes:")
        val indexes = newVectorStore.listIndexes()
        println("  Indexes: $indexes")

        // 查询第一个索引
        println("\nQuerying first index:")
        val queryVector1 = floatArrayOf(1f, 0f, 0f)
        val results1 = newVectorStore.query(indexName1, queryVector1, 2)
        results1.forEachIndexed { index, result ->
            println("  ${index + 1}. ${result.metadata?.get("name")} (score: ${result.score})")
        }

        // 查询第二个索引
        println("\nQuerying second index:")
        val queryVector2 = floatArrayOf(0.5f, 0.5f, 0f)
        val results2 = newVectorStore.query(indexName2, queryVector2, 2)
        results2.forEachIndexed { index, result ->
            println("  ${index + 1}. ${result.metadata?.get("name")} (score: ${result.score})")
        }

        // 只恢复第一个索引
        println("\nRestoring only the first index...")
        val newVectorStore2 = VectorStoreFactory.createInMemoryVectorStore()
        val restoredCount2 = VectorStoreBackup.restore(
            newVectorStore2,
            backupFile,
            indexNames = listOf(indexName1)
        )
        println("Restored $restoredCount2 indexes")

        // 验证只恢复了第一个索引
        val indexes2 = newVectorStore2.listIndexes()
        println("  Indexes: $indexes2")
        
        // 删除备份
        println("\nDeleting backup...")
        val deleted = VectorStoreBackup.deleteBackup(backupFile)
        println("Backup deleted: $deleted")
    }
}
