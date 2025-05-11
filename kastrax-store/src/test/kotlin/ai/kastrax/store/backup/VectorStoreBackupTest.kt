package ai.kastrax.store.backup

import ai.kastrax.store.SimilarityMetric
import ai.kastrax.store.VectorStoreFactory
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class VectorStoreBackupTest {

    @TempDir
    lateinit var tempDir: Path

    private lateinit var vectorStore: ai.kastrax.store.VectorStore

    @BeforeEach
    fun setUp() {
        vectorStore = ai.kastrax.store.vector.memory.InMemoryVectorStore()
    }

    @Test
    fun `test backup and restore`() = runBlocking {
        // 创建索引
        val indexName = "test_index"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f),
            floatArrayOf(0f, 0f, 1f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1", "category" to "A"),
            mapOf("name" to "vector2", "category" to "B"),
            mapOf("name" to "vector3", "category" to "A")
        )
        val ids = vectorStore.upsert(indexName, vectors, metadata)
        assertEquals(3, ids.size)

        // 备份向量存储
        val backupDir = tempDir.toString()
        val backupFile = VectorStoreBackup.backup(vectorStore, backupDir)
        assertTrue(backupFile.isNotEmpty())

        // 创建新的向量存储
        val newVectorStore = ai.kastrax.store.vector.memory.InMemoryVectorStore()

        // 恢复向量存储
        val restoredCount = VectorStoreBackup.restore(newVectorStore, backupFile)
        assertEquals(1, restoredCount)

        // 验证索引是否已恢复
        val indexes = newVectorStore.listIndexes()
        assertTrue(indexes.contains(indexName))

        // 验证向量是否已恢复
        val stats = newVectorStore.describeIndex(indexName)
        assertEquals(dimension, stats.dimension)
        assertEquals(3, stats.count)
        assertEquals(SimilarityMetric.COSINE, stats.metric)

        // 查询向量
        val queryVector = floatArrayOf(1f, 0f, 0f)
        val results = newVectorStore.query(indexName, queryVector, 2, null, false)
        assertTrue(results.isNotEmpty())
        val firstResult = results.firstOrNull()
        assertNotNull(firstResult)
        assertEquals("vector1", firstResult?.metadata?.get("name"))
    }

    @Test
    fun `test backup and restore with multiple indexes`() = runBlocking {
        // 创建索引
        val indexName1 = "test_index_1"
        val indexName2 = "test_index_2"
        val dimension = 3
        vectorStore.createIndex(indexName1, dimension, SimilarityMetric.COSINE)
        vectorStore.createIndex(indexName2, dimension, SimilarityMetric.EUCLIDEAN)

        // 添加向量到第一个索引
        val vectors1 = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val metadata1 = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2")
        )
        vectorStore.upsert(indexName1, vectors1, metadata1)

        // 添加向量到第二个索引
        val vectors2 = listOf(
            floatArrayOf(0f, 0f, 1f),
            floatArrayOf(0.5f, 0.5f, 0f)
        )
        val metadata2 = listOf(
            mapOf("name" to "vector3"),
            mapOf("name" to "vector4")
        )
        vectorStore.upsert(indexName2, vectors2, metadata2)

        // 备份向量存储
        val backupDir = tempDir.toString()
        val backupFile = VectorStoreBackup.backup(vectorStore, backupDir)

        // 创建新的向量存储
        val newVectorStore = ai.kastrax.store.vector.memory.InMemoryVectorStore()

        // 恢复向量存储
        val restoredCount = VectorStoreBackup.restore(newVectorStore, backupFile)
        assertTrue(restoredCount > 0)

        // 验证索引是否已恢复
        val indexes = newVectorStore.listIndexes()
        assertTrue(indexes.contains(indexName1))
        assertTrue(indexes.contains(indexName2))

        // 验证第一个索引的向量是否已恢复
        val stats1 = newVectorStore.describeIndex(indexName1)
        assertEquals(dimension, stats1.dimension)
        assertEquals(2, stats1.count)
        assertEquals(SimilarityMetric.COSINE, stats1.metric)

        // 验证第二个索引的向量是否已恢复
        val stats2 = newVectorStore.describeIndex(indexName2)
        assertEquals(dimension, stats2.dimension)
        assertEquals(2, stats2.count)
        assertEquals(SimilarityMetric.EUCLIDEAN, stats2.metric)
    }

    @Test
    fun `test backup and restore specific indexes`() = runBlocking {
        // 创建索引
        val indexName1 = "test_index_1"
        val indexName2 = "test_index_2"
        val dimension = 3
        vectorStore.createIndex(indexName1, dimension, SimilarityMetric.COSINE)
        vectorStore.createIndex(indexName2, dimension, SimilarityMetric.EUCLIDEAN)

        // 添加向量到两个索引
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2")
        )
        vectorStore.upsert(indexName1, vectors, metadata)
        vectorStore.upsert(indexName2, vectors, metadata)

        // 备份特定索引
        val backupDir = tempDir.toString()
        val backupFile = VectorStoreBackup.backup(vectorStore, backupDir, listOf(indexName1))

        // 创建新的向量存储
        val newVectorStore = VectorStoreFactory.createInMemoryVectorStore()

        // 恢复向量存储
        val restoredCount = VectorStoreBackup.restore(newVectorStore, backupFile)
        assertTrue(restoredCount > 0)

        // 验证只有指定的索引被恢复
        val indexes = newVectorStore.listIndexes()
        assertTrue(indexes.contains(indexName1))
        assertFalse(indexes.contains(indexName2))
    }

    @Test
    fun `test list backups`() = runBlocking {
        // 创建索引
        val indexName = "test_index"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2")
        )
        vectorStore.upsert(indexName, vectors, metadata)

        // 创建多个备份
        val backupDir = tempDir.toString()
        val backupFile1 = VectorStoreBackup.backup(vectorStore, backupDir)
        Thread.sleep(1000) // 确保时间戳不同
        val backupFile2 = VectorStoreBackup.backup(vectorStore, backupDir)

        // 列出备份
        val backups = VectorStoreBackup.listBackups(backupDir)
        assertEquals(2, backups.size)

        // 验证备份信息
        assertTrue(backups[0].file.endsWith(".zip"))
        assertTrue(backups[0].size > 0)
        assertEquals(1, backups[0].indexCount)

        // 验证备份按时间戳降序排序
        assertTrue(backups[0].timestamp.isAfter(backups[1].timestamp))
    }

    @Test
    fun `test delete backup`() = runBlocking {
        // 创建索引
        val indexName = "test_index"
        val dimension = 3
        vectorStore.createIndex(indexName, dimension, SimilarityMetric.COSINE)

        // 添加向量
        val vectors = listOf(
            floatArrayOf(1f, 0f, 0f),
            floatArrayOf(0f, 1f, 0f)
        )
        val metadata = listOf(
            mapOf("name" to "vector1"),
            mapOf("name" to "vector2")
        )
        vectorStore.upsert(indexName, vectors, metadata)

        // 创建备份
        val backupDir = tempDir.toString()
        val backupFile = VectorStoreBackup.backup(vectorStore, backupDir)

        // 删除备份
        val deleted = VectorStoreBackup.deleteBackup(backupFile)
        assertTrue(deleted)

        // 验证备份已删除
        val backups = VectorStoreBackup.listBackups(backupDir)
        assertEquals(0, backups.size)
    }
}
